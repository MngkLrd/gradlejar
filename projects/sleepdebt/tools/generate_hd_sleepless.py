#!/usr/bin/env python3
"""Create independently painted 2048x1024 Sleepless texture from its exact UV."""
import math, struct, zlib
from pathlib import Path
S=8;W=256*S;H=128*S
OUT=Path("src/main/resources/assets/sleepdebt/textures/entity/sleepless.png")
img=bytearray(W*H*4)
def col(c):return bytes.fromhex(c[1:])+b"\xff"
def clamp(n):return max(0,min(255,int(n)))
# Full-resolution structured material, not an enlarged pixel-art texture.
for y in range(H):
    uy=y/S
    a=y*W*4
    band=4*math.sin(uy*.27)+3*math.cos(uy*.81)
    for x in range(W):
        ux=x/S
        f=3.6*math.sin(ux*.87+math.sin(uy*.36)*1.8)
        v=3.2*math.cos(uy*.54-ux*.22)
        pore=1.3*math.sin(x*.39+y*.16)+1.5*math.sin(x*.20-y*.29)
        b=41+f+v+pore+band
        q=a+x*4
        img[q:q+4]=bytes((clamp(b+2),clamp(b-3),clamp(b+5),255))
def box(x0,y0,x1,y1,c):
    a=max(0,round(x0*S));b=min(W,round(x1*S))
    pattern=col(c)*max(0,b-a)
    for y in range(max(0,round(y0*S)),min(H,round(y1*S))):
        k=(y*W+a)*4
        img[k:k+len(pattern)]=pattern
def oval(cx,cy,rx,ry,c):
    cr=col(c);rx=max(.1,rx*S);ry=max(.1,ry*S)
    for y in range(max(0,int(cy*S-ry)),min(H,int(cy*S+ry)+1)):
        fy=(y+.5-cy*S)/ry
        if abs(fy)>1:continue
        spread=rx*math.sqrt(1-fy*fy)
        a=max(0,int(cx*S-spread));b=min(W,int(cx*S+spread)+1)
        if a<b:
            q=(y*W+a)*4
            img[q:q+(b-a)*4]=cr*(b-a)
def line(x0,y0,x1,y1,c,thick=.13):
    n=max(1,int(math.hypot(x1-x0,y1-y0)*S))
    for i in range(n+1):
        t=i/n
        oval(x0+(x1-x0)*t,y0+(y1-y0)*t,thick,thick,c)
def bone(x0,y0,x1,y1,r=.22):
    line(x0+.09,y0,x1+.09,y1,"#16121B",r+.12)
    line(x0,y0,x1,y1,"#9F968E",r)
    line(x0-.08,y0,x1-.08,y1,"#DED0B8",r*.32)
def scar(x0,y0,x1,y1,r=.13):
    line(x0,y0,x1,y1,"#110811",r+.10)
    line(x0+.07,y0,x1+.07,y1,"#75343C",r*.66)
# Model skull texOffs(0,0), 10x15x10. FRONT x[10,20],y[10,25].
box(10,10,20,25,"#39323C")
oval(15,13.2,4.8,3.4,"#716467")
oval(15,17,4.5,4.9,"#241C28")
oval(12.7,16.0,2.2,2.6,"#04040A")
oval(17.35,15.5,2.0,2.8,"#040308")
oval(12.7,16.2,1.35,1.94,"#000002")
oval(17.3,15.4,1.4,2.0,"#000002")
oval(12.7,15.9,.25,.4,"#D8CCB6")
oval(17.4,15.2,.19,.33,"#C8B7A9")
bone(10.5,12.9,13.4,12,.25)
bone(16.5,12.1,19.4,13.1,.23)
oval(15,21.3,4,3.9,"#010003")
for j in range(10):
    x=11.35+j*.8
    bone(x,19.9+(j%2)*.22,x+.07,21.5+(j%3)*.32,.12)
    bone(x+.12,24.8,x+.11,23.6-(j%3)*.24,.10)
for p in [(10.8,12.3,12.7,14.3),(19.2,13.1,18.1,15.5),
          (11.0,20.4,12.8,24.7),(18.9,19.1,17.7,24.5)]:
    scar(*p,.13)
# Side and rear skull, old gashes and raking exposed plates.
for offset in (0,20,30):
    oval(offset+5,16.3,4.4,5.6,"#302933")
    oval(offset+4.8,18,2.7,3.8,"#16111B")
    for j in range(5):
        bone(offset+1.2,12.8+j*2.0,offset+7.1,12.1+j*2.0,.12)
        scar(offset+2+j*.2,14+j*1.5,offset+5.8,16+j*1.1,.10)
# Loose lower jaw, its exact forward UV face is [114,123] x [2,9].
box(114,2,123,9,"#100A12")
oval(118.5,6.0,4,2.7,"#030206")
for j in range(11):
    x=114.6+j*.72
    bone(x,2.4,x+.05,5.15+(j%3)*.36,.12)
    bone(x,8.7,x+.08,7.1+(j%3)*.12,.09)
scar(114.4,7.5,122.8,7.6,.20)
# Throat front, wet red cartilage.
box(144,2,147,10,"#05040B")
oval(145.5,5.6,1.1,3.1,"#421A25")
scar(144.4,3.4,144.9,9.2,.12)
scar(146.6,3.1,146.1,9.4,.10)
# Rib cage: torso front face is [5,12]x[55,71], not [12,19].
box(5,55,12,71,"#211A25")
oval(8.4,63,2.9,6.1,"#06040B")
for j in range(8):
    yy=56.9+j*1.68
    bone(5.4,yy+.15,7.5,yy-.44,.17)
    bone(9.3,yy-.38,11.5,yy+.51,.20)
    if j%2:scar(7.2,yy+.3,8.5,yy+.5,.12)
bone(8.3,56,8.5,70,.25)
scar(9.0,57,9.7,69.7,.16)
# Decomposing back and protruding vertebrae.
box(17,55,24,71,"#3B303A")
for j in range(10):
    yy=56+j*1.4
    bone(19.4,yy,21.2,yy+.36,.19)
    scar(20.1,yy+.15,20.0,yy+.8,.10)
# Every limb UV receives tendon definition, not only its front.
limbs=[(58,0,4,20,4),(82,0,4,18,4),(48,52,3,14,3),
       (70,52,3,14,3),(70,73,2,11,3),(82,73,2,10,3),
       (96,72,2,20,2),(114,54,3,12,1),(146,51,2,9,2),
       (157,51,2,10,2),(215,0,3,7,3),(229,0,3,6,3)]
for u,v,w,h,d in limbs:
    for k,x in enumerate((u,u+d,u+d+w,u+2*d+w)):
        fw=d if k%2==0 else w
        y0=v+d+.7;y1=min(127,v+d+h-.7)
        if y1<=y0 or x+fw>256:continue
        mid=x+fw/2
        bone(mid-.12,y0,mid+.17,y1,.15 if fw<=2 else .26)
        scar(x+.15,y0+1,x+fw-.15,y1-1,.10)
        if h>11:
            for n in range(3):
                yy=y0+(n+1)*(y1-y0)/4
                scar(x+.18,yy,x+fw-.2,yy+.5,.10)
# Fine horn and articulated finger treatment.
for row in (0,20):
    for k in range(4):
        x=182+k*6
        box(x+1,row+1,x+3,row+12,"#302731")
        bone(x+1.5,row+1.7,x+1.5,row+10.4,.18)
        scar(x+2.2,row+4,x+2.2,row+8,.12)
        oval(x+1.6,row+11,.35,.55,"#CFC2B3")
for u in (158,173):
    for k in range(3):
        x=u+1+k*2.1
        bone(x,2,x+.85,12,.25)
        scar(x+.35,4,x+.62,11,.11)
for p in [(2,51,5,61),(29,53,32,62),(59,9,61,21),
          (84,7,88,19),(122,57,124,66),(34,9,40,21)]:
    scar(*p,.16)
# PNG RGBA, filter type 0, lossless.
raw=bytearray()
stride=W*4
for y in range(H):
    raw.append(0)
    raw.extend(img[y*stride:(y+1)*stride])
def chunk(kind,d):
    return struct.pack(">I",len(d))+kind+d+struct.pack(">I",zlib.crc32(kind+d)&0xffffffff)
png=b"\x89PNG\r\n\x1a\n"
png+=chunk(b"IHDR",struct.pack(">IIBBBBB",W,H,8,6,0,0,0))
png+=chunk(b"IDAT",zlib.compress(bytes(raw),9))
png+=chunk(b"IEND",b"")
OUT.parent.mkdir(parents=True,exist_ok=True)
OUT.write_bytes(png)
print("Sleepless independent anatomy atlas:",W,H,len(png),"bytes")
