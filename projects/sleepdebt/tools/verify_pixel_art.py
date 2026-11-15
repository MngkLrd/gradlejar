#!/usr/bin/env python3
"""Pixel-art integrity gate for the 45 reviewed Sleep Debt 16x16 sprites.
Technical constraints complement, but never replace, the in-game art review.
No Pillow dependency: decodes all PNG filter modes using the Python stdlib.
"""
from collections import Counter
from pathlib import Path
import struct, sys, zlib, zipfile

BASE="assets/sleepdebt/textures/"
MATERIALS="copper_machine copper_top coffee_screen iron_rim stitched_fabric bedroll_edge bedroll_pillow camp_canvas wooden_pole rest_lamp lantern_flame dream_ring dream_woven incense_clay incense_coals".split()
ITEMS="coffee strong_coffee long_coffee caramel_coffee herbal_tea dream_tea warm_milk bitter_tonic coffee_seeds coffee_berries roasted_coffee_beans roasted_cocoa_beans energy_drink coffee_grounds dried_herbs dream_shard cocoa_biscuit sleep_mask pillow sleep_journal herbal_poultice incense_bundle".split()

def decode(raw):
    if raw[:8]!=b"\x89PNG\r\n\x1a\n": raise ValueError("Invalid PNG")
    w,h,depth,typ,*_=struct.unpack(">IIBBBBB",raw[16:29])
    if (w,h,depth,typ)!=(16,16,8,6):raise ValueError("Expected 16x16 RGBA art")
    content=bytearray();pos=8
    while pos+12<=len(raw):
        n=int.from_bytes(raw[pos:pos+4],"big")
        typ=raw[pos+4:pos+8];chunk=raw[pos+8:pos+8+n]
        if zlib.crc32(typ+chunk)&0xffffffff!=int.from_bytes(raw[pos+8+n:pos+12+n],"big"):
            raise ValueError("Invalid PNG CRC")
        if typ==b"IDAT":content.extend(chunk)
        pos+=n+12
        if typ==b"IEND":break
    blob=zlib.decompress(content);offset=0;prev=bytearray(w*4);pixels=[]
    for row in range(h):
        kind=blob[offset];offset+=1
        line=bytearray(blob[offset:offset+w*4]);offset+=w*4
        if kind>4:raise ValueError("Unsupported PNG filter")
        for i in range(w*4):
            a=line[i-4] if i>=4 else 0
            b=prev[i];c=prev[i-4] if i>=4 else 0
            if kind==1:factor=a
            elif kind==2:factor=b
            elif kind==3:factor=(a+b)//2
            elif kind==4:
                q=a+b-c
                da,db,dc=abs(q-a),abs(q-b),abs(q-c)
                factor=a if da<=db and da<=dc else b if db<=dc else c
            else:factor=0
            line[i]=(line[i]+factor)&255
        pixels.append([tuple(line[j:j+4]) for j in range(0,w*4,4)])
        prev=line
    return pixels

def check(read):
    count=0;palette_total=0
    for group,names in (("block",MATERIALS),("item",ITEMS)):
        for name in names:
            im=decode(read(f"{BASE}{group}/{name}.png"))
            colors=Counter(pixel for row in im for pixel in row)
            if not 3<=len(colors)<=16:
                raise ValueError(f"{name}: palette {len(colors)} outside 3..16")
            opaque=sum(px[3]==255 for row in im for px in row)
            if group=="item" and not 18<=opaque<=205:
                raise ValueError(f"{name}: item silhouette not legible ({opaque}/256)")
            if group=="block":
                if name in ("rest_lamp","dream_woven","lantern_flame"):
                    if sum(px[3]==0 for row in im for px in row)<20:
                        raise ValueError(f"{name}: lost required transparent gaps")
                elif opaque!=256:raise ValueError(f"{name}: unintended alpha on solid material")
            count+=1;palette_total+=len(colors)
    seen=set()
    for age in range(8):
        im=decode(read(f"{BASE}block/coffee_bush_stage_{age}.png"))
        silhouette=tuple((x,y) for y,row in enumerate(im) for x,px in enumerate(row) if px[3]>0)
        if silhouette in seen:raise ValueError(f"Duplicate growth silhouette: {age}")
        seen.add(silhouette);count+=1
    print(f"PIXEL ART QA PASS: {count} inspected textures; average palette {palette_total/37:.1f} colors")

if __name__=="__main__":
    file=Path(sys.argv[1])
    if file.is_file():
        with zipfile.ZipFile(file) as jar:check(jar.read)
    else:check(lambda path:(file/path).read_bytes())
