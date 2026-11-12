"""Measure and pack the supplied Daddy strips; no generated artwork."""
from pathlib import Path
import json,hashlib
import numpy as np
from PIL import Image,ImageDraw

ROOT=Path(__file__).resolve().parents[1]
SOURCE=ROOT/'art/bosses/daddy/source'
OUT=ROOT/'assets/bosses/the_void_core/v5'
ORDER=['idle','run','jump','attack','hurt','death','walk','special','block','dash','fall','land','light','heavy','launcher','air_attack','charge','skill_u','skill_i','ultimate','new_skill','knockdown']
MAP={'idle':'Charged','run':'Walk','walk':'Walk','jump':'Charhedv2','fall':'Charhedv2','land':'Walk',
 'attack':'Shoot','light':'Shoot','heavy':'Charged','launcher':'Charged','air_attack':'Charhedv2',
 'hurt':'Charhedv2','death':'Charhedv2','special':'Scream','block':'Charged','dash':'Charhedv2',
 'charge':'Charged','skill_u':'Charged','skill_i':'Charhedv2','ultimate':'Scream','new_skill':'Scream','knockdown':'Charhedv2'}

def main():
 OUT.mkdir(parents=True,exist_ok=True);frames={};sources=[]
 for name,fw,fh,count in [('Walk',146,180,9),('Shoot',176,176,8),('Charged',168,176,8),('Charhedv2',160,176,8),('Scream',154,176,24)]:
  path=SOURCE/(name+'.png');im=Image.open(path).convert('RGBA');assert im.size==(fw,fh*count)
  first=np.array(im.crop((0,0,fw,fh)));yy,xx=np.nonzero((first[:,:,3]>128)&(np.indices(first.shape[:2])[0]>=fh-16))
  pivot=float(np.mean(xx)) if len(xx) else fw/2
  scale=94/fh;items=[]
  for i in range(count):
   tile=im.crop((0,i*fh,fw,(i+1)*fh));assert tile.getbbox()
   affine=(1/scale,0,pivot-128/scale,0,1/scale,fh-232/scale)
   out=tile.transform((256,256),Image.Transform.AFFINE,affine,resample=Image.Resampling.NEAREST)
   items.append(out.transpose(Image.Transpose.FLIP_LEFT_RIGHT))
  frames[name]=items
  sources.append({'file':path.relative_to(ROOT).as_posix(),'size':list(im.size),'frame_size':[fw,fh],
   'columns':1,'rows':count,'pivot':[pivot,fh],'uniform_scale':scale,'mirrored_horizontal':True,
   'sha256':hashlib.sha256(path.read_bytes()).hexdigest()})
 props={'version':'5','frameWidth':'256','frameHeight':'256','pivotX':'128','pivotY':'232','scale':'2','anchor':'feet','offsetY':'0'}
 atlas=Image.new('RGBA',(24*256,len(ORDER)*256));manifest={'actor':'the_void_core','pack':'Daddy','sources':sources,'animations':{}}
 preview=Image.new('RGB',(120+24*100,22+len(ORDER)*112),'#102237');draw=ImageDraw.Draw(preview)
 for row,state in enumerate(ORDER):
  source=MAP[state];indices=list(range(len(frames[source])))
  if state=='idle':indices=[0,1,2,1]
  if state in {'hurt','knockdown'}:indices=[5,6,7]
  if state in {'jump','fall','land'}:indices=[0,1,2]
  if state=='death':indices=[4,5,6,7,7,7,7,7]
  loop=state in {'idle','walk','run','fall','charge'}
  seconds=.08 if state=='run' else .10 if state=='walk' else .12 if state=='idle' else .075
  props.update({state+'.frames':str(len(indices)),state+'.loop':str(loop).lower(),state+'.secondsPerFrame':str(seconds)})
  if state in {'attack','light','heavy','launcher','air_attack','special','skill_u','skill_i','ultimate','new_skill'}:
   props.update({state+'.activeFrame':'10' if source=='Scream' else '3',state+'.recoveryFrame':'20' if source=='Scream' else '6'})
  records=[];draw.text((4,22+row*112),state,fill='#a0efff')
  for n,index in enumerate(indices):
   im=frames[source][index].copy()
   if state=='death':im.putalpha(im.getchannel('A').point(lambda a,f=1-n/7:round(a*f)))
   name=f'{state}_{n+1}.png';im.save(OUT/name);atlas.paste(im,(n*256,row*256))
   thumb=im.resize((100,100),Image.Resampling.NEAREST);preview.paste(thumb,(120+n*100,22+row*112),thumb)
   records.append({'file':name,'source':source,'source_frame':index,'sha256':hashlib.sha256(im.tobytes()).hexdigest()})
  manifest['animations'][state]={'frames':len(indices),'source':source,'derived_fade':state=='death','frames_metadata':records}
 atlas.save(OUT/'atlas.png');preview.save(ROOT/'art/bosses/daddy/runtime-preview.png')
 Image.open(SOURCE/'Bestiary.png').save(OUT/'portrait.png')
 (OUT/'animation.properties').write_text(''.join(f'{k}={v}\n' for k,v in props.items()),encoding='ascii')
 (OUT/'manifest.json').write_text(json.dumps(manifest,indent=2),encoding='utf-8')
 print('Daddy V5: 22 clips; Walk9 / Shoot8 / Charged8 / Charhedv2 8 / Scream24; 256px feet anchor')

if __name__=='__main__': main()
