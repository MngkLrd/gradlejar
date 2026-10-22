#!/usr/bin/env python3
"""Generate hand-designed 16x16 pixel sprites using stdlib only.
Every asset uses hard-edged integer pixels. No antialiasing or external dependencies."""
from pathlib import Path
import struct, zlib

OUT=Path("src/main/resources/assets/sleepdebt/textures/item")
OUT.mkdir(parents=True,exist_ok=True)

# Color names are intentionally distinct clusters: outline, shadow, base, lit side, highlight.
COFFEE={"X":"#302622","D":"#4b3128","C":"#7a4c33","L":"#aa7753","W":"#eee2c5","S":"#a9a08c","B":"#6f624d","H":"#fff7dc","T":"#d8d1ba","R":"#af654c","G":"#bc995a","V":"#ddd5b9"}
BEAN={"X":"#34261e","D":"#563524","C":"#7b492d","L":"#a56d43","H":"#ca9460","S":"#38281f"}
CAN={"X":"#252e32","D":"#41525b","C":"#788991","L":"#aab9b9","H":"#d2d7c8","T":"#327c75","U":"#70bb9b","S":"#274c49"}
MILK={"X":"#424e50","D":"#70868c","C":"#b1c9cd","L":"#e3f3ea","H":"#fff9df","S":"#cbcbb6","B":"#b3a08d"}
CLOTH={"X":"#332c33","D":"#4a3f52","C":"#6e6072","L":"#9a8791","H":"#c2aca9","S":"#786477","T":"#8d7a5f"}
BAG={"X":"#3b2922","D":"#684637","C":"#ad7751","L":"#d3a16b","H":"#ead0a1","W":"#b8a282","S":"#795e46"}
BOOK={"X":"#342a27","D":"#54382f","C":"#825541","L":"#ad8060","H":"#d7b88b","S":"#a58f68","W":"#eee0b9","B":"#4b4d55"}

def rgba(color):
    return bytes.fromhex(color[1:])+b"\xff"

def save(name, pixels, pal):
    assert len(pixels)==16, (name,len(pixels))
    raw=bytearray()
    for row in pixels:
        assert len(row)==16,(name,row,len(row))
        raw.append(0)
        for k in row:
            raw.extend(b"\0\0\0\0" if k=="." else rgba(pal[k]))
    def chunk(kind, blob):
        return struct.pack(">I",len(blob))+kind+blob+struct.pack(">I",zlib.crc32(kind+blob)&0xffffffff)
    data=b"\x89PNG\r\n\x1a\n"
    data+=chunk(b"IHDR",struct.pack(">IIBBBBB",16,16,8,6,0,0,0))
    data+=chunk(b"IDAT",zlib.compress(bytes(raw),9))+chunk(b"IEND",b"")
    (OUT/(name+".png")).write_bytes(data)

def rows(*lines):
    # Pad cleanly to 16 columns; left-to-right character maps.
    return [line.ljust(16,'.') for line in lines]

save("coffee",rows(
".....T...T......",
"......T...T.....",
".....T...T......",
"................",
"...XXXXXXXX.....",
"..XLLLLLLLXX....",
"..XDDCCCLLWX...",
"..XDDDDCCCWXX..",
"..XCCDDDDCWSX..",
"..XWWHHWWWWWSX.",
"..XWWHWWWWBWSX.",
"..XWHWVVVWBWSX.",
"...XWWWWWBWSX..",
"...XSSSSSSSX...",
"....XXXXXXXX....",
"................"
),COFFEE)

save("strong_coffee",rows(
".....T.T.T......",
"......T.T......",
"....T..T.......",
"................",
"...XXXXXXXX.....",
"..XLLLLLLLXX....",
"..XDDDDDDDWX...",
"..XDDDDDDDWXX..",
"..XDDCCCDDW S X".replace(" ",""),
"..XWWRRRWWWSX..",
"..XWWRHRWWWSX..",
"..XWHRRRWWBWSX.",
"...XWWWWWBWSX..",
"...XSSSSSSSX...",
"....XXXXXXXX....",
"................"
),COFFEE)

save("long_coffee",rows(
"....T.....T.....",
".....T...T......",
".....T....T.....",
"................",
"...XXXXXXXX.....",
"..XLLLLLLLXX....",
"..XDDCCC CLWX..".replace(" ",""),
"..XDDDDCCCWXX..",
"..XCCDDDDCWSX..",
"..XWWGGGWWWSX..",
"..XWWHGWWWBWSX.",
"..XWHGGGWWBWSX.",
"...XWWWWWBWSX..",
"...XSSSSSSSX...",
"....XXXXXXXX....",
"................"
),COFFEE)

save("energy_drink",rows(
".....XXXXXX.....",
"....XHHHHHHX....",
"....XLCCLLCX....",
"....XDTTTLCX....",
"....XDUTTLCX....",
"....XDTUTLCX....",
"....XDTUTLCX....",
"....XDTTULCX....",
"....XDTUTLCX....",
"....XDUTTLCX....",
"....XDTUTLCX....",
"....XDTTULCX....",
"....XDTTTLCX....",
"....XLCCLLCX....",
".....XXXXXX.....",
"................"
),CAN)

save("roasted_cocoa_beans",rows(
"................",
"................",
".......XXXX.....",
"......XCCLLX....",
"....XXXCDCHX....",
"...XCCXXDDCX....",
"..XCLHXXDDX.....",
"..XCDD CXX......".replace(" ",""),
"..XCDDDX.XXXX...",
"...XDDDXXCLHX...",
"....XXXDCDCHX...",
"......XCCDCHX...",
".......XDDCX....",
"........XXX.....",
"................",
"................"
),BEAN)

save("coffee_grounds",rows(
".......XX.......",
"......XHHX......",
".....XHLLHX.....",
".....XLLLLX.....",
"....XDDDDDDX....",
"...XCCCCCCCLX...",
"...XCCLLLCCLX..",
"...XCCWWWCCLX..",
"...XCCWWWCDDX..",
"...XCDDXDDDDX...",
"...XCDDDDDDDX...",
"...XDDDDDDDDX...",
"...XLCCCCCCLX...",
"....XLLLLLLX....",
".....XXXXXX.....",
"................"
),BAG)

save("warm_milk",rows(
"......XXXX......",
"......XDDX......",
"......XLLX......",
".....XXCCXX.....",
"....XCCCCCCX....",
"...XCCLLLLCCX...",
"...XLLHHHHLLX...",
"...XLHHHHHHLX...",
"...XLHHHHHHLX...",
"...XLHHHHHHLX...",
"...XLHHHHHHLX...",
"...XCLHHHHLCX...",
"...XCCLLLLCCX...",
"....XCCCCCCX....",
".....XXXXXX.....",
"................"
),MILK)

save("pillow",rows(
"................",
"................",
"...XXXXXXXXXX...",
"..XLLLLHHLLLXX..",
".XLLHHHHHHL LLX.".replace(" ",""),
".XLHHHHHHHHHLX..",
".XHHHWWWWWHHLX..",
".XHHWWWWWWWHLX..",
".XHHWWWWWWWHLX..",
".XHHHWWWWWHHLX..",
".XLHHHHHHHLLX...",
"..XLLLLLLLLXX...",
"...XXXXXXXXX....",
"................",
"................",
"................"
),{"X":"#514843","L":"#c6b8a4","H":"#e2d4be","W":"#f4ecd7"})

save("sleep_mask",rows(
"................",
"................",
"................",
".XX..........XX.",
".XDX........XDX.",
".XDDXXXXXXXXDDX.",
"..XCCCCCCCCCCX..",
"..XCLLLCCLLLCX..",
"..XCLXCLLCXLCX..",
"..XCDXXDDXXDCX..",
"..XXDDDSSDDDXX..",
"...XXXXXXXXXX...",
"................",
"................",
"................",
"................"
),CLOTH)

save("field_guide",rows(
".....XXXXXXXX...",
"....XHHHHHHHXX..",
"...XXLLLLLLLHX..",
"...XDDCCCCCLHX..",
"...XDDCWWWCLHX..",
"...XDDCWXC CLHX.".replace(" ",""),
"...XDDCWWWCLHX..",
"...XDDCCCCCLHX..",
"...XDDCHHHCLHX..",
"...XDDCCBCCLHX..",
"...XDDCCBCCLHX..",
"...XDDCCCCCLHX..",
"...XDDCCCCCLHX..",
"....XLLLLLLLHX..",
".....XXXXXXXX...",
"................"
),BOOK)
print("Generated",len(list(OUT.glob("*.png"))),"sprites")

# Sleepless skin: a custom 64x64 humanoid UV texture for vanilla ZombieModel.
# Hard-edged original pixel clusters; no copyrighted vanilla texture bytes.
ENTITY_DIR=Path("src/main/resources/assets/sleepdebt/textures/entity")
ENTITY_DIR.mkdir(parents=True,exist_ok=True)
W=H=64
skin=[[(0,0,0,0) for _ in range(W)] for __ in range(H)]
def rgb(s):
    return tuple(bytes.fromhex(s[1:]))+(255,)
def area(x1,y1,x2,y2,color):
    c=rgb(color)
    for yy in range(y1,y2):
        for xx in range(x1,x2):
            skin[yy][xx]=c
# Solid muted cloak / desaturated skin to ensure no missing-looking faces.
for xx,yy,w,h in ((0,0,32,16),(16,16,40,16),(0,16,16,16),(32,0,32,16),(0,32,64,16)):
    area(xx,yy,xx+w,yy+h,"#25242d")
# hood top, front, head cheeks, hanging shadow.
area(8,8,16,16,"#363441")
area(9,10,15,15,"#171923")
area(8,8,16,9,"#505061")
area(10,11,12,12,"#a47d86")
area(13,11,15,12,"#a47d86")
area(10,12,12,13,"#69485a")
area(13,12,15,13,"#69485a")
area(11,14,14,15,"#161921")
# Tunic front and stitched seam.
area(20,20,28,32,"#35333d")
area(21,21,22,32,"#5b5157")
area(25,20,26,32,"#514b56")
area(22,22,25,23,"#68616a")
area(23,26,24,30,"#786469")
# Arms in dark sleeves and pale bony hand shapes.
area(44,20,48,32,"#292a36")
area(44,27,48,32,"#81737b")
area(36,20,40,32,"#31303a")
area(36,28,40,32,"#82717b")
# Lower legs boots.
area(4,20,8,32,"#322d35")
area(4,27,8,32,"#1b1a22")
area(20,52,24,64,"#34303b")
area(20,59,24,64,"#1b1a22")
# Crack-like sparse highlights, never smooth.
for xx,yy in ((9,8),(11,9),(16,9),(21,24),(27,28),(45,22),(5,24)):
    skin[yy][xx]=rgb("#66606c")
raw=bytearray()
for yy in range(H):
    raw.append(0)
    for xx in range(W):
        raw.extend(bytes(skin[yy][xx]))
def echunk(kind,data):
    return struct.pack(">I",len(data))+kind+data+struct.pack(">I",zlib.crc32(kind+data)&0xffffffff)
png=b"\x89PNG\r\n\x1a\n"
png+=echunk(b"IHDR",struct.pack(">IIBBBBB",W,H,8,6,0,0,0))
png+=echunk(b"IDAT",zlib.compress(bytes(raw),9))
png+=echunk(b"IEND",b"")
(ENTITY_DIR/"sleepless.png").write_bytes(png)
print("Generated Sleepless skin",W,H)

