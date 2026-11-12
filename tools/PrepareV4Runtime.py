"""Install measured V4 animation source into runtime banks, without gameplay edits.

python tools/PrepareV4Runtime.py --import-source <design-pack-folder>
Subsequent runs only need the checked-in art/animations/v4 source and metadata.
Operations: alpha cleanup, measured slicing, body-root normalization, uniform
nearest-neighbour resampling, common palette, fixed-pivot padding and aliases.
"""
from pathlib import Path
import argparse
import hashlib
import json
import shutil
import numpy as np
from PIL import Image, ImageDraw, ImageFont
from scipy import ndimage

PROJECT = Path(__file__).resolve().parents[1]
ART = PROJECT/'art/animations/v4'
SOURCE = ART/'source'
META = ART/'metadata'
PREVIEW = ART/'previews'
ACTORS = ['phan_si_kien','ronaldo','do_mixi','the_time_guardian','the_void_core']
A = ['idle','walk','run','jump','fall','land','dash','charge','block','hurt']
B = ['light','heavy','launcher','air_attack','skill_u','skill_i','ultimate','new_skill','knockdown','death']
PLAYER_ORDER = ['idle','run','jump','attack','hurt','death','walk','special','block','dash','fall','land','light_1','light_2','light_3','heavy','launcher','air_attack','charge','skill_u','skill_i','ultimate','new_skill','knockdown']
BOSS_ORDER = ['idle','run','jump','attack','hurt','death','walk','special','block','dash','fall','land','light','heavy','launcher','air_attack','charge','skill_u','skill_i','ultimate','new_skill','knockdown']
LOOPS = {'idle','walk','run','fall','charge'}
FONT = ImageFont.truetype('C:/Windows/Fonts/arial.ttf',14)

def relative(path):
    return path.relative_to(PROJECT).as_posix()

def bbox(mask):
    yy,xx = np.nonzero(mask)
    if not len(xx): raise ValueError('Empty body mask')
    return [int(xx.min()),int(yy.min()),int(xx.max()+1),int(yy.max()+1)]

def components(mask):
    labels,count = ndimage.label(mask,np.ones((3,3),dtype=np.uint8))
    sizes = np.bincount(labels.ravel())
    return labels,sizes

def clean(source, actor, part):
    arr = np.asarray(source.convert('RGBA')).copy()
    arr[:,:,3] = np.where(arr[:,:,3]>=128,255,0)
    removed = 0
    # Measured source defect: three violet VFX pixels cross the empty fast-
    # strafe separator. They are not armor/core pixels. Keep original PNG intact.
    if actor=='the_void_core' and part=='A':
        for x,y in [(392,521),(392,537),(392,538)]:
            assert tuple(arr[y,x,:3]) in [(217,38,248),(175,50,241),(164,55,240)]
            arr[y,x,3] = 0
            removed += 1
    labels,sizes = components(arr[:,:,3]>0)
    tiny = (sizes<5); tiny[0] = False
    specks = tiny[labels]
    removed += int(specks.sum())
    arr[specks,3] = 0
    arr[arr[:,:,3]==0,:3] = 0
    return Image.fromarray(arr,'RGBA'), removed

def body_mask(im,actor):
    a = np.asarray(im)
    rgb = a[:,:,:3].astype(np.int16)
    r,g,b = [rgb[:,:,i] for i in range(3)]
    mask = a[:,:,3]>0
    mask &= ~((g>110)&(b>125)&(r<125)&(g>r*1.35))
    mask &= ~((b>165)&(r>120)&(g<110))
    mask &= ~((r>210)&(g>125)&(b<110)&(r-g>45))
    mask &= ~((g>170)&(g>r*1.6)&(g>b*1.2))
    if actor in {'phan_si_kien','the_time_guardian'}:
        mask &= ~((r>70)&(r>g*1.45)&(r>b*1.15))
    labels,sizes = components(mask)
    if len(sizes)<=1: raise ValueError('No connected costume')
    if actor=='ronaldo':
        red = (r>90)&(r>g*1.35)&(r>b*1.25)&mask
        score = sizes + np.bincount(labels[red].ravel(),minlength=len(sizes))*5
    else:
        # Filtering bright costume highlights can disconnect the head, jacket
        # and boots. Keep their substantial islands together rather than
        # calibrating scale to one leg or the hair alone.
        keep=sizes>=12;keep[0]=False
        return keep[labels]
    score[0]=0
    return labels==int(score.argmax())

def root_for(im,actor,state,ref_height):
    mask=body_mask(im,actor)
    x0,y0,x1,y1=bbox(mask)
    arr=np.asarray(im)
    if actor=='the_void_core':
        # Reactor is bright pink/cyan inside the lower shell, not the upper
        # halo. Find its compact bright cluster near the shell's middle.
        rgb=arr[:,:,:3].astype(np.int16)
        r,g,b=[rgb[:,:,i] for i in range(3)]
        yy,xx=np.indices(mask.shape)
        bright=(arr[:,:,3]>0)&(r>130)&(g>100)&(b>175)
        bright&=(xx>x0+(x1-x0)*.20)&(xx<x1-(x1-x0)*.15)&(yy>y0+(y1-y0)*.35)&(yy<y0+(y1-y0)*.85)
        labels,sizes=components(bright)
        candidates=[]
        ex=(x0+x1)/2;ey=y0+(y1-y0)*.61
        for component in range(1,len(sizes)):
            if sizes[component]<3: continue
            ys,xs=np.nonzero(labels==component)
            cx,cy=float(xs.mean()),float(ys.mean())
            candidates.append((((cx-ex)**2+(cy-ey)**2)/max(1,sizes[component]**.25),cx,cy))
        if candidates and state!='death':
            _,rx,ry=min(candidates)
        else:
            rx=(x0+x1)/2;ry=y1-ref_height*.55
        return [rx,ry],mask
    yy,xx=np.indices(mask.shape)
    if actor=='ronaldo':
        rgb=arr[:,:,:3].astype(np.int16);r,g,b=[rgb[:,:,i] for i in range(3)]
        red=mask&(r>90)&(r>g*1.35)&(r>b*1.25)&(yy>y0+(y1-y0)*.18)&(yy<y0+(y1-y0)*.6)
        rx=float(xx[red].mean()) if red.any() else (x0+x1)/2
    elif state in {'death','knockdown'}:
        rx=im.width/2
    else:
        feet=mask&(yy>=y1-max(5,ref_height*.15))
        rx=float(xx[feet].mean()) if feet.any() else (x0+x1)/2
    return [rx,float(y1)],mask

def old_properties(actor):
    category='bosses' if actor.startswith('the_') else 'players'
    bank=PROJECT/'assets'/category/actor/('v2' if category=='bosses' else 'v3')
    p={}
    for line in (bank/'animation.properties').read_text().splitlines():
        if '=' in line and not line.startswith('#'):
            k,v=line.split('=',1);p[k]=v
    return bank,p

def make_palette(images):
    opaque=np.concatenate([np.asarray(im)[:,:,:3][np.asarray(im)[:,:,3]>0] for im in images])
    width=1024; height=(len(opaque)+width-1)//width
    values=np.zeros((height*width,3),dtype=np.uint8);values[:len(opaque)]=opaque
    return Image.fromarray(values.reshape(height,width,3),'RGB').quantize(colors=128,method=Image.Quantize.MEDIANCUT,dither=Image.Dither.NONE)

def remap(im,palette):
    alpha=im.getchannel('A')
    out=im.convert('RGB').quantize(palette=palette,dither=Image.Dither.NONE).convert('RGBA')
    out.putalpha(alpha)
    return out

def fit(im,mask,root,scale,size,pivot,target_offset=(0,0)):
    # One uniform actor scale, rounded only by nearest-neighbour sample mapping.
    affine=(1/scale,0,root[0]-(pivot[0]+target_offset[0])/scale,0,1/scale,root[1]-(pivot[1]+target_offset[1])/scale)
    out=im.transform((size,size),Image.Transform.AFFINE,affine,resample=Image.Resampling.NEAREST)
    m=Image.fromarray((mask*255).astype(np.uint8),'L').transform((size,size),Image.Transform.AFFINE,affine,resample=Image.Resampling.NEAREST)
    # Verify transformed opaque extents remain inside the canvas before drawing.
    b=im.getbbox(); left=pivot[0]+target_offset[0]+(b[0]-root[0])*scale; right=pivot[0]+target_offset[0]+(b[2]-root[0])*scale
    top=pivot[1]+target_offset[1]+(b[1]-root[1])*scale;bottom=pivot[1]+target_offset[1]+(b[3]-root[1])*scale
    if min(left,top)<0 or max(right,bottom)>size:
        raise ValueError(f'Frame would clip: {left,right,top,bottom}')
    return out,bbox(np.asarray(m)>0),[round(v,3) for v in [left,top,right,bottom]]

def refine_fractional_edge(im,root,scale,size,pivot,offset):
    """Resolve <1.5 output-pixel rounding at an extreme VFX endpoint only."""
    b=im.getbbox();xs=[pivot[0]+offset[0]+(v-root[0])*scale for v in (b[0],b[2])]
    ys=[pivot[1]+offset[1]+(v-root[1])*scale for v in (b[1],b[3])]
    dx=-xs[0] if xs[0]<0 else (size-xs[1]-.01 if xs[1]>size else 0)
    dy=-ys[0] if ys[0]<0 else (size-ys[1]-.01 if ys[1]>size else 0)
    if max(abs(dx),abs(dy))>1.5:
        return root,[0,0] # Main fit raises and reports larger geometry problems.
    return [root[0]-dx/scale,root[1]-dy/scale],[dx,dy]

def actor_pack(actor,measurements):
    category='bosses' if actor.startswith('the_') else 'players'
    old,props=old_properties(actor)
    directory=PROJECT/'assets'/category/actor/'v4';directory.mkdir(parents=True,exist_ok=True)
    size=192 if category=='players' else 256
    pivot=(96,172) if category=='players' else ((128,128) if actor=='the_void_core' else (128,232))
    source_cells={}; sources=[]; source_images=[]
    for part,states in [('A',A),('B',B)]:
        path=SOURCE/f'{actor}-{part}.png';meta=measurements[path.name]
        raw=Image.open(path).convert('RGBA');assert raw.size==(meta['width'],meta['height'])
        clean_im,removed=clean(raw,actor,part)
        sources.append({'file':relative(path),'width':raw.width,'height':raw.height,'sha256':hashlib.sha256(path.read_bytes()).hexdigest(),'removed_alpha_speck_pixels':removed,'y_cuts':meta['y_cuts']})
        for row,state in enumerate(states):
            source_cells[state]=[]
            for column,item in enumerate(meta['row_measurements'][row]['cells']):
                x,y,w,h=item['rect'];im=clean_im.crop((x,y,x+w,y+h));source_images.append(im)
                source_cells[state].append({'image':im,'source_file':relative(path),'source_rect':[x,y,w,h]})
    # Reference measurement uses only six upright idle body masks; excludes VFX.
    heights=[]
    for c in source_cells['idle']:
        bb=bbox(body_mask(c['image'],actor));heights.append(bb[3]-bb[1])
    if actor=='the_void_core':
        # The lower shell starts below the halo. 68px is the existing torso
        # envelope used by combat geometry; preserve its author-space scale.
        ref_height=float(np.median(heights))*.70;target_height=68
    else:
        ref_height=float(np.median(heights));target_height=94 if actor=='the_time_guardian' else 64
    scale=target_height/ref_height
    palette=make_palette(source_images)
    clips={};records={}
    for state,cells in source_cells.items():
        name='light_1' if category=='players' and state=='light' else state
        clips[name]=[];records[name]=[]
        for i,c in enumerate(cells):
            root,mask=root_for(c['image'],actor,state,ref_height)
            # Core remains centre-anchored, but its extinguished wreck must
            # settle on the same arena floor as its unchanged 140px physics
            # body: root=ground-94 at scale2 -> author bottom175.
            offset=(3,10 if state=='death' else 4) if actor=='the_void_core' else (0,0)
            root,adjustment=refine_fractional_edge(c['image'],root,scale,size,pivot,offset)
            try:out,body,extent=fit(c['image'],mask,root,scale,size,pivot,offset)
            except ValueError as error:raise ValueError(f'{actor}/{state}/{i+1}: {error}') from error
            out=remap(out,palette);clips[name].append(out)
            records[name].append({k:v for k,v in c.items() if k!='image'}|{'source_body_bounds':bbox(mask),'source_root':root,'scale':scale,'target_pivot':list(pivot),'art_offset':list(offset),'target_body_bounds':body,'transformed_visible_bounds':extent,'fractional_edge_adjustment':adjustment,'source_kind':'v4-authored'})
    if category=='players':
        for state in ['light_2','light_3']:
            clips[state]=[];records[state]=[]
            for i in range(1,7):
                path=old/f'{state}_{i}.png';original=Image.open(path).convert('RGBA');assert original.size==(128,128)
                im=Image.new('RGBA',(size,size));im.paste(original,(pivot[0]-64,pivot[1]-116))
                clips[state].append(im);records[state].append({'source_file':relative(path),'source_kind':'retained-distinct-v3','scale':1,'source_root':[64,116],'padding_offset':[pivot[0]-64,pivot[1]-116],'source_rect':[0,0,128,128]})
    for alias,origin in [('attack','light_1' if category=='players' else 'light'),('special','skill_u' if category=='players' else ('ultimate' if actor=='the_void_core' else 'skill_i'))]:
        clips[alias]=clips[origin];records[alias]=[dict(v,alias_of=origin) for v in records[origin]]
    order=PLAYER_ORDER if category=='players' else BOSS_ORDER
    atlas=Image.new('RGBA',(size*6,size*len(order)))
    animations={}
    for row,state in enumerate(order):
        unique=set();frame_meta=[]
        for i,im in enumerate(clips[state]):
            assert im.getbbox() is not None and set(np.unique(np.asarray(im)[:,:,3])).issubset({0,255})
            file=f'{state}_{i+1}.png';im.save(directory/file);atlas.paste(im,(i*size,row*size))
            digest=hashlib.sha256(im.tobytes()).hexdigest();unique.add(digest)
            frame_meta.append(records[state][i]|{'file':file,'atlas_rect':[i*size,row*size,size,size],'visible_bounds':list(im.getbbox()),'sha256':digest})
        assert len(unique)==6,(actor,state,'duplicate frames')
        fallback=props.get(({'light':'attack','skill_u':'attack','skill_i':'special','ultimate':'special'}.get(state,state))+'.secondsPerFrame','0.1')
        seconds=props.get(state+'.secondsPerFrame',fallback)
        loop=props.get(state+'.loop',str(state in LOOPS).lower())
        if state in {'new_skill','knockdown'}:loop='false'
        animations[state]={'row':row,'frames':6,'loop':loop=='true','hold_last':loop!='true','seconds_per_frame':float(seconds),'frame_metadata':frame_meta}
        props[state+'.secondsPerFrame']=seconds;props[state+'.loop']=loop;props[state+'.frames']='6'
    atlas.save(directory/'atlas.png')
    props.update({'version':'4','frameWidth':str(size),'frameHeight':str(size),'pivotX':str(pivot[0]),'pivotY':str(pivot[1]),'combat.activeFrame':'3','combat.recoveryFrame':'5'})
    if category=='players':props.update({'jump.startFrame':'2','jump.endFrame':'5'})
    if actor=='the_void_core':props.update({'skill_i.startFrame':'0','skill_i.endFrame':'3'})
    if category=='players':props.update({'light_2.activeFrame':'2','light_2.recoveryFrame':'4','light_3.activeFrame':'2','light_3.recoveryFrame':'4'})
    text='\n'.join(f'{k}={v}' for k,v in props.items())+'\n'
    (directory/'animation.properties').write_text(text,encoding='ascii')
    manifest={'version':4,'status':'runtime-v4','actor':actor,'frame_width':size,'frame_height':size,'columns':6,'rows':len(order),'pivot':list(pivot),'anchor':props['anchor'],'render_scale':int(props['scale']),'offset_y':int(props['offsetY']),'uniform_source_scale':scale,'idle_body_reference_height':ref_height,'target_body_height':target_height,'resampling':'nearest-neighbour uniform','alpha_cutoff':128,'palette_colors':128,'reserved_clips':['new_skill','knockdown'],'retained_distinct_v3_clips':['light_2','light_3'] if category=='players' else [],'sources':sources,'animations':animations}
    (directory/'manifest.json').write_text(json.dumps(manifest,ensure_ascii=False,indent=2),encoding='utf-8')
    (directory/'manifest.txt').write_text(f'V4 runtime bank: {actor}\n{size}x{size}; pivot={pivot}; anchor={props["anchor"]}; scale={props["scale"]}.\n20 V4 authored clips; runtime aliases included. New_skill and knockdown reserved; no new gameplay logic.\nSource coordinates/roots/palette/body calibration are in manifest.json.\n',encoding='utf-8')
    # Review contact sheet only; no debug labels or backing enter runtime PNGs.
    thumb=size if category=='players' else 128
    contact=Image.new('RGB',(150+thumb*6,24+len(order)*(thumb+12)),'#06172b');d=ImageDraw.Draw(contact)
    d.text((5,4),actor,font=FONT,fill='#a7e9ff')
    for row,state in enumerate(order):
        y=24+row*(thumb+12);d.text((5,y+10),state,font=FONT,fill='#69ccf0')
        for i,im in enumerate(clips[state]):
            pic=im if im.width==thumb else im.resize((thumb,thumb),Image.Resampling.NEAREST)
            contact.paste(pic,(150+i*thumb,y),pic)
    contact.save(PREVIEW/f'{actor}-runtime-contact.png')
    print(json.dumps({'actor':actor,'size':size,'scale':round(scale,5),'source_body':round(ref_height,2),'target_body':target_height,'clips':len(order),'idle_body_output':[records['idle'][0]['target_body_bounds']],'directory':relative(directory)}),flush=True)
    return manifest

def main():
    parser=argparse.ArgumentParser();parser.add_argument('--import-source',type=Path);parser.add_argument('--actors',default=','.join(ACTORS));args=parser.parse_args()
    for p in [SOURCE,META,PREVIEW]:p.mkdir(parents=True,exist_ok=True)
    if args.import_source:
        for file in (args.import_source/'source').glob('*'):
            if file.suffix=='.png' or file.name.endswith('.prompt.txt'):shutil.copyfile(file,SOURCE/file.name)
        shutil.copyfile(args.import_source/'source-measurements.json',META/'source-measurements.json')
        shutil.copyfile(args.import_source/'PROMPTS.md',ART/'PROMPTS.md')
    measurements={m['file']:m for m in json.loads((META/'source-measurements.json').read_text())}
    selected=args.actors.split(',');assert all(a in ACTORS for a in selected)
    manifests=[actor_pack(a,measurements) for a in selected]
    # Reinstall the newer authored sword poses after rebuilding the base bank.
    # Importing this helper never runs this tool's main or generates artwork.
    sword_source=PROJECT/'art/effects/phan_sword/source/skill-inbetweens.png'
    if 'phan_si_kien' in selected and sword_source.exists():
        from PreparePhanSwordEffects import apply_skill_pose_override
        apply_skill_pose_override()
    report={'actors':len(manifests),'runtime_banks':{m['actor']:{'frame_size':[m['frame_width'],m['frame_height']],'rows':m['rows'],'scale':m['uniform_source_scale']} for m in manifests},'preserved_gameplay':True,'source_root':'art/animations/v4/source','source_defects_cleaned':{'the_void_core-A.png':[[392,521],[392,537],[392,538]]}}
    (META/'runtime-validation.json').write_text(json.dumps(report,indent=2),encoding='utf-8')

if __name__=='__main__':main()
