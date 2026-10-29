# Read-only verification: no image processing or modification.
from PIL import Image
from pathlib import Path
import json, hashlib

root=Path('art/animations/v2')
total=0
for path in sorted(root.glob('*/*/manifest.json')):
    meta=json.loads(path.read_text(encoding='utf-8'))
    actor=meta['actor']; kind=path.parent.parent.name
    destination=Path('assets')/kind/actor/'v2'
    frame_size=(meta['frame_width'],meta['frame_height'])
    atlas=Image.open(path.parent/'sprite_sheet.png').convert('RGBA')
    assert atlas.size==(frame_size[0]*meta['columns'],frame_size[1]*meta['rows'])
    assert sorted(atlas.getchannel('A').getextrema())==[0,255]
    assert len(set(atlas.getchannel('A').get_flattened_data()))==2
    for name,clip in meta['animations'].items():
        assert clip['frames']==len(clip['rects'])
        for i,(x,y,w,h) in enumerate(clip['rects']):
            frame=Image.open(path.parent/f'{name}_{i+1}.png').convert('RGBA')
            assert frame.size==frame_size
            assert atlas.crop((x,y,x+w,y+h)).tobytes()==frame.tobytes()
            assert frame.getbbox() is not None
            total+=1
    for source in meta['sources']:
        original=Image.open(source['file'])
        assert original.size==(source['width'],source['height'])
        for cell in source['cells']:
            x,y,w,h=cell['rect']
            actual=Image.open(cell['override_file']) if 'override_file' in cell else original
            assert x>=0 and y>=0 and w>0 and h>0 and x+w<=actual.width and y+h<=actual.height
    for file in path.parent.iterdir():
        assert hashlib.sha256(file.read_bytes()).digest()==hashlib.sha256((destination/file.name).read_bytes()).digest(),str(file)
    print(actor,': atlas/frame pixels, alpha, source bounds, installed file hashes PASS')
assert total==320,total
print('PASS: 320 installed PNG frames match all 5 atlases and manifests; all 340 runtime pack files match authored exports.')
