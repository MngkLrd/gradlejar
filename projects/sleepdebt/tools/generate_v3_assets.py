#!/usr/bin/env python3
"""Sleep Debt 3.0 UI: fixed-scale hard-pixel atlas and original ornamentation.
No font is rasterized: text always uses Minecraft's native Font renderer.
"""
from pathlib import Path
import struct
import zlib

OUT = Path("src/main/resources/assets/sleepdebt/textures/gui")
OUT.mkdir(parents=True, exist_ok=True)

class Canvas:
    def __init__(self, width, height):
        self.w, self.h = width, height
        self.p = bytearray(width * height * 4)
    def rect(self, x0, y0, x1, y1, color):
        r, g, b, a = color
        for y in range(max(0,y0), min(self.h,y1)):
            for x in range(max(0,x0), min(self.w,x1)):
                i = 4 * (y * self.w + x)
                self.p[i:i+4] = bytes((r,g,b,a))
    def point(self, x, y, color):
        self.rect(x,y,x+1,y+1,color)
    def border(self, x0, y0, x1, y1, dark, light):
        self.rect(x0,y0,x1,y1,dark)
        self.rect(x0+1,y0+1,x1-1,y1-1,light)
    def png(self, dest):
        def chunk(typ, data):
            return (struct.pack(">I",len(data))+typ+data+
                    struct.pack(">I", zlib.crc32(typ+data)&0xffffffff))
        raw = bytearray()
        for y in range(self.h):
            raw.append(0)
            raw.extend(self.p[y*self.w*4:(y+1)*self.w*4])
        result = b"\x89PNG\r\n\x1a\n"
        result += chunk(b"IHDR",struct.pack(">IIBBBBB",self.w,self.h,8,6,0,0,0))
        result += chunk(b"IDAT",zlib.compress(bytes(raw),9))
        result += chunk(b"IEND",b"")
        # Do not replace the reviewed, version-controlled GUI assets.
        if dest.name=="sleep_hud.png" or not dest.exists(): dest.write_bytes(result)

INK=(67,48,40,255)
LEATHER=(74,51,43,255)
LEATHER_DARK=(45,31,29,255)
TAN=(197,169,122,255)
PARCHMENT=(224,207,167,255)
PAPER=(234,219,184,255)
GOLD=(181,136,76,255)

# Journal: stitched leather hard-cover with intentional asymmetry and ruling.
j = Canvas(228,188)
j.rect(0,0,228,188,LEATHER_DARK)
j.rect(2,2,226,186,LEATHER)
j.rect(6,5,222,182,TAN)
j.rect(11,8,218,178,PARCHMENT)
j.rect(14,9,217,176,PAPER)
j.rect(8,6,12,182,(110,79,57,255))
j.rect(15,6,16,182,(158,117,77,255))
j.rect(19,21,207,23,GOLD)
j.rect(19,25,207,26,(178,151,111,255))
j.rect(19,118,207,120,GOLD)
j.rect(19,160,207,161,(178,151,111,255))
for y in range(9,180,9):
    j.rect(5,y,7,y+3,(165,129,86,255))
    j.point(213,y+2,(198,171,128,255))
for x in range(22,205,8):
    if x%16==6:
        j.point(x,28,(207,184,145,255))
# Section hints: deliberately lighter than gameplay text.
for yy in (43,71,99):
    j.rect(16,yy,208,yy+1,(211,193,159,255))
for yy in (58,86,114):
    j.rect(16,yy,131,yy+1,(196,172,140,255))
# Top-right crescent emblem (no gradient).
for yy in range(10,21):
    for xx in range(193,204):
        if (xx-198)**2+(yy-15)**2<31:
            j.point(xx,yy,GOLD)
        if (xx-201)**2+(yy-12)**2<29:
            j.point(xx,yy,PAPER)
for xx,yy in ((184,13),(207,16),(209,8),(191,20)):
    j.point(xx,yy,(148,103,62,255))
j.png(OUT/"sleep_journal.png")

# Precisely pixel-authored oak HUD: deliberately spaced grain and quiet dark panel.
# Regenerated on every build to guarantee valid PNG bytes even if SCM transfer
# changes binary assets. Font and bar colors are drawn in Minecraft at runtime.
h=Canvas(158,68)
INK=(38,30,26,255)
WOOD=(91,64,49,255)
MID=(121,84,59,255)
LIGHT=(149,107,72,255)
GRAIN=(170,128,83,255)
WARM=(185,153,104,255)
PANEL=(41,39,41,255)
DARK=(21,23,25,255)
SHADOW=(66,51,41,255)
h.rect(0,0,158,68,INK)
h.rect(1,1,157,67,WOOD)
h.rect(2,2,156,66,MID)
h.rect(3,3,155,65,SHADOW)
h.rect(5,5,153,63,PANEL)
h.rect(4,4,154,5,LIGHT)
h.rect(4,4,5,64,LIGHT)
h.rect(153,4,154,64,SHADOW)
h.rect(4,63,154,64,INK)
for xx,yy in ((2,2),(150,2),(2,60),(150,60)):
    h.rect(xx,yy,xx+6,yy+6,INK)
    h.rect(xx+1,yy+1,xx+5,yy+5,MID)
    h.point(xx+2,yy+2,WARM)
    h.point(xx+3,yy+3,SHADOW)
h.rect(9,19,149,21,INK)
h.rect(11,19,147,20,LIGHT)
for xx,yy,length in ((7,12,7),(7,33,11),(8,51,8),
                     (150,11,5),(149,36,9),(150,54,5)):
    h.rect(xx,yy,xx+1,yy+length,MID)
for yy,fragments in ((2,((16,33),(41,55),(82,94),(121,141))),
                     (65,((20,29),(36,59),(76,85),(107,136)))):
    for a,b in fragments:
        h.rect(a,yy,b+1,yy+1,LIGHT)
        if b-a>10:h.rect(a+4,yy+1,b-2,yy+2,WOOD)
for yy in (28,41,54):
    h.rect(73,yy,148,yy+7,DARK)
    h.rect(74,yy+1,147,yy+2,(54,51,55,255))
    h.rect(73,yy+6,148,yy+7,(97,82,71,255))
for yy in (27,40,53):
    h.rect(10,yy+1,13,yy+4,(146,116,81,255))
    h.point(11,yy+1,(209,174,127,255))
for xx,yy in ((12,2),(145,2),(11,65),(145,65)):
    h.point(xx,yy,WARM)
h.png(OUT/"sleep_hud.png")
print("Sleep Debt V3 GUI: native 228x188 journal, 158x68 vanilla oak HUD generated")
