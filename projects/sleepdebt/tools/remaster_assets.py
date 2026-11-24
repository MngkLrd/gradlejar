#!/usr/bin/env python3
"""3.1.4: original native 16px sprite sheets. No reuse of previous art.
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
for n,t in json.loads(Path("tools/art_314.json").read_text()).items():save(n,t["rows"],pal(t["palette"]))
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
# Native HUD. A forged bronze frame surrounds a dark oak inset.
# Broad, connected plank sections describe wood, with few fine exposed joints.
p=pal("151519 272127 382a2a 4b3026 683e2b 925736 b87848 dfa366 f0c78b 44342e 5b4538 83604a")
def frame(w,h):
    a=canvas(w,h,".")
    rect(a,1,0,w-2,h,"0");rect(a,0,1,w,h-2,"0")
    rect(a,1,1,w-2,h-2,"4");rect(a,2,2,w-4,h-4,"5")
    rect(a,5,5,w-10,h-10,"1")
    # Two distinct frame planes. Bevel discontinuities at the joints.
    rect(a,4,3,w-8,1,"7");rect(a,3,4,1,h-8,"6")
    rect(a,4,h-4,w-8,1,"3");rect(a,w-4,4,1,h-8,"3")
    for x,pat in ((12,"77666/6555"),(36,"6677666/555655"),(64,"77666/6555"),(95,"667766/55555"),(127,"77666/6555")):
        if x+7<w-7:stamp(a,x,2,pat)
    for x,pat in ((18,"44455/33344"),(46,"44554/33443"),(85,"44455/33344"),(122,"44554/33443")):
        if x+5<w-7:stamp(a,x,h-4,pat)
    # Four compact bronze clasps with a rivet recess and angled reflected light.
    corner=[".000000.","06777760","07866650","07604540","06504430","06544330","05443300",".000000."]
    for fx,fy in ((0,0),(1,0),(0,1),(1,1)):
        for yy,row in enumerate(corner):
            for xx,c in enumerate(row):
                if c!=".":put(a,w-1-xx if fx else xx,h-1-yy if fy else yy,c)
    return a
a=frame(158,68)
# Carved title plank. Long, interrupted grain follows the board's axis.
rect(a,9,7,140,15,"2")
for x,y,pat in [(9,7,"3333333333333333/333333333333"),(35,8,".333333333333/..333333333"),(95,7,"333333333333333333/3333333333333"),(123,10,"333333333333/..33333333"),(11,17,"11111111111111/11111111"),(70,18,"111111111111111111/..111111111111"),(128,18,"1111111111111/11111111")]:
    stamp(a,x,y,pat)
rect(a,9,21,140,1,"5");rect(a,10,22,138,1,"0")
for y in (25,38,51):
    rect(a,9,y-1,140,12,"3")
    rect(a,10,y,138,10,"2")
    rect(a,10,y,138,1,"9");rect(a,10,y+10,138,1,"0")
    stamp(a,11,y+2,".7./786/.54")
    # Recessed horizontal track, separated from timber by a metal lip.
    rect(a,72,y,76,11,"4");rect(a,73,y+1,74,9,"0")
    rect(a,74,y+2,72,7,"1")
    for x,pat in ((75,"222222/2222"),(86,"22222/2222"),(103,"2222222/..2222"),(121,"222222/2222"),(136,"22222/222")):
        stamp(a,x,y+7,pat)
    rect(a,74,y+9,72,1,"6")
    # Carved wood grain is visible outside the labels, not behind every letter.
    stamp(a,55,y+1,"3333333333/..333333")
    stamp(a,20,y+8,"111111111111/..111111")
save("gui/sleep_hud",a,p)
save("gui/sleep_hint",frame(158,15),p)
# Meter atlas: sculpted connected relief, not identical three recolours.
# Red - folded cloth ribbon, ochre - polished copper, violet - woven ribbon.
mp=pal("562936 8b3b49 b85461 dc7680 f3a59c 684020 a96b2d d49a47 f6c66b ffe09e 45315e 674681 8e6ab0 b698ce e0c1ec")
m=canvas(72,21,"0")
for idx,chars in enumerate(("01234","56789","abcde")):
    d,s,b,l,h=chars;y=idx*7
    rect(m,0,y,72,7,b);rect(m,0,y+6,72,1,d)
    rect(m,0,y+5,72,1,s)
    if idx==0:
        for x,pat in [(0,"333333/44433/3333"),(8,"3333333333/33333333"),(20,"334444333333/3333333333"),(35,"333333333/333333"),(47,"3334443333/33333333"),(61,"33333333333/4433333333")]:
            stamp(m,x,y,pat)
        for x in (7,26,45,62):stamp(m,x,y+4,"1111/111")
    elif idx==1:
        for x,pat in [(0,"888888888888/99998888888/8888888"),(16,"888888888/888888"),(28,"88899998888/88888888"),(44,"888888888888/8888888"),(61,"88899988888/8888888")]:
            stamp(m,x,y,pat)
        stamp(m,11,y+3,"666/6666/666")
        stamp(m,35,y+3,"66/6666/666")
        stamp(m,56,y+3,"666/6666/666")
    else:
        for x,pat in [(0,"ddddddd/eedddd/dddd"),(11,"dddddddddd/dddddddd"),(24,"ddeeeddddd/dddddddd"),(38,"dddddddd/dddddd"),(50,"dddeeedddd/dddddddd"),(63,"ddddddddd/dddddd")]:
            stamp(m,x,y,pat)
        for x in (6,23,41,58):stamp(m,x,y+3,"bb/bbb/bb")
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
# Structural upper-panel copper joints and graphite working surface.
for x,pat in ((8,"5533/4433"),(70,"5565/4433"),(157,"5565/4433")):stamp(c,x,19,pat)
rect(c,9,65,158,1,"3")
# Tiny vent slits/fasteners, distinct from slots.
for x in (12,17,22,148,153,158):stamp(c,x,27,"000/888")
save("gui/coffee_maker",c,q)
print("3.1.4: independent pixel maps, 47 item/material/crop textures and 6 GUI assets")
