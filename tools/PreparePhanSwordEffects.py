"""Pack authored Phan sword poses: six keys + five new drawings = 11 frames.

Only measured slicing, alpha cleanup, palette mapping and nearest-neighbour
normalization happen here. Source artwork and prompts remain unchanged.
Run after PrepareV4Runtime.py; that tool also reapplies this override itself.
"""
from pathlib import Path
import hashlib
import json
import numpy as np
from PIL import Image, ImageDraw
import PrepareV4Runtime as base

PROJECT = base.PROJECT
ART = PROJECT / 'art/effects/phan_sword'
BANK = PROJECT / 'assets/players/phan_si_kien/v4'
STATES = ['skill_u', 'skill_i', 'ultimate']


def measured_cuts(mask, axis, count):
    quiet = ~mask.any(axis=axis)
    changes = np.diff(np.r_[False, quiet, False].astype(int))
    runs = list(zip(np.where(changes == 1)[0], np.where(changes == -1)[0]))
    inner = [(int(a), int(b)) for a, b in runs if a > 0 and b < len(quiet) and b-a >= 5]
    if len(inner) != count-1:
        raise ValueError(f'Expected {count-1} verified gutters, found {inner}')
    return [0] + [(a+b)//2 for a, b in inner] + [len(quiet)]


def source_frames(name, columns):
    path = ART/'source'/f'{name}.png'
    raw = Image.open(path).convert('RGBA')
    cleaned, removed = base.clean(raw, 'phan_si_kien', 'sword')
    mask = np.asarray(cleaned)[:, :, 3] > 0
    xs = measured_cuts(mask, 0, columns)
    ys = measured_cuts(mask, 1, 3)
    rows = []
    for row in range(3):
        frames = []
        for col in range(columns):
            rect = [xs[col], ys[row], xs[col+1]-xs[col], ys[row+1]-ys[row]]
            x, y, w, h = rect
            frames.append({'image': cleaned.crop((x, y, x+w, y+h)),
                           'source_file': base.relative(path), 'source_rect': rect})
        rows.append(frames)
    # First pose of each row is upright; overhead sword/arm poses are excluded
    # from calibration. Each source sheet has one scale for all of its drawings.
    heights = []
    for row in rows:
        b = base.bbox(base.body_mask(row[0]['image'], 'phan_si_kien'))
        heights.append(b[3]-b[1])
    reference = float(np.median(heights))
    meta = {'file': base.relative(path), 'width': raw.width, 'height': raw.height,
            'columns': columns, 'rows': 3, 'x_cuts': xs, 'y_cuts': ys,
            'sha256': hashlib.sha256(path.read_bytes()).hexdigest(),
            'alpha_cutoff': 128, 'removed_speck_pixels': removed,
            'upright_body_heights': heights, 'body_reference_height': reference,
            'uniform_scale': 64/reference}
    return rows, meta


def apply_skill_pose_override():
    originals, old_meta = source_frames('skill-poses', 6)
    additions, new_meta = source_frames('skill-inbetweens', 5)
    palette = base.make_palette([c['image'] for rows in (originals, additions) for row in rows for c in row])
    clips, records = {}, {}
    for row, state in enumerate(STATES):
        merged = []
        for n in range(6):
            merged.append((originals[row][n], old_meta, 'key'))
            if n < 5:
                merged.append((additions[row][n], new_meta, 'new-inbetween'))
        clips[state], records[state] = [], []
        for cell, meta, role in merged:
            root, mask = base.root_for(cell['image'], 'phan_si_kien', state, meta['body_reference_height'])
            im, body, extent = base.fit(cell['image'], mask, root, meta['uniform_scale'], 192, (96, 172))
            im = base.remap(im, palette)
            clips[state].append(im)
            records[state].append({k: v for k, v in cell.items() if k != 'image'} | {
                'source_kind': 'sword-authored-'+role, 'source_body_bounds': base.bbox(mask),
                'source_root': root, 'scale': meta['uniform_scale'], 'target_pivot': [96, 172],
                'art_offset': [0, 0], 'target_body_bounds': body,
                'transformed_visible_bounds': extent})
        assert len({im.tobytes() for im in clips[state]}) == 11, state+' repeated frame'
    clips['special'] = clips['skill_u']
    records['special'] = [dict(r, alias_of='skill_u') for r in records['skill_u']]

    manifest_path = BANK/'manifest.json'
    manifest = json.loads(manifest_path.read_text(encoding='utf-8'))
    _, old_props = base.old_properties('phan_si_kien')
    props = dict(line.split('=', 1) for line in (BANK/'animation.properties').read_text().splitlines()
                 if '=' in line and not line.startswith('#'))
    for state, frames in clips.items():
        row = manifest['animations'][state]['row']
        duration = float(old_props[state+'.secondsPerFrame'])*6
        props.update({state+'.frames': '11', state+'.secondsPerFrame': f'{duration/11:.10f}',
                      state+'.activeFrame': '6', state+'.recoveryFrame': '10'})
        metadata = []
        for i, im in enumerate(frames):
            name = f'{state}_{i+1}.png'
            im.save(BANK/name)
            metadata.append(records[state][i] | {'file': name, 'atlas_rect': [i*192, row*192, 192, 192],
                'visible_bounds': list(im.getbbox()), 'sha256': hashlib.sha256(im.tobytes()).hexdigest()})
        manifest['animations'][state].update({'frames': 11, 'seconds_per_frame': duration/11,
            'active_frame': 6, 'recovery_frame': 10, 'frame_metadata': metadata})
    manifest.update({'columns': 11, 'source_overrides': [old_meta, new_meta],
                     'sword_override': 'three skill poses, five additional authored drawings per clip'})
    atlas = Image.new('RGBA', (192*11, 192*manifest['rows']))
    for state, clip in manifest['animations'].items():
        for i, record in enumerate(clip['frame_metadata']):
            atlas.paste(Image.open(BANK/record['file']), (i*192, clip['row']*192))
    atlas.save(BANK/'atlas.png')
    manifest_path.write_text(json.dumps(manifest, indent=2, ensure_ascii=False), encoding='utf-8')
    (BANK/'animation.properties').write_text(''.join(f'{k}={v}\n' for k, v in props.items()), encoding='ascii')
    (BANK/'manifest.txt').write_text(
        'V4 runtime bank: phan_si_kien\n192x192; feet pivot=(96,172); scale=1.\n'
        '24 rows, 11 atlas columns. U/I/O and special alias have 11 frames; other clips have 6.\n'
        'Source coordinates, roots and override provenance are in manifest.json.\n', encoding='utf-8')
    (ART/'metadata').mkdir(parents=True, exist_ok=True)
    (ART/'metadata/pose-measurements.json').write_text(json.dumps([old_meta, new_meta], indent=2), encoding='utf-8')
    preview = Image.new('RGB', (11*192, 3*212), '#071a2c')
    draw = ImageDraw.Draw(preview)
    for row, state in enumerate(STATES):
        draw.text((8, row*212+3), state+' / 11 frames', font=base.FONT, fill='#65ddff')
        for col, im in enumerate(clips[state]):
            preview.paste(im, (col*192, row*212+20), im)
    preview.save(ART/'pose-preview.png')
    full_preview = Image.new('RGB', (150+11*192, 24+manifest['rows']*204), '#06172b')
    draw = ImageDraw.Draw(full_preview)
    draw.text((5, 4), 'phan_si_kien / sword override', font=base.FONT, fill='#a7e9ff')
    for state, clip in manifest['animations'].items():
        y = 24+clip['row']*204
        draw.text((5, y+10), state, font=base.FONT, fill='#69ccf0')
        for i, record in enumerate(clip['frame_metadata']):
            im = Image.open(BANK/record['file']).convert('RGBA')
            full_preview.paste(im, (150+i*192, y), im)
    full_preview.save(base.PREVIEW/'phan_si_kien-runtime-contact.png')
    print(json.dumps({'phan_skill_frames': 11, 'source_sizes': [[m['width'], m['height']] for m in (old_meta, new_meta)],
                      'source_body_reference': [m['body_reference_height'] for m in (old_meta, new_meta)],
                      'runtime_frame_size': [192, 192], 'pivot': [96, 172], 'active_frame': 6}), flush=True)


if __name__ == '__main__':
    apply_skill_pose_override()
