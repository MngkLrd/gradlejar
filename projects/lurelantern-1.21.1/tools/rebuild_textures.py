#!/usr/bin/env python3
"""Generate complete 16x16 vanilla-lantern UV atlases without external packages.

Unlike a centered item icon, the vanilla template_lantern model samples
the body at u=0..5, v=2..8; base at u=0..5,v=9..14; cap at u=1..4,v=0..1.
The ring is transparent outside its actual pixels and requires cutout rendering.
"""
import json
import struct
import zlib
from pathlib import Path

PROJECT = Path(__file__).resolve().parents[1]
RES = PROJECT / "src/main/resources/assets/lurelantern"
TEX = RES / "textures/block"
TEX.mkdir(parents=True, exist_ok=True)
METAL = {
    '0':(37,42,43,255), '1':(55,62,61,255),
    '2':(81,86,77,255), '3':(117,112,92,255),
    '4':(144,123,82,255),
}
GLOW = {
    False: {'a':(107,65,34,255),'b':(149,89,38,255),
            'c':(189,120,46,255),'d':(222,149,58,255),
            'e':(242,182,73,255),'f':(255,204,102,255)},
    True:  {'a':(125,70,32,255),'b':(177,91,31,255),
            'c':(217,125,33,255),'d':(247,163,54,255),
            'e':(255,195,72,255),'f':(255,225,122,255)},
}
BODY = {
    False: ['123210','3dced2','2efde1','2fedc1','2cedc1','1bcdc1','013210'],
    True:  ['123210','3dffe2','2efff1','2ffff1','2effd1','1ceec1','013210'],
}
BASE=['011110','123321','13aa21','12aa11','122211','011110']
CAP=['2332','1221']
RING={1:'232',2:'2.1',3:'2.1',4:'121',6:'232',7:'2.1',8:'2.1',
      9:'2.1',10:'232',11:'121'}

def place(px, rows, x, y, palette):
    for iy, line in enumerate(rows):
        for ix, ch in enumerate(line):
            if ch != '.':
                px[y+iy][x+ix] = palette[ch]

def png_rgba(px):
    def chunk(tag, data):
        return struct.pack('>I',len(data))+tag+data+struct.pack('>I',zlib.crc32(tag+data)&0xffffffff)
    scan=b''.join(b'\x00'+b''.join(bytes(pixel) for pixel in row) for row in px)
    return (b'\x89PNG\r\n\x1a\n'
        +chunk(b'IHDR',struct.pack('>IIBBBBB',16,16,8,6,0,0,0))
        +chunk(b'IDAT',zlib.compress(scan,9))
        +chunk(b'IEND',b''))

for active in (False,True):
    px = [[(0,0,0,0) for _ in range(16)] for _ in range(16)]
    palette={**METAL,**GLOW[active]}
    place(px,BODY[active],0,2,palette)
    place(px,BASE,0,9,palette)
    place(px,CAP,1,0,palette)
    for y,row in RING.items():
        place(px,[row],11,y,palette)
    for x0,y0,x1,y1 in [(0,2,6,9),(0,9,6,15),(1,0,5,2)]:
        assert all(px[y][x][3]==255 for y in range(y0,y1) for x in range(x0,x1))
    assert all(pixel[3] in (0,255) for row in px for pixel in row)
    dest=TEX/('lure_lantern_active.png' if active else 'lure_lantern.png')
    dest.write_bytes(png_rgba(px))
    print('Generated',dest)

for f in (RES/'models/block').glob('*.json'):
    model=json.loads(f.read_text())
    model['render_type']='minecraft:cutout'
    model['ambientocclusion']=False
    model['textures']['particle']=model['textures']['lantern']
    f.write_text(json.dumps(model,indent=2)+'\n')

states=json.loads((RES/'blockstates/lure_lantern.json').read_text())
assert len(states['variants'])==8
for state, entry in states['variants'].items():
    name=entry['model'].split(':',1)[1].split('/',1)[1]
    m=json.loads((RES/'models/block'/f'{name}.json').read_text())
    assert m['render_type']=='minecraft:cutout',state
    assert (TEX/(m['textures']['lantern'].split('/')[-1]+'.png')).is_file(),state
print('PASS: UV coverage, 16x16 RGBA, 8 states, four cutout models.')
