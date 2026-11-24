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
# Original synthetic voiced breath; no sampled meme audio.
snd=root/'sounds/huss.ogg'
snd.parent.mkdir(parents=True,exist_ok=True)
wav=snd.with_suffix('.wav')
rate=22050
rng=random.Random(671)
out=[]
low=0.0
for n in range(int(rate*1.8)):
    t=n/rate
    p=t%0.53
    k=int(t/0.53)
    if k>=3 or p>0.38:
        out.append(0);continue
    env=min(1,p/.035)*min(1,(.38-p)/.045)
    noise=rng.uniform(-1,1);low=.78*low+.22*noise
    f=127+7*k-23*p
    buzz=math.sin(2*math.pi*f*p)+.35*math.sin(4*math.pi*f*p)+.2*math.sin(6*math.pi*f*p)
    voice=math.exp(-((p-.105)/.085)**2)
    hiss=math.exp(-((p-.26)/.10)**2)
    val=env*(.19*voice*buzz+.22*voice*low+.34*hiss*(noise-low))
    out.append(int(max(-1,min(1,val))*32767))
with wave.open(str(wav),'wb') as w:
    w.setparams((1,2,rate,0,'NONE','not compressed'))
    w.writeframes(struct.pack('<'+'h'*len(out),*out))
subprocess.run(['ffmpeg','-hide_banner','-loglevel','error','-y','-i',str(wav),'-c:a','libvorbis','-q:a','4',str(snd)],check=True)
wav.unlink()
