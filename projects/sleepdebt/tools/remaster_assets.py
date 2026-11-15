#!/usr/bin/env python3
"""Sleep Debt Alpha 14: hand-composed material clusters, crops, icons and UIs.

Assets are authored from connected shapes, with one deliberate light direction.
No random noise, photographic resampling, fonts or anti-aliased edges.
Native PNG pixels: 16x16 for world/item; 158x68 HUD; 228x188 journal;
176x166 coffee-machine interface. Never touch textures/entity/sleepless.png.

Runs AFTER legacy asset generators, replacing their earlier pixel art.
Only Python standard library is required. Deterministic and reproducible.
"""
from __future__ import annotations
from pathlib import Path
import math
import struct
import zlib

DEST=Path("src/main/resources/assets/sleepdebt/textures")

def rgba(value):
    if isinstance(value,tuple):
        return value if len(value)==4 else (*value,255)
    value=value.lstrip("#")
    if len(value)==8:return tuple(bytes.fromhex(value))
    return tuple(bytes.fromhex(value))+(255,)

def png_save(path,w,h,pixels):
    raw=bytearray()
    for row in pixels:
        raw.append(0)
        for p in row:raw.extend(bytes(p))
    def chunk(k,data):
        return (struct.pack(">I",len(data))+k+data+
            struct.pack(">I",zlib.crc32(k+data)&0xffffffff))
    body=b"\x89PNG\r\n\x1a\n"
    body+=chunk(b"IHDR",struct.pack(">IIBBBBB",w,h,8,6,0,0,0))
    body+=chunk(b"IDAT",zlib.compress(bytes(raw),9))
    body+=chunk(b"IEND",b"")
    path.parent.mkdir(parents=True,exist_ok=True)
    path.write_bytes(body)

class Canvas:
    def __init__(self,w=16,h=16,base=(0,0,0,0),pal=None):
        self.w=w;self.h=h;self.pal=pal or {}
        self.pix=[[rgba(base) for _ in range(w)] for _ in range(h)]
    def c(self,k):
        return rgba(self.pal.get(k,k))
    def dot(self,x,y,k):
        if 0<=x<self.w and 0<=y<self.h:self.pix[y][x]=self.c(k)
    def box(self,x0,y0,x1,y1,k):
        k=self.c(k)
        for y in range(max(0,int(y0)),min(self.h,int(y1))):
            for x in range(max(0,int(x0)),min(self.w,int(x1))):
                self.pix[y][x]=k
    def line(self,x0,y0,x1,y1,k,width=1):
        x0=int(x0);y0=int(y0);x1=int(x1);y1=int(y1)
        dx=abs(x1-x0);dy=abs(y1-y0)
        sx=1 if x0<x1 else -1;sy=1 if y0<y1 else -1
        error=dx-dy
        while True:
            self.box(x0,y0,x0+width,y0+width,k)
            if x0==x1 and y0==y1:break
            e=2*error
            if e>-dy:error-=dy;x0+=sx
            if e<dx:error+=dx;y0+=sy
    def poly(self,pts,k):
        # Pixel centers, exact polygon fill. Hard geometry, never AA.
        xs=[p[0] for p in pts];ys=[p[1] for p in pts]
        for y in range(max(0,int(min(ys))),min(self.h,int(max(ys))+2)):
            for x in range(max(0,int(min(xs))),min(self.w,int(max(xs))+2)):
                inside=False
                for i in range(len(pts)):
                    a=pts[i];b=pts[(i+1)%len(pts)]
                    if (a[1]>y+.5)!=(b[1]>y+.5):
                        crossing=(b[0]-a[0])*(y+.5-a[1])/(b[1]-a[1])+a[0]
                        if x+.5<crossing:inside=not inside
                if inside:self.dot(x,y,k)
    def save(self,relative):
        png_save(DEST/relative,self.w,self.h,self.pix)

# Material palette shared across CAMP surfaces: oxidized copper, ivory paper,
# bleached linen and charred walnut. Different blocks use separate light ramps.
COP={"0":"#281F22","d":"#533A36","s":"#79503A","m":"#A46B46",
     "l":"#C68B57","h":"#E2B779","v":"#567A70","t":"#759087"}
FAB={"0":"#332B30","d":"#5B4749","m":"#846662","l":"#AD877A",
     "h":"#C9A88D","a":"#D9C4A6","s":"#473B42"}
WOOD={"0":"#2F2426","d":"#594033","m":"#7D563B","l":"#A7774A",
      "h":"#C1935D","p":"#DDB67B","s":"#48312D"}
IRON={"0":"#30343A","d":"#586166","m":"#818988","l":"#ADB6AB",
      "h":"#D8D6C2","v":"#53665D"}
GLASS={"0":"#3B333B","d":"#6C5450","m":"#967154",
       "l":"#C69C65","h":"#F8DD96","r":"#F4F0D0"}
DREAM={"0":"#332B37","d":"#5D4352","m":"#936B78",
       "l":"#B88D89","h":"#DFC9AA","v":"#6C6C8B"}
CLAY={"0":"#3A2D2D","d":"#68443A","m":"#955D49",
      "l":"#B77B5B","h":"#D3A079","c":"#352C31","f":"#D6854D"}
LEAF={"0":"#233527","d":"#36513A","m":"#52734A",
      "l":"#80A260","h":"#B3BB78","r":"#9E424A","b":"#C47459"}

def block(name,scheme,base):
    p=Canvas(base=scheme[base],pal=scheme)
    if name in ("copper_machine","copper_top"):
        p.box(0,0,16,16,"m")
        p.poly([(0,0),(12,0),(10,4),(2,6),(0,5)],"l")
        p.poly([(11,3),(16,1),(16,16),(10,16),(13,8)],"s")
        p.poly([(2,6),(10,4),(11,9),(5,11),(1,9)],"s")
        p.box(3,8,9,11,"d");p.box(4,8,8,9,"0")
        p.box(6,12,13,14,"l");p.box(6,13,12,15,"d")
        p.box(2,2,4,4,"h");p.box(2,3,3,4,"0")
        p.box(13,12,15,14,"h");p.dot(14,13,"0")
        p.line(1,15,7,15,"d")
        if name=="copper_top":
            p.box(0,0,16,1,"0");p.box(0,15,16,16,"d")
            p.box(0,0,1,16,"0");p.box(15,0,16,16,"d")
            p.box(3,3,12,12,"s");p.box(4,4,11,10,"m")
            p.poly([(5,5),(10,4),(9,7),(6,8)],"l")
            p.line(5,12,12,12,"v")
    elif name=="coffee_screen":
        p.box(0,0,16,16,"0")
        p.box(1,1,15,15,"d");p.box(2,2,14,13,"0")
        p.box(3,4,13,11,"s");p.box(4,5,12,10,"d")
        p.box(5,5,11,6,"h");p.box(5,7,7,9,"l")
        p.box(8,7,10,9,"h");p.box(11,6,12,9,"l")
        p.box(3,12,13,13,"m");p.box(4,14,11,15,"l")
        p.box(1,1,4,2,"h")
    elif name=="iron_rim":
        p.box(0,0,16,16,"m");p.box(0,0,16,2,"0")
        p.box(0,14,16,16,"d")
        p.poly([(0,2),(10,2),(5,7),(0,9)],"l")
        p.poly([(10,8),(16,5),(16,13),(9,14)],"d")
        p.box(2,3,12,4,"h");p.box(4,11,13,12,"l")
        p.box(13,2,15,4,"0");p.dot(14,2,"h")
        p.box(1,12,3,14,"0");p.dot(2,13,"l")
        p.line(4,8,9,8,"v")
    elif name in ("stitched_fabric","bedroll_edge","bedroll_pillow","camp_canvas"):
        p.box(0,0,16,16,"m")
        p.poly([(0,0),(12,0),(10,4),(6,5),(0,6)],"l")
        p.poly([(0,11),(8,8),(12,9),(16,5),(16,16),(0,16)],"d")
        p.line(2,9,6,8,"h");p.line(10,12,14,10,"l")
        if name in ("stitched_fabric","bedroll_edge"):
            p.line(1,3,14,3,"s");p.line(0,4,13,4,"h")
            p.line(3,12,9,12,"s")
            p.box(2,3,4,5,"a");p.box(7,3,9,5,"a");p.box(12,3,14,5,"a")
        if name=="bedroll_pillow":
            p.poly([(0,3),(10,2),(15,5),(16,14),(6,15),(0,12)],"a")
            p.poly([(1,4),(10,3),(14,5),(9,8),(1,9)],"h")
            p.line(4,12,13,12,"d");p.line(12,9,14,11,"m")
        if name=="camp_canvas":
            p.box(0,1,16,3,"s");p.box(0,2,12,3,"a")
            p.poly([(8,4),(16,3),(15,8),(10,10)],"h")
            p.line(4,12,13,14,"s")
    elif name=="wooden_pole":
        p.box(0,0,16,16,"m")
        p.poly([(0,0),(9,0),(7,6),(0,8)],"l")
        p.poly([(12,0),(16,0),(16,16),(10,16),(12,7)],"d")
        p.line(2,1,2,9,"h");p.line(4,7,4,14,"d")
        p.line(7,0,7,4,"p");p.line(9,6,9,13,"h")
        p.line(13,1,13,8,"0");p.line(5,14,5,15,"0")
        p.line(1,12,3,10,"s");p.line(11,14,13,14,"l")
    elif name=="rest_lamp":
        p=Canvas(pal=scheme)
        p.box(3,1,13,15,(107,139,145,82))
        p.poly([(4,2),(10,2),(7,7),(4,9)],(180,225,221,102))
        p.poly([(8,5),(12,3),(12,14),(9,13)],(76,105,119,122))
        p.box(3,1,13,2,"h");p.box(3,13,13,14,"d")
        p.box(2,2,3,12,"m");p.box(13,2,14,12,"m")
        p.box(5,3,6,9,(247,233,197,180))
        p.box(10,9,11,12,(233,234,201,190))
    elif name=="lantern_flame":
        p=Canvas(pal=scheme)
        p.poly([(7,1),(10,5),(11,9),(9,14),(5,14),(3,9)],"l")
        p.poly([(6,5),(8,4),(9,8),(9,13),(5,13),(5,8)],"h")
        p.poly([(7,8),(8,10),(8,13),(7,13),(6,11)],"r")
        p.line(6,3,6,5,"m")
    elif name=="dream_ring":
        p.box(0,0,16,16,"0")
        p.poly([(0,0),(10,0),(10,3),(2,6),(0,5)],"l")
        p.poly([(11,0),(16,0),(16,16),(10,16),(13,9)],"m")
        p.box(2,2,14,14,"d");p.box(4,4,12,12,"h")
        p.box(6,6,10,10,"0");p.box(7,7,9,9,"v")
        p.box(2,2,5,4,"h");p.box(11,12,14,14,"l")
    elif name=="dream_woven":
        p=Canvas(pal=scheme)
        for x in (2,6,10,14):
            p.line(x,1,x-1,14,"m")
        for y in (2,6,10,14):
            p.line(1,y,14,y-1,"l")
        p.box(0,0,16,1,"d");p.box(0,15,16,16,"d")
        p.box(0,0,1,16,"d");p.box(15,0,16,16,"d")
        p.box(7,6,9,8,"h")
    elif name in ("incense_clay","incense_coals"):
        p.box(0,0,16,16,"m")
        p.poly([(0,0),(10,0),(9,4),(3,6),(0,4)],"l")
        p.poly([(10,4),(16,2),(16,16),(11,16),(8,10)],"d")
        p.poly([(1,9),(5,7),(9,10),(5,13),(0,12)],"d")
        p.line(2,4,7,4,"h");p.line(8,12,13,11,"l")
        if name=="incense_coals":
            p.box(0,0,16,16,"c")
            p.poly([(1,3),(7,1),(12,3),(10,9),(4,10)],"d")
            p.poly([(8,9),(15,5),(16,13),(12,15),(6,13)],"m")
            p.box(3,5,7,7,"f");p.box(4,5,6,6,"h")
            p.box(10,10,13,12,"f");p.box(10,10,12,11,"h")
            p.box(0,13,6,15,"d")
    else:raise ValueError(name)
    p.save("block/"+name+".png")

block_palette={
 "copper_machine":COP,"copper_top":COP,"coffee_screen":COP,
 "iron_rim":IRON,"stitched_fabric":FAB,"bedroll_edge":FAB,
 "bedroll_pillow":FAB,"camp_canvas":FAB,"wooden_pole":WOOD,
 "rest_lamp":GLASS,"lantern_flame":GLASS,"dream_ring":DREAM,
 "dream_woven":DREAM,"incense_clay":CLAY,"incense_coals":CLAY
}
for name,pal in block_palette.items():
    block(name,pal,"0" if name in ("coffee_screen","dream_ring") else "m")

# Each crop age uses the same visual grammar: tapering woody stem, grouped leaf
# planes, and clustered fruit. This produces eight distinct silhouettes.
for age in range(8):
    c=Canvas(pal=LEAF)
    height=3+age
    c.box(7,16-height,9,16,"d")
    if age>=3:c.box(8,13-age,9,16,"0")
    for tier in range(max(1,age)):
        y=13-tier*2
        if y<2:break
        spread=min(5,2+age//3)
        sign=-1 if tier%2 else 1
        left=7-spread if sign<0 else 8
        right=8 if sign<0 else 9+spread
        c.poly([(left,y),(right,y-1),(right,y+1),(left+1,y+2)],"m")
        c.poly([(left+1,y),(right-1,y-1),(right,y),(left+2,y+1)],"l")
        c.line(8,y+1,7+sign*spread,y,"0")
    if age>=5:
        clusters=[(4,10),(11,9),(5,6),(10,12),(7,5)]
        for x,y in clusters[:age-3]:
            c.box(x,y,x+2,y+2,"r")
            c.dot(x,y,"b")
    if age>=7:
        c.box(9,8,11,10,"r");c.dot(9,8,"h")
    c.save("block/coffee_bush_stage_"+str(age)+".png")

# Inventory: shared 16-colour ramp, but every item has a different silhouette,
# proportion, reflected-light plane and functional marker.
ICON={
 "X":"#292427","D":"#49332D","M":"#755040","L":"#A67450",
 "H":"#D2A26C","Y":"#F1C892","W":"#DED4B8","P":"#F5EACD",
 "R":"#A94F50","B":"#71343E","G":"#648951","I":"#375B43",
 "V":"#9F84B1","Q":"#62516E","T":"#80AAA3","U":"#3B666A"
}
def icon(name):
    p=Canvas(pal=ICON)
    def mug(accent,liquid):
        p.box(3,6,12,14,"X");p.box(4,7,11,13,"W")
        p.box(4,7,11,9,liquid);p.box(5,9,10,12,"P")
        p.box(8,9,11,13,"H");p.box(11,8,14,12,"X")
        p.box(12,9,13,11,"W");p.box(3,13,12,14,"D")
        p.box(4,8,6,12,accent)
        p.box(5,14,10,15,"M");p.box(5,3,6,5,"H")
        p.box(8,2,9,5,"H");p.box(10,3,11,5,"W")
    if name=="coffee":
        mug("L","D")
    elif name=="strong_coffee":
        mug("R","X");p.box(5,9,10,11,"D");p.box(4,12,11,13,"R")
    elif name=="long_coffee":
        mug("T","M");p.box(7,4,10,7,"W");p.box(8,5,9,7,"H")
    elif name=="caramel_coffee":
        mug("H","M");p.box(6,7,9,8,"Y");p.box(8,9,10,11,"L")
    elif name=="herbal_tea":
        mug("G","I");p.line(6,9,9,10,"G");p.box(8,7,9,8,"G")
    elif name=="dream_tea":
        mug("V","Q");p.box(6,9,9,10,"V");p.dot(8,3,"V")
    elif name=="warm_milk":
        mug("W","P");p.box(5,8,10,10,"P");p.box(7,6,9,7,"W")
    elif name=="energy_drink":
        p.box(5,2,11,15,"X");p.box(6,3,10,14,"U")
        p.box(5,2,11,4,"W");p.box(6,2,10,3,"P")
        p.box(6,13,10,14,"T")
        p.poly([(9,4),(6,9),(8,9),(7,12),(10,7),(8,7)],"Y")
        p.box(6,10,7,12,"H");p.box(9,4,10,6,"T")
    elif name=="bitter_tonic":
        p.box(6,1,10,4,"X");p.box(7,2,9,4,"H")
        p.poly([(6,4),(10,4),(12,7),(12,13),(10,15),(5,15),(4,13),(4,8)],"X")
        p.box(5,7,11,14,"I");p.box(5,7,10,9,"G")
        p.box(5,10,11,12,"W");p.box(6,10,10,11,"Y")
        p.box(6,13,10,14,"T")
    elif name=="cocoa_biscuit":
        p.poly([(4,3),(10,2),(14,6),(13,11),(9,14),(4,12),(2,8)],"X")
        p.poly([(4,4),(10,3),(13,6),(11,11),(7,13),(3,10)],"L")
        p.poly([(4,5),(9,4),(12,7),(8,10),(4,9)],"H")
        for x,y in ((5,6),(10,7),(7,10)):
            p.box(x,y,x+2,y+2,"D")
    elif name in ("roasted_coffee_beans","roasted_cocoa_beans"):
        for x,y,sz in ((3,4,5),(9,8,5),(9,2,4)):
            p.poly([(x+1,y),(x+sz-1,y+1),(x+sz,y+sz-2),(x+2,y+sz),
                (x,y+sz-1)],"X")
            p.poly([(x+1,y+1),(x+sz-1,y+2),(x+sz-1,y+sz-2),
                (x+2,y+sz-1)],"L" if name=="roasted_coffee_beans" else "M")
            p.line(x+2,y+1,x+2,y+sz-2,"D")
        p.dot(5,5,"H" if name=="roasted_coffee_beans" else "L")
    elif name=="coffee_seeds":
        for x,y in ((3,7),(7,3),(9,10)):
            p.poly([(x+1,y),(x+4,y+1),(x+3,y+4),(x,y+3)],"X")
            p.box(x+1,y+1,x+3,y+3,"H")
            p.dot(x+2,y+2,"M")
    elif name=="coffee_berries":
        p.line(7,3,8,7,"D");p.line(8,4,12,5,"G")
        p.poly([(4,4),(8,3),(7,6),(4,6)],"G")
        for x,y in ((4,7),(9,8)):
            p.poly([(x+1,y),(x+4,y+1),(x+4,y+4),(x+2,y+5),
              (x,y+3)],"B")
            p.box(x+1,y+1,x+4,y+3,"R");p.dot(x+1,y+1,"Y")
    elif name=="coffee_grounds":
        p.poly([(5,3),(11,3),(13,6),(12,13),(10,15),(4,14),(3,7)],"X")
        p.poly([(5,4),(10,4),(12,7),(11,13),(5,13),(4,8)],"H")
        p.box(5,8,11,12,"M");p.box(5,9,10,11,"D")
        p.box(5,5,11,7,"Y");p.box(7,8,9,10,"X")
        p.box(5,13,10,14,"L")
    elif name=="dried_herbs":
        p.line(4,13,10,3,"D");p.line(8,13,11,5,"M")
        for x,y in ((4,8),(7,5),(10,7),(6,10)):
            p.poly([(x,y),(x+3,y-2),(x+4,y),(x+2,y+2)],"I")
            p.poly([(x+1,y),(x+3,y-1),(x+2,y+1)],"G")
        p.box(4,12,10,14,"H");p.box(5,13,9,14,"D")
    elif name=="dream_shard":
        p.poly([(8,1),(12,6),(11,11),(8,15),(4,10),(4,5)],"X")
        p.poly([(8,2),(11,6),(8,13),(5,10),(5,6)],"Q")
        p.poly([(8,2),(11,6),(8,9),(5,6)],"V")
        p.poly([(8,2),(9,6),(7,8),(5,6)],"W")
        p.line(8,9,8,13,"H")
    elif name=="sleep_mask":
        p.box(1,6,15,9,"X")
        p.poly([(3,5),(7,6),(8,7),(11,5),(14,7),(12,12),
          (9,12),(8,10),(6,12),(3,11),(2,8)],"X")
        p.box(4,7,7,10,"Q");p.box(9,7,12,10,"Q")
        p.box(4,7,6,8,"V");p.box(9,7,11,8,"V")
        p.box(7,9,9,10,"M")
    elif name=="pillow":
        p.poly([(3,4),(11,3),(14,5),(14,12),(11,14),
           (3,13),(1,11),(2,6)],"X")
        p.poly([(3,5),(11,4),(13,6),(13,11),(10,13),(3,12),(2,10)],"W")
        p.poly([(4,5),(10,5),(12,7),(9,9),(3,8)],"P")
        p.line(4,11,10,12,"M");p.line(12,7,12,10,"H")
        p.box(3,6,4,9,"Y")
    elif name=="sleep_journal":
        p.poly([(4,1),(12,2),(13,14),(5,15),(3,13),(3,3)],"X")
        p.box(5,2,12,14,"M");p.box(6,3,11,12,"L")
        p.box(4,2,6,14,"D");p.box(5,13,12,14,"H")
        p.box(7,4,10,8,"X");p.box(8,4,10,7,"Y")
        p.box(8,5,10,6,"D");p.line(7,10,10,10,"Y")
        p.box(11,4,12,12,"H")
    elif name=="herbal_poultice":
        p.poly([(3,4),(11,3),(14,6),(13,12),(10,14),(3,13),(2,9)],"X")
        p.poly([(4,5),(11,4),(13,7),(12,11),(10,13),(4,12),(3,9)],"W")
        p.poly([(5,7),(10,6),(11,10),(7,12),(4,10)],"G")
        p.line(3,8,12,9,"P");p.line(8,5,7,12,"H")
    elif name=="incense_bundle":
        p.line(4,4,8,12,"X",2);p.line(8,2,11,13,"D",2)
        p.line(11,4,13,12,"M",2)
        p.poly([(3,8),(6,7),(12,10),(13,13),(5,13)],"L")
        p.line(5,11,12,11,"H");p.box(6,10,9,12,"R")
        p.box(7,1,9,3,"G");p.box(10,2,12,4,"G")
    else:raise ValueError(name)
    p.save("item/"+name+".png")

inventory_items=[
 "coffee","strong_coffee","long_coffee","caramel_coffee","energy_drink",
 "warm_milk","herbal_tea","dream_tea","bitter_tonic","cocoa_biscuit",
 "roasted_cocoa_beans","roasted_coffee_beans","coffee_seeds","coffee_berries",
 "coffee_grounds","dried_herbs","dream_shard","sleep_mask","pillow",
 "sleep_journal","herbal_poultice","incense_bundle"
]
for name in inventory_items:icon(name)

# UI design: dark walnut cabinet, hammered brass corners, deliberately spaced
# grain marks, inset panels. Text is still rendered by native Minecraft fonts.
UI={
 "k":"#201C22","o":"#382B2A","d":"#553B32","m":"#795036",
 "l":"#A6764B","h":"#C49A61","p":"#E0C99E","i":"#24232A",
 "v":"#39343B","t":"#8D7865","q":"#4C4040","x":"#CBA56E"
}
hud=Canvas(158,68,UI["k"],UI)
hud.box(1,1,157,67,"d")
hud.box(2,2,156,66,"m")
hud.box(3,3,155,65,"l")
hud.box(4,4,154,64,"o")
hud.box(6,6,152,62,"i")
hud.box(8,7,150,19,"k")
hud.box(9,8,149,18,"v")
hud.box(9,17,149,18,"o")
hud.box(10,20,148,21,"m")
hud.box(12,21,146,22,"h")
for y in (25,38,51):
    hud.box(9,y-2,150,y+11,"o")
    hud.box(11,y-1,149,y+9,"v")
    hud.box(11,y-1,149,y,"m")
    hud.box(12,y+8,148,y+9,"d")
    hud.box(72,y+2,148,y+10,"k")
    hud.box(73,y+3,147,y+9,"o")
    hud.box(74,y+4,146,y+8,"i")
    hud.box(13,y+2,15,y+4,"h")
    hud.box(14,y+3,15,y+5,"x")
    for px in (91,109,127):
        hud.box(px,y+3,px+1,y+8,"d")
# Brushed grain as coherent interrupted bands, not speckle.
for y,bands in ((2,[(16,36),(42,56),(68,91),(111,143)]),
                (64,[(11,31),(35,57),(79,101),(115,145)])):
    for left,right in bands:
        hud.box(left,y,right,y+1,"h")
        if right-left>14:hud.box(left+3,y+1,right-3,y+2,"m")
# Four stepped metal brackets, with two-pixel heads and countersunk centers.
for x,y in ((2,2),(149,2),(2,58),(149,58)):
    hud.box(x,y,x+7,y+8,"k")
    hud.box(x+1,y+1,x+6,y+7,"l")
    hud.box(x+2,y+2,x+5,y+5,"h")
    hud.box(x+3,y+3,x+5,y+5,"d")
# Lid emblem: a broken crescent that fits next to the title.
hud.box(137,9,141,15,"h")
hud.box(139,8,145,13,"v")
hud.box(137,14,140,16,"p")
hud.save("gui/sleep_hud.png")

# Journal: a proper layered parchment journal with quiet areas for dynamic
# text at y 37/65/93/125/140/155. No pseudo-text baked into the PNG.
journal=Canvas(228,188,UI["k"],UI)
journal.box(2,2,226,186,"d");journal.box(5,4,224,184,"h")
journal.box(9,7,218,181,(205,183,144))
journal.box(13,9,214,178,(232,216,177))
journal.box(7,8,14,179,"m");journal.box(8,9,12,176,"d")
journal.box(15,27,211,29,"l")
journal.box(16,29,210,30,"h")
journal.box(15,119,212,121,"l")
journal.box(15,162,212,163,"h")
for y in (43,71,99):
    journal.box(15,y,211,y+1,(215,199,164))
for y in (58,86,114):
    journal.box(16,y,208,y+1,(197,181,148))
for y in range(14,178,17):
    journal.box(7,y,9,y+6,"p")
    journal.box(10,y+2,13,y+4,"k")
# Leather corner reinforcements.
for x,y in ((4,3),(213,3),(4,172),(213,172)):
    journal.box(x,y,x+9,y+9,"o")
    journal.box(x+1,y+1,x+7,y+7,"l")
    journal.box(x+3,y+3,x+5,y+5,"k")
# Decorative crescent and tiny inky ornaments, safely outside title.
journal.box(195,12,203,23,"m")
journal.box(198,10,207,21,(232,216,177))
journal.box(196,22,200,24,"h")
journal.line(22,22,43,22,"l")
journal.line(185,22,190,22,"l")
journal.box(22,167,32,169,"m");journal.box(195,167,206,169,"m")
journal.save("gui/sleep_journal.png")

# Coffee-machine screen: real inventory slot coordinates are preserved. Top
# machine front is copper, inventory tray is leather/oak, vents stay out of slots.
coffee=Canvas(176,166,UI["k"],UI)
coffee.box(1,1,175,165,"d")
coffee.box(3,3,173,163,"m")
coffee.box(5,5,171,161,"o")
coffee.box(7,17,169,69,"k")
coffee.box(8,19,168,66,"v")
coffee.box(8,20,168,22,"h")
coffee.box(8,64,168,66,"d")
# Machine panel to the right, and the brass channel around its progress dial.
coffee.box(104,40,137,59,"o")
coffee.box(106,44,135,57,"k")
coffee.box(107,47,132,55,"d")
coffee.box(108,48,132,54,"i")
coffee.box(131,46,136,57,"l")
coffee.box(133,48,138,54,"h")
# Inventory backdrop and panel dividers.
coffee.box(7,71,169,162,"m")
coffee.box(8,73,168,160,"o")
coffee.box(8,82,168,139,"d")
coffee.box(8,141,168,161,"d")
coffee.box(8,71,168,72,"p")
coffee.box(8,76,168,78,"k")
for y in (82,141):
    coffee.box(8,y,168,y+1,"k")
# Native 18px slot grid; no decorative pixels inside item silhouettes.
def slot(cx,cy):
    coffee.box(cx-1,cy-1,cx+18,cy+18,"k")
    coffee.box(cx,cy,cx+18,cy+18,"p")
    coffee.box(cx+1,cy+1,cx+17,cy+17,"t")
    coffee.box(cx+2,cy+2,cx+17,cy+17,"v")
    coffee.box(cx,cy,cx+17,cy+1,"o")
    coffee.box(cx,cy,cx+1,cy+17,"o")
for x in (28,55,82,136):slot(x,44)
for y in (84,102,120):
    for x in range(8,170,18):slot(x,y)
for x in range(8,170,18):slot(x,142)
# Top wood grain and copper furnace guard.
coffee.box(10,26,157,28,"o")
coffee.box(12,27,51,28,"l");coffee.box(63,27,88,28,"l")
coffee.box(148,27,163,28,"l")
coffee.box(10,160,165,161,"h")
coffee.save("gui/coffee_maker.png")

print("Alpha 14 atlas complete: %d block materials + 8 crops + %d item icons + 3 GUIs"
      %(len(block_palette),len(inventory_items)))
