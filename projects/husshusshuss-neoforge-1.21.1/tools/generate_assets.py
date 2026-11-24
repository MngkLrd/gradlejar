from pathlib import Path
import math, random, struct, subprocess, sys, wave, zlib
root=Path(sys.argv[1])/'assets/husshusshuss'
tex=root/'textures/item/discipline_whistle.png'
tex.parent.mkdir(parents=True, exist_ok=True)
# Hand-authored 16x16 pixel icon. No smoothing.
rows=[
'................','................','................','.........dd.....','........dhlld...',
'.......dhlhld...','......dhlllld...','.....dhllgld....','....dhllgld.....',
'...dhllldd......','..dhllhd........','.dhllhd.........','.dllhd..........',
'..ddd...........','................','................']
pal={'.':(0,0,0,0),'d':(49,48,45,255),'h':(117,122,117,255),'l':(195,204,191,255),'g':(227,174,73,255)}
def chunk(name,data):
    return struct.pack('>I',len(data))+name+data+struct.pack('>I',zlib.crc32(name+data)&0xffffffff)
raw=b''.join(b'\x00'+bytes(c for p in row for c in pal[p]) for row in rows)
tex.write_bytes(b'\x89PNG\r\n\x1a\n'+chunk(b'IHDR',struct.pack('>IIBBBBB',16,16,8,6,0,0,0))+chunk(b'IDAT',zlib.compress(raw))+chunk(b'IEND',b''))
# Original user-supplied meme excerpt 1.500-3.000 seconds, mono Vorbis. No apt or network needed.
import base64, hashlib
snd=root/'sounds/huss.ogg'
snd.parent.mkdir(parents=True,exist_ok=True)
data=zlib.decompress(base64.b64decode(Path(__file__).with_name('huss.ogg.b64z').read_text()))
assert hashlib.sha256(data).hexdigest()=='a060227d7a37af7184af1cfaffb4cc6a6ddb9cdf3dd41bdd2f43b539b0051c02'
snd.write_bytes(data)
