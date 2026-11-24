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
# Original generated shouted one-shot; not sampled from a meme.
snd=root/'sounds/huss.ogg'
snd.parent.mkdir(parents=True,exist_ok=True)
wav=snd.with_suffix('.wav')
subprocess.run(['espeak-ng','-v','en-us+m3','-s','145','-p','39','-a','170','-w',str(wav),'HUSS!'],check=True)
subprocess.run(['ffmpeg','-hide_banner','-loglevel','error','-y','-i',str(wav),
'-af','silenceremove=start_periods=1:start_threshold=-40dB,highpass=f=80,bass=g=5:f=170,acompressor=threshold=0.10:ratio=5:attack=3:release=70:makeup=2,alimiter=limit=0.88',
'-ac','1','-ar','22050','-c:a','libvorbis','-q:a','5',str(snd)],check=True)
wav.unlink()
