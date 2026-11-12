"""Install user-supplied PhyschoBoss strips; no generated artwork or AI changes."""
from pathlib import Path
import hashlib
import json
from PIL import Image, ImageDraw

ROOT=Path(__file__).resolve().parents[1]
SOURCE=ROOT/'art/bosses/physcho/source'
OUT=ROOT/'assets/bosses/the_time_guardian/v5'
ORDER=['idle','run','jump','attack','hurt','death','walk','special','block','dash','fall','land','light','heavy','launcher','air_attack','charge','skill_u','skill_i','ultimate','new_skill','knockdown']
MAPPING={'idle':'idle','walk':'walk','run':'walk','jump':'dash','fall':'dash','land':'idle',
 'dash':'dash','attack':'barrelroll','light':'dash','heavy':'barrelroll',
 'launcher':'barrelroll','air_attack':'barrelroll','special':'charged','skill_i':'barrelroll',
 'skill_u':'charged','ultimate':'charged','charge':'charged','block':'charged',
 'new_skill':'charged','hurt':'charged','knockdown':'charged','death':'charged'}

def main():
 OUT.mkdir(parents=True,exist_ok=True)
 strips={}; sources=[]
 for name,count in [('idle',6),('walk',13),('dash',8),('barrelroll',8),('charged',10)]:
  path=SOURCE/(name+'.png');im=Image.open(path).convert('RGBA')
  assert im.size==(128,140*count),(name,im.size)
  assert im.getchannel('A').getextrema()==(0,255)
  frames=[]
  for i in range(count):
   cell=im.crop((0,i*140,128,(i+1)*140))
   # Fixed source origin (64,140), one uniform scale for every pose. Visible
   # body height becomes 94 author pixels / 188 world pixels, like V4.
   ratio=94/140
   affine=(1/ratio,0,64-128/ratio,0,1/ratio,140-232/ratio)
   frame=cell.transform((256,256),Image.Transform.AFFINE,affine,resample=Image.Resampling.NEAREST)
   # Supplied poses face left. Runtime banks must face right; the renderer
   # then mirrors them when the boss targets a player to its left.
   frame=frame.transpose(Image.Transpose.FLIP_LEFT_RIGHT)
   frames.append(frame)
  strips[name]=frames
  sources.append({'file':path.relative_to(ROOT).as_posix(),'width':im.width,'height':im.height,
   'columns':1,'rows':count,'frame_width':128,'frame_height':140,
   'sha256':hashlib.sha256(path.read_bytes()).hexdigest(),
   'rects':[[0,i*140,128,140] for i in range(count)]})
 props={'version':'5','frameWidth':'256','frameHeight':'256','pivotX':'128','pivotY':'232',
        'scale':'2','anchor':'feet','offsetY':'0'}
 manifest={'actor':'the_time_guardian','source_pack':'PhyschoBoss','version':5,
  'sources':sources,'source_pivot':[64,140],'source_mirrored_horizontal':True,'uniform_source_scale':94/140,
  'frame_width':256,'frame_height':256,'pivot':[128,232],'render_scale':2,
  'resampling':'nearest-neighbour','columns':13,'rows':len(ORDER),'animations':{}}
 atlas=Image.new('RGBA',(256*13,256*len(ORDER)))
 preview=Image.new('RGB',(130+128*13,22+len(ORDER)*140),'#102237');draw=ImageDraw.Draw(preview)
 for row,state in enumerate(ORDER):
  source=MAPPING[state];frames=strips[source];indices=list(range(len(frames)))
  if state in {'hurt','knockdown'}: indices=[6,7,8,9]
  if state=='jump': indices=[0,1,2,3]
  if state=='fall': indices=[4,5,6,7]
  if state=='land': indices=[3,4,5]
  selected=[frames[i].copy() for i in indices]
  if state=='death':
   indices=[6,7,8,9,9,9,9,9]
   selected=[]
   for i,index in enumerate(indices):
    im=frames[index].copy();alpha=im.getchannel('A')
    im.putalpha(alpha.point(lambda a,fade=1-i/7:round(a*fade)))
    selected.append(im)
  looping=state in {'idle','walk','run','charge','fall'}
  seconds=.07 if state=='run' else .10 if state=='walk' else .14 if state=='idle' else .09
  props.update({state+'.frames':str(len(selected)),state+'.secondsPerFrame':str(seconds),state+'.loop':str(looping).lower()})
  if state in {'light','heavy','launcher','attack','air_attack','skill_u','skill_i','ultimate','special','new_skill'}:
   active=4 if source=='charged' else 2 if source=='dash' else 3
   recovery=8 if source=='charged' else 6
   props.update({state+'.activeFrame':str(active),state+'.recoveryFrame':str(recovery)})
  records=[];draw.text((3,22+row*140),state,fill='#9ce8ff')
  for n,im in enumerate(selected):
   name=f'{state}_{n+1}.png';im.save(OUT/name);atlas.paste(im,(n*256,row*256))
   thumb=im.resize((128,128),Image.Resampling.NEAREST);preview.paste(thumb,(130+n*128,22+row*140),thumb)
   records.append({'file':name,'source':source,'source_frame':indices[n],
    'source_rect':[0,indices[n]*140,128,140],'atlas_rect':[n*256,row*256,256,256],
    'sha256':hashlib.sha256(im.tobytes()).hexdigest(),'visible_bounds':im.getbbox()})
  manifest['animations'][state]={'row':row,'frames':len(selected),'loop':looping,
   'derived_fade':state=='death','frame_metadata':records}
 atlas.save(OUT/'atlas.png');preview.save(ROOT/'art/bosses/physcho/runtime-preview.png')
 # The original projectile is already 48x48 RGBA; preserve its pixel art exactly.
 projectile=Image.open(SOURCE/'Projectile/BrutHide.png').convert('RGBA')
 assert projectile.size==(48,48)
 projectile.save(OUT/'projectile.png')
 Image.open(SOURCE/'PsychoBoss_Head_Boss.png').save(OUT/'portrait.png')
 (OUT/'animation.properties').write_text(''.join(f'{k}={v}\n' for k,v in props.items()),encoding='ascii')
 (OUT/'manifest.json').write_text(json.dumps(manifest,indent=2),encoding='utf-8')
 print('PhyschoBoss: five measured strips, 22 complete runtime clips, V5, 256x256 pivot128,232 scale2')

if __name__=='__main__': main()
