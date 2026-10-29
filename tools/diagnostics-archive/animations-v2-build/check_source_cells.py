from PIL import Image
from pathlib import Path

def body(p):
    r,g,b,a=p
    return a>=128 and not(r>70 and r>g*1.45 and r>b*1.15) and not(g>110 and b>150 and r<170) and not(r>170 and g>90 and b<110) and not(b>170 and r>110 and g<110) and r+g+b<450

def cuts(proj,n,radius):
    result=[0]
    for i in range(1,n):
        expected=len(proj)*i//n
        candidates=range(max(result[-1]+8,expected-radius),min(len(proj)-8,expected+radius)+1)
        result.append(min(candidates,key=lambda k:(sum(proj[max(0,k-2):k+3]),abs(k-expected))))
    return result+[len(proj)]

for path in sorted(Path('art/animations/v2/source').glob('*-*.png')):
    if 'jump-apex' in path.name: continue
    im=Image.open(path).convert('RGBA'); w,h=im.size
    pix=list(im.get_flattened_data())
    rows=cuts([sum(pix[y*w+x][3]>=128 for x in range(w)) for y in range(h)],5,int(h*.09))
    print(path.name,im.size,'rows',rows)
    for r,(y0,y1) in enumerate(zip(rows,rows[1:])):
        proj=[sum(17 if body(pix[y*w+x]) else 1 if pix[y*w+x][3]>=128 else 0 for y in range(y0,y1)) for x in range(w)]
        cols=cuts(proj,6,int(w*.025))
        boxes=[im.crop((x0,y0,x1,y1)).getchannel('A').point(lambda a:255 if a>=128 else 0).getbbox() for x0,x1 in zip(cols,cols[1:])]
        print(' row',r,'cols',cols,'boxes',boxes)
