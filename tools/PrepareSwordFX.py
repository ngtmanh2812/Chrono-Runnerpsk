"""Mechanically pack measured, image-generated sword FX into 11-frame banks.

No artwork is synthesized here: originals plus five authored in-betweens are
interleaved, cleaned, uniformly sampled with nearest-neighbour, and padded.
Run: python tools/PrepareSwordFX.py
"""
from pathlib import Path
import hashlib
import json
import numpy as np
from PIL import Image, ImageDraw, ImageFont, ImageOps
from scipy import ndimage

ROOT = Path(__file__).resolve().parents[1]
ART = ROOT / 'art/effects/phan_sword'
SOURCE = ART / 'source'
OUT = ROOT / 'assets/effects/phan_sword/v1'
KINDS = ['crescent', 'long_slash', 'cross_burst', 'smoke']
SIZES = [(128,128), (256,128), (384,256), (192,192)]
ENVELOPES = [(100,94), (220,88), (220,220), (160,140)]


def runs(values):
    ids = np.flatnonzero(values)
    if not len(ids): return []
    return [(int(a[0]), int(a[-1])+1) for a in
            np.split(ids, np.flatnonzero(np.diff(ids)>1)+1) if len(a)]


def cleaned(path):
    original = Image.open(path).convert('RGBA')
    arr = np.asarray(original).copy()
    arr[:,:,3] = np.where(arr[:,:,3]>=128,255,0)
    labels,_ = ndimage.label(arr[:,:,3]>0, np.ones((3,3)))
    sizes = np.bincount(labels.ravel())
    tiny = sizes<5; tiny[0] = False
    removed = int(tiny[labels].sum())
    arr[tiny[labels],3] = 0
    arr[arr[:,:,3]==0,:3] = 0
    return original, Image.fromarray(arr,'RGBA'), removed


def measured_source(name, columns):
    path = SOURCE / name
    raw, image, removed = cleaned(path)
    alpha = np.asarray(image)[:,:,3]>0
    gutters = [(a,b) for a,b in runs(~alpha.any(axis=1))
               if a>0 and b<image.height and b-a>=30]
    assert len(gutters)==3, (name,'row gutters',gutters)
    ys = [0] + [(a+b)//2 for a,b in gutters] + [image.height]
    xs = [round(i*image.width/columns) for i in range(columns+1)]
    rows=[]; measurements=[]
    for row, (y0,y1) in enumerate(zip(ys,ys[1:])):
        # Equal-width columns are accepted only after checking the PNG's real
        # alpha gutters, rather than assuming the generation obeyed its grid.
        for x in xs[1:-1]:
            assert not alpha[y0:y1,x-1:x+2].any(), (name,row,'column crosses art',x)
        row_mask=alpha[y0:y1]
        yy=np.flatnonzero(row_mask.any(axis=1))
        row_root_y=(int(yy[0])+int(yy[-1])+1)/2
        cells=[]; records=[]
        for column,(x0,x1) in enumerate(zip(xs,xs[1:])):
            im=image.crop((x0,y0,x1,y1))
            cells.append(im)
            records.append({'source_rect':[x0,y0,x1-x0,y1-y0],
                            'source_root':[(x1-x0)/2,row_root_y],
                            'visible_bounds':list(im.getbbox())})
        rows.append(cells)
        measurements.append({'row':row,'kind':KINDS[row],
                             'row_root_y':row_root_y,'cells':records})
    return rows, {'file':path.relative_to(ROOT).as_posix(),
                  'sha256':hashlib.sha256(path.read_bytes()).hexdigest(),
                  'width':raw.width,'height':raw.height,'mode':raw.mode,
                  'x_cuts':xs,'y_cuts':ys,'columns':columns,'rows':4,
                  'alpha_cutoff':128,'removed_tiny_pixels':removed,
                  'row_measurements':measurements}


def palette_for(images):
    pixels=np.concatenate([np.asarray(im)[:,:,:3][np.asarray(im)[:,:,3]>0]
                           for im in images])
    sample=Image.fromarray(pixels.reshape(1,len(pixels),3),'RGB')
    return sample.quantize(colors=128,method=Image.Quantize.MEDIANCUT,
                           dither=Image.Dither.NONE)


def main():
    OUT.mkdir(parents=True,exist_ok=True)
    (ART/'metadata').mkdir(parents=True,exist_ok=True)
    old,om=measured_source('fx-sheet.png',6)
    between,bm=measured_source('fx-inbetweens.png',5)
    metadata={'version':1,'frames_per_animation':11,'added_frames':5,
              'order':'original0,inbetween0,...,original4,inbetween4,original5',
              'resampling':'nearest-neighbour uniform; no per-frame normalization',
              'alpha':'binary, actual transparent channel','sources':[om,bm],
              'animations':{}}
    props=['version=1','alphaCutoff=128','resampling=nearest-neighbour']
    preview=Image.new('RGB',(150+160*11,4*170+28),'#101b2a')
    draw=ImageDraw.Draw(preview)
    font=ImageFont.truetype('C:/Windows/Fonts/arial.ttf',14)
    draw.text((8,6),'Phan sword effects: 6 originals + 5 authored in-betweens',fill='#a7e9ff',font=font)
    for row,kind in enumerate(KINDS):
        frames=[]; records=[]
        for index in range(11):
            extra=index%2==1; column=index//2
            sheet=between if extra else old; measure=bm if extra else om
            im=sheet[row][column]
            record=dict(measure['row_measurements'][row]['cells'][column])
            record['source_file']=measure['file']
            record['source_kind']='authored-inbetween' if extra else 'original-authored'
            # Normalize source pixel density through its verified column span.
            # Every frame from this source has the same uniform scale factor.
            record['density_scale']=256/(measure['width']/measure['columns'])
            frames.append(im);records.append(record)
        extents=[]
        for im,record in zip(frames,records):
            x0,y0,x1,y1=im.getbbox();rx,ry=record['source_root'];d=record['density_scale']
            extents.append([(x0-rx)*d,(y0-ry)*d,(x1-rx)*d,(y1-ry)*d])
        max_x=max(max(abs(b[0]),abs(b[2])) for b in extents)
        max_y=max(max(abs(b[1]),abs(b[3])) for b in extents)
        uniform_scale=min(ENVELOPES[row][0]/(2*max_x),ENVELOPES[row][1]/(2*max_y))
        width,height=SIZES[row];pivot=(width/2,height/2)
        output=[]
        for im,record in zip(frames,records):
            scale=uniform_scale*record['density_scale'];rx,ry=record['source_root']
            affine=(1/scale,0,rx-pivot[0]/scale,0,1/scale,ry-pivot[1]/scale)
            out=im.transform((width,height),Image.Transform.AFFINE,affine,
                             resample=Image.Resampling.NEAREST)
            if kind=='smoke':
                # Gray impact smoke stays neutral; remove cyan fringe colour
                # only, preserving its authored alpha and plume animation.
                alpha=out.getchannel('A');gray=ImageOps.grayscale(out)
                out=Image.merge('RGBA',(gray,gray,gray,alpha))
            output.append(out)
            record['uniform_animation_scale']=uniform_scale
            record['effective_source_scale']=scale
            record['target_pivot']=list(pivot)
        palette=palette_for(output)
        atlas=Image.new('RGBA',(width*11,height))
        digests=set();frame_metadata=[]
        for index,(im,record) in enumerate(zip(output,records)):
            alpha=im.getchannel('A')
            im=im.convert('RGB').quantize(palette=palette,dither=Image.Dither.NONE).convert('RGBA')
            im.putalpha(alpha)
            arr=np.asarray(im).copy();arr[arr[:,:,3]==0,:3]=0
            im=Image.fromarray(arr,'RGBA')
            assert set(np.unique(arr[:,:,3])).issubset({0,255})
            assert all(im.getpixel(p)[3]==0 for p in [(0,0),(width-1,0),(0,height-1),(width-1,height-1)])
            bounds=im.getbbox();assert bounds and min(bounds[:2])>0 and bounds[2]<width and bounds[3]<height
            filename=f'{kind}_{index+1}.png';im.save(OUT/filename)
            digest=hashlib.sha256(im.tobytes()).hexdigest();digests.add(digest)
            atlas.paste(im,(index*width,0))
            frame_metadata.append(record|{'file':filename,'sha256':digest,
                                          'visible_bounds':list(bounds),
                                          'atlas_rect':[index*width,0,width,height]})
            thumb=im.copy();thumb.thumbnail((156,148),Image.Resampling.NEAREST)
            x=150+index*160+(160-thumb.width)//2
            y=28+row*170+(148-thumb.height)//2
            preview.paste(thumb,(x,y),thumb)
            draw.text((150+index*160+4,28+row*170+149),str(index+1),font=font,
                      fill='#80f3cc' if index%2 else '#e4e9ee')
        assert len(digests)==11, (kind,'duplicate frames')
        atlas.save(OUT/f'{kind}_atlas.png')
        metadata['animations'][kind]={'frames':11,'frame_width':width,'frame_height':height,
                                     'pivot':list(pivot),'uniform_group_scale':uniform_scale,
                                     'frame_metadata':frame_metadata}
        props.extend([f'{kind}.frames=11',f'{kind}.frameWidth={width}',f'{kind}.frameHeight={height}',
                      f'{kind}.pivotX={width//2}',f'{kind}.pivotY={height//2}'])
        draw.text((8,28+row*170+55),kind,font=font,fill='#a7e9ff')
        print(f'{kind}: 11 distinct {width}x{height} frames, alpha 0/255, scale={uniform_scale:.4f}')
    (OUT/'effects.properties').write_text('\n'.join(props)+'\n',encoding='ascii')
    (OUT/'manifest.json').write_text(json.dumps(metadata,indent=2),encoding='utf-8')
    (ART/'metadata/fx-measurements.json').write_text(json.dumps(metadata,indent=2),encoding='utf-8')
    preview.save(ART/'fx-preview.png')


if __name__=='__main__':main()
