"""Install the approved Do Mixi sprite design as a separate runtime bank."""
from hashlib import sha256
import json
from pathlib import Path
from shutil import copyfile
from tempfile import TemporaryDirectory

from PIL import Image

ROOT = Path(__file__).resolve().parents[1]
SOURCE = ROOT / 'art/animations/do-mixi-design'
SHISHA = SOURCE / 'shisha'
PHONE_CALL = SOURCE / 'phone-call'
PLAYER = ROOT / 'assets/players/do_mixi'
FRAME = 192
PIVOT = (96, 172)


def shift_feet(frame):
    """Remove the preview's drawn lift. The game applies vertical movement."""
    image = Image.new('RGBA', frame.size)
    image.paste(frame, (0, PIVOT[1] - frame.getbbox()[3]))
    return image


def read_design():
    data = json.loads((SOURCE / 'frames.json').read_text(encoding='utf-8'))
    atlas = Image.open(SOURCE / 'do-mixi-sprites-v4.png').convert('RGBA')
    if data['frame_size'] != [FRAME, FRAME] or data['pivot'] != list(PIVOT):
        raise ValueError('The design must use 192x192 cels with feet pivot 96,172')
    if atlas.size != (data['columns'] * FRAME, data['rows'] * FRAME):
        raise ValueError('The design atlas does not match its grid')
    clips = {}
    for state, clip in data['clips'].items():
        cels = []
        for record in clip['frames']:
            x, y, w, h = record['atlas_rect']
            cell = atlas.crop((x, y, x + w, y + h))
            bounds = cell.getbbox()
            if (w, h) != (FRAME, FRAME) or not bounds or min(bounds[:2]) < 1 or max(bounds[2:]) >= FRAME:
                raise ValueError(f'Invalid or clipped Do Mixi drawing: {state}')
            cels.append(cell)
        if len(cels) != clip['frame_count'] or len({cel.tobytes() for cel in cels}) != len(cels):
            raise ValueError(f'Unexpected frame count or duplicate drawing: {state}')
        clips[state] = cels
    if len(clips['walk']) != 8 or len(clips['run']) != 16:
        raise ValueError('Expected the approved 8/16 frame walk/run cycles')
    return clips, data


def main():
    clips, design = read_design()
    smoke_atlas = Image.open(SHISHA / 'do-mixi-shisha-poses.png').convert('RGBA')
    if smoke_atlas.size != (FRAME * 6, FRAME):
        raise ValueError('Expected six 192x192 shisha poses')
    smoke_poses = [smoke_atlas.crop((i * FRAME, 0, (i + 1) * FRAME, FRAME)) for i in range(6)]
    if any(pose.getbbox() is None or pose.getbbox()[3] != PIVOT[1] for pose in smoke_poses):
        raise ValueError('Shisha poses must use the same feet pivot as other clips')
    call_atlas = Image.open(PHONE_CALL / 'o-call-poses.png').convert('RGBA')
    if call_atlas.size != (FRAME * 4, FRAME):
        raise ValueError('Expected four 192x192 phone-call drawings')
    call_poses = [call_atlas.crop((i * FRAME, 0, (i + 1) * FRAME, FRAME)) for i in range(4)]
    if any(pose.getbbox() is None or pose.getbbox()[3] != PIVOT[1] for pose in call_poses):
        raise ValueError('Phone-call poses must use the same feet pivot as other clips')
    jump = clips.pop('jump_land')
    light, heavy, launcher, idle = (clips[state] for state in ('light_1', 'heavy', 'launcher', 'idle'))
    clips.update({
        'jump': [shift_feet(jump[i]) for i in (1, 2, 3)],
        'fall': [shift_feet(jump[i]) for i in (3, 4)],
        'land': [shift_feet(jump[5]), idle[0]],
        'attack': light,
        'light_2': heavy,
        'light_3': launcher,
        'air_attack': [shift_feet(jump[1]), shift_feet(jump[2]), light[2], light[3], shift_feet(jump[4]), idle[0]],
        'block': [idle[0], heavy[0], light[0], launcher[1], light[4], heavy[4]],
        'charge': [heavy[0], heavy[1], launcher[1], heavy[1], launcher[0], heavy[4]],
        'knockdown': clips['death'],
        # Four pickup/lift poses, one release pose, then two recovery poses.
        # The held jar is overlaid at runtime so its label reads in both directions.
        'skill_u': [idle[0], launcher[1], launcher[2], launcher[3], heavy[2], heavy[3], idle[0]],
        'skill_i': smoke_poses,
        # Hold the phone through wind-up and Active, then lower it in Recovery.
        'ultimate': [call_poses[i] for i in (0, 1, 2, 2, 2, 2, 3)],
    })
    clips['special'] = clips['skill_u']

    base = PLAYER / 'v4'
    props = dict(line.split('=', 1) for line in (base / 'animation.properties').read_text().splitlines()
                 if '=' in line and not line.startswith('#'))
    states = [key.removesuffix('.frames') for key in props if key.endswith('.frames')]
    props['version'] = '5'
    for state in clips:
        props[state + '.frames'] = str(len(clips[state]))
        if state in design['clips']:
            props[state + '.secondsPerFrame'] = f"{design['clips'][state]['preview_frame_ms'] / 1000:.3f}"
    props.update({'jump.startFrame': '0', 'jump.endFrame': '2'})
    props.update({'skill_i.activeFrame': '4', 'skill_i.recoveryFrame': '5'})
    props.update({'ultimate.activeFrame': '5', 'ultimate.recoveryFrame': '6'})
    for state in ('skill_u', 'special'):
        props[state + '.activeFrame'] = '4'
        props[state + '.recoveryFrame'] = '5'
    for state in ('attack', 'light_1', 'light_2', 'light_3', 'heavy', 'launcher', 'air_attack'):
        props[state + '.activeFrame'] = '2'
        props[state + '.recoveryFrame'] = '4' if state in ('attack', 'light_1', 'air_attack', 'light_3', 'launcher') else '3'

    # Preserve all frames of unchanged clips byte for byte. Assemble the
    # complete bank before publishing to the directory the game discovers.
    with TemporaryDirectory(prefix='do-mixi-', dir=PLAYER) as staging:
        stage = Path(staging)
        runtime = Image.new('RGBA', (FRAME * 16, FRAME * len(states)))
        manifest = {'actor': 'do_mixi', 'version': 5, 'frame_size': [FRAME, FRAME], 'pivot': list(PIVOT),
                    'source_atlas': str((SOURCE / 'do-mixi-sprites-v4.png').relative_to(ROOT).as_posix()),
                    'animations': {}}
        for row, state in enumerate(states):
            count = int(props[state + '.frames'])
            frames = []
            for index in range(count):
                name = f'{state}_{index + 1}.png'
                target = stage / name
                if state in clips:
                    frame = clips[state][index]
                    frame.save(target)
                else:
                    copyfile(base / name, target)
                    frame = Image.open(target).convert('RGBA')
                x, y = index * FRAME, row * FRAME
                runtime.paste(frame, (x, y))
                frames.append({'file': name, 'atlas_rect': [x, y, FRAME, FRAME],
                               'sha256': sha256(target.read_bytes()).hexdigest()})
            manifest['animations'][state] = {'row': row, 'frame_count': count, 'source': 'design' if state in clips else 'v4',
                                              'frames': frames}
        runtime.save(stage / 'atlas.png')
        copyfile(SOURCE / 'do-mixi-portrait.png', stage / 'portrait.png')
        copyfile(SOURCE / 'kho-ga-jar-pixel.png', stage / 'kho-ga-jar.png')
        copyfile(SHISHA / 'shisha-pixel.png', stage / 'shisha.png')
        copyfile(PHONE_CALL / 'anh-goi-dien-goc.png', stage / 'phone-call.png')
        (stage / 'manifest.json').write_text(json.dumps(manifest, indent=2), encoding='utf-8')
        (stage / 'animation.properties').write_text(''.join(f'{k}={v}\n' for k, v in props.items()), encoding='ascii')

        bank = PLAYER / 'v5'
        if not bank.exists():
            stage.rename(bank)
        else:
            for asset in stage.iterdir():
                asset.replace(bank / asset.name)
    print('Do Mixi V5 installed: 8 walk / 16 run; jar throw, smoke blast and phone call')


if __name__ == '__main__':
    main()
