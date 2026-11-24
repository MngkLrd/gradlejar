#!/usr/bin/env python3
"""3.1.2: original native 16px sprite sheets. No reuse of previous art.
Material-first palettes, deliberate cluster maps, no stochastic noise or AA.
Blockbench Minecraft Style Guide: https://blockbench.net/wiki/guides/minecraft-style-guide/
"""
import json,struct,zlib
from pathlib import Path
ROOT=Path("src/main/resources/assets/sleepdebt/textures")
def pal(s):
    return {".":(0,0,0,0),**{format(i,"x"):tuple(bytes.fromhex(c))+(255,) if len(c)==6 else tuple(bytes.fromhex(c)) for i,c in enumerate(s.split())}}
def save(name,a,p):
    w,h=len(a[0]),len(a)
    assert all(len(r)==w for r in a),name
    def chunk(k,d):return struct.pack(">I",len(d))+k+d+struct.pack(">I",zlib.crc32(k+d)&0xffffffff)
    raw=b"".join(b"\0"+b"".join(bytes(p[c]) for c in r) for r in a)
    target=ROOT/(name+".png");target.parent.mkdir(parents=True,exist_ok=True)
    target.write_bytes(b"\x89PNG\r\n\x1a\n"+chunk(b"IHDR",struct.pack(">IIBBBBB",w,h,8,6,0,0,0))+chunk(b"IDAT",zlib.compress(raw,9))+chunk(b"IEND",b""))
for n,t in json.loads(Path("tools/art_312.json").read_text()).items():save(n,t["rows"],pal(t["palette"]))
def canvas(w,h,c="."):return [[c]*w for _ in range(h)]
def put(a,x,y,c):
    if 0<=y<len(a) and 0<=x<len(a[y]):a[y][x]=c
def rect(a,x,y,w,h,c):
    for j in range(y,y+h):
        for i in range(x,x+w):put(a,i,j,c)
def stamp(a,x,y,p):
    for j,r in enumerate(p.split("/")):
        for i,c in enumerate(r):
            if c!=".":put(a,x+i,y+j,c)
# Dark timber and warm bronze. Wood grain is a set of interrupted, coherent
# groups; recessed faces remain readable, not filled with random dither.
p=pal("171419 292328 352c2b 48332a 633c29 885335 b07545 d8a265 ebc486 3b3434 51413a 6b4c37")
def frame(w,h):
    a=canvas(w,h,".")
    rect(a,1,0,w-2,h,"0");rect(a,0,1,w,h-2,"0")
    rect(a,1,1,w-2,h-2,"4");rect(a,2,2,w-4,h-4,"5")
    rect(a,4,4,w-8,h-8,"0");rect(a,5,5,w-10,h-10,"1")
    rect(a,3,2,w-6,1,"6");rect(a,2,3,1,h-6,"6")
    rect(a,3,h-3,w-6,1,"3");rect(a,w-3,3,1,h-6,"3")
    for x,pattern in ((10,"667766/55565"),(25,"677666/55655"),(49,"666776/55565"),(81,"677766/56555"),(117,"667766/55565"),(137,"6776/5655")):
        if x+6<w-5:stamp(a,x,2,pattern)
    for x,pattern in ((12,"44554/33443"),(34,"4544/3433"),(74,"4455/3344"),(105,"5544/4433"),(135,"4544/3433")):
        if x+5<w-5:stamp(a,x,h-4,pattern)
    corner=["056550","567760","578650","675430","643430","000000"]
    for fx,fy in ((0,0),(1,0),(0,1),(1,1)):
        for yy,row in enumerate(corner):
            for xx,c in enumerate(row):put(a,w-1-xx if fx else xx,h-1-yy if fy else yy,c)
    return a
a=frame(158,68)
# Large joined shallow relief clusters, not uniform bands.
for x,y,s in [(9,8,"22222222/22233322/..2222"),(38,9,"222222222/..222222"),(95,8,"2222222/2223322"),(122,13,"222222222/2222"),(15,18,"333333333333/..333333"),(76,18,"333333333333333/..33333333"),(11,60,"222222222/222222"),(101,61,"222222222222/..222222")]:stamp(a,x,y,s)
rect(a,9,21,140,1,"5");rect(a,10,22,138,1,"3")
for y in (25,38,51):
    rect(a,9,y-1,140,12,"3");rect(a,10,y,138,10,"2")
    rect(a,9,y-1,140,1,"0");rect(a,10,y+10,138,1,"a")
    stamp(a,11,y+2,".7./765/.54")
    # coarse wooden grain along left and under meters, keeping text unobstructed.
    for x,pattern in ((20,"2223332/..22222"),(39,"222222/222332"),(61,"33322/22222"),(93,"333333/..3333"),(121,"3333333/..33333")):
        stamp(a,x,y+8,pattern)
    rect(a,73,y+1,74,9,"0");rect(a,74,y+2,72,7,"1")
    for x,pattern in ((75,"2222/222"),(88,".22/222"),(105,"22222/..222"),(123,".2222/222"),(138,"22222/222")):stamp(a,x,y+6,pattern)
    rect(a,74,y+9,72,1,"5");rect(a,146,y+2,1,7,"3")
save("gui/sleep_hud",a,p)
save("gui/sleep_hint",frame(158,15),p)
# 72x21 atlas. Three native 72x7 strips: shape light clusters from the
# left/top, distinct short material patches and local shadow planes.
# The visible width is cropped by Java; no stretching or flat fills.
mp=pal("57262f 873443 b94b58 db6770 f18e86 714224 a36a2d d69843 f4c16a ffd88b 3f315e 63487f 8965ad b490d0 d5b5e7")
m=canvas(72,21,"0")
for idx,chars in enumerate(("01234","56789","abcde")):
    d,s,b,l,h=chars;y=idx*7
    rect(m,0,y,72,7,b);rect(m,0,y+5,72,2,s)
    rect(m,0,y,72,1,l)
    for x,shape in [(1,[h+h+l+l,l+l+l]),(11,[l+l+h+h+h,l+l+l]),(24,[h+h+l,l+l+l+l]),(39,[l+l+l+h+h,l+l+l]),(56,[h+h+l+l+l,l+l+l+l]),(66,[l+h+h,l+l])]:
        stamp(m,x,y+1,"/".join(shape))
    for x,shape in [(5,[s+s+b,s+s]),(17,[s+s+s,s+s]),(31,[s+s+s+b,s+s]),(47,[s+s+b,s+s+s]),(61,[s+s+s,s+s])]:
        stamp(m,x,y+4,"/".join(shape))
    rect(m,0,y,1,7,d)
save("gui/sleep_meters",m,mp)
# Grey vanilla-style journal framing retained; no custom fonts.
jp=pal("242326 555257 777477 a3a09e c6c4c1 e1ded8 f6f3e9")
save("gui/journal_frame",[".00000000000000.","065555665555560.","054444544445450.","054333433343450.","0544........4210","0644........4210","0543........3210","0544........4210","0544........4210","0543........3210","0644........4210","0544........4210","0543334333434210","0433332333333210","0322223222222210",".00000000000000."],jp)
j=canvas(228,188,"4")
rect(j,0,0,228,188,"0");rect(j,1,1,226,186,"4");rect(j,2,2,224,1,"6");rect(j,2,2,1,184,"6");rect(j,5,5,218,178,"5")
save("gui/sleep_journal",j,jp)
# Coffee machine panel: copper casing around recessed graphite work area,
# vanilla inventory section and exact slot coordinates.
q=pal("242127 392823 573624 84502e b16d3b de9b55 f4c77e 414247 696e70 a5aaa5 c6c6c6 e7e7e7 535459")
c=canvas(176,166,"0")
rect(c,1,1,174,164,"3");rect(c,2,2,172,162,"4")
rect(c,4,4,168,158,"1");rect(c,7,19,162,47,"7")
for x,pat in [(12,"55665/44554"),(38,"556665/45554"),(92,"55665/44554"),(147,"5665/4554")]:stamp(c,x,2,pat)
rect(c,7,69,162,93,"a");rect(c,7,69,162,1,"b");rect(c,7,69,1,93,"b");rect(c,168,70,1,91,"c")
def slot(x,y):
    rect(c,x-1,y-1,18,18,"0");rect(c,x,y,16,16,"8");rect(c,x,y,16,1,"c");rect(c,x,y,1,16,"c");rect(c,x,y+15,16,1,"b");rect(c,x+15,y,1,16,"a")
for x in (28,55,82,136):slot(x,44)
for y in (84,102,120,142):
    for x in range(8,153,18):slot(x,y)
rect(c,108,47,24,8,"0");rect(c,109,48,22,6,"1")
# Tiny vent slits/fasteners, distinct from slots.
for x in (12,17,22,148,153,158):stamp(c,x,27,"000/888")
save("gui/coffee_maker",c,q)
print("3.1.2: independent pixel maps, 47 item/material/crop textures and 6 GUI assets")
