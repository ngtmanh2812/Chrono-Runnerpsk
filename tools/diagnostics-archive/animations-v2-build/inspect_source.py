# Read-only layout inspection; no images are altered by this script.
from PIL import Image
from pathlib import Path
import json
for path in sorted(Path('art/animations/v2/source').glob('*.png')):
    image = Image.open(path).convert('RGBA')
    w,h = image.size
    pixels = list(image.get_flattened_data())
    projection = [sum(pixels[y*w+x][3] >= 128 for x in range(w)) for y in range(h)]
    bands=[]; start=None
    for y,count in enumerate(projection+[0]):
        if count>2 and start is None: start=y
        if count<=2 and start is not None:
            if y-start>8: bands.append([start,y])
            start=None
    valleys=[]
    for i in range(1,5):
        lo=max(0,int(h*i/5-h*.085)); hi=min(h,int(h*i/5+h*.085))
        candidates=sorted(range(lo,hi),key=lambda y:(sum(projection[max(0,y-2):min(h,y+3)]),abs(y-h*i/5)))
        valleys.append([candidates[0],projection[candidates[0]]])
    print(json.dumps({'file':path.name,'size':[w,h],'bands':bands,'cuts':valleys}))
