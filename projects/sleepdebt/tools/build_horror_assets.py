#!/usr/bin/env python3
"""Build the original Sleepless skin and remix pinned CC0 audio into Minecraft OGG.
Requires Python 3, ffmpeg and first-build access to raw.githubusercontent.com.
Downloaded MP3s are used only as build inputs; the JAR contains Vorbis OGGs.
"""
from __future__ import annotations
import hashlib
import math
import shutil
import struct
import subprocess
import tempfile
import urllib.request
import wave
import zlib
from pathlib import Path

ROOT=Path(__file__).resolve().parents[1]
ASSETS=ROOT/"src/main/resources/assets/sleepdebt"
ATLAS=ASSETS/"textures/entity/sleepless.png"
SOUNDS=ASSETS/"sounds/sleepless"
PIN="ba116cae4ef9f479c3a8e0bd50ebf6ada36cedf8"
PREFIX="https://raw.githubusercontent.com/mayerwin/halloween-soundboard/"+PIN+"/src/sounds/sfx/"
# Each source's original Git blob sha is checked before processing.
CC0={
 "breath":("monsters/creature-breathing.mp3",
           "0173637f945dffa2ba167d5d55b913bbb5d8998e"),
 "whisper":("ghosts/creepy-whispers.mp3",
            "d3343cc7b19322978b86127b889c4184116c050f"),
 "growl":("monsters/monster-growl.mp3",
          "3dbe8bc3d34ca576daf19aa727c66c1f65d3bf34"),
 "strike":("heartbeat-breath/choking.mp3",
           "b9de7cbbde6427dee2250a2c1a785e9dccb6fc49")
}
W,H=256,128
image=bytearray(W*H*4)

def pigment(x,y):
    # Deliberate layered skin strata, not uncorrelated random pixel confetti.
    fiber=math.sin(x*.17+y*.31)*4+math.sin(x*.43-y*.16)*3
    folds=math.sin((x+y*.2)*.075)*9
    scar=abs(math.sin(x*.07+y*.19))<.07
    value=int(max(10,min(90,39+fiber+folds)))
    if (x//8+y//13)%6==0: value-=4
    if scar: value+=12
    return (max(8,value), max(8,value-2), min(105,value+5),255)

for y in range(H):
    for x in range(W):
        idx=(y*W+x)*4
        image[idx:idx+4]=bytes(pigment(x,y))

def rect(x0,y0,x1,y1,rgb):
    for y in range(max(0,y0),min(H,y1)):
        for x in range(max(0,x0),min(W,x1)):
            i=(y*W+x)*4
            image[i:i+4]=bytes((*rgb,255))

def streak(x0,y0,x1,y1,shade,thick=1):
    dx=x1-x0;dy=y1-y0
    n=max(abs(dx),abs(dy),1)
    for step in range(n+1):
        x=int(round(x0+dx*step/n));y=int(round(y0+dy*step/n))
        rect(x,y,x+thick,y+thick,shade)

# Skin is dead-grey, accented by old wounds rather than a conventional robe.
# Skull/front face coordinates derive from skull's real cube UV, not a guessed sprite.
# Skull: texOffs(0,0), width=10, depth=10 -> frontal UV x=[20,30), y=[10,25).
rect(20,10,30,25,(38,35,41))
# Shallow cheekbone streaks around a void where the center of the face should be.
streak(20,14,24,12,(118,110,107))
streak(25,12,29,14,(107,99,99))
rect(22,15,25,19,(5,5,10))
rect(26,14,29,18,(4,4,8))
rect(23,17,24,19,(179,164,149))
rect(27,16,28,18,(146,134,135))
rect(23,20,28,25,(2,2,5))
streak(22,20,23,24,(109,85,88))
streak(29,19,28,24,(126,104,95))
for x in (23,25,27):
    rect(x,20,x+1,23,(165,150,130))
# Hairline and cracks across skull sides.
for (x0,y0,x1,y1) in ((2,11,7,18),(12,13,15,21),(32,11,39,15),(3,23,8,19)):
    streak(x0,y0,x1,y1,(117,107,103))
# Split mandible, UV front side near texOffs(112,0): depth 2 + width 9
rect(123,3,132,10,(7,7,11))
for x in range(124,131,2):
    rect(x,4,x+1,8,(166,150,132))
rect(124,8,132,10,(55,31,38))
# Open chest cavity: torso u=0,v=50 w7,d5 => frontal x=[12,19), y=[55,71)
rect(12,56,19,71,(15,12,18))
streak(12,57,14,69,(108,89,91))
streak(18,56,17,70,(99,80,84))
for y in (57,60,63,66,69):
    streak(12,y,18,y-1,(134,122,112))
    rect(14,y+1,17,y+2,(36,20,28))
# Bone rib plates and ashen scarred limbs throughout unique UV regions.
for sx,sy in ((58,0),(82,0),(48,52),(70,52),(70,73),(82,73),
              (96,72),(142,51),(146,51),(157,51)):
    for d in range(4):
        streak(sx+1+d*3,sy+4,sx+3+d*2,sy+15,
               (72+d*11,66+d*10,71+d*9))
for sx,sy in ((182,0),(188,0),(194,0),(200,0),(182,20),(188,20),
              (194,20),(200,20),(215,0),(229,0)):
    streak(sx+1,sy+2,sx+2,sy+10,(145,141,128))
    streak(sx+2,sy+4,sx+3,sy+11,(71,52,59))
# Scattered long red veins are anatomical marks, not pixel noise.
for endpoints in ((4,50,8,64),(24,52,32,62),(59,19,63,30),
                  (86,16,90,29),(116,58,124,65),(134,7,137,14),
                  (150,9,152,17)):
    streak(*endpoints,(92,39,51))

def png_chunk(kind,payload):
    return (struct.pack(">I",len(payload))+kind+payload+
        struct.pack(">I",zlib.crc32(kind+payload)&0xffffffff))
raw=bytearray()
for y in range(H):
    raw.append(0)
    raw.extend(image[y*W*4:(y+1)*W*4])
png=b"\x89PNG\r\n\x1a\n"
png+=png_chunk(b"IHDR",struct.pack(">IIBBBBB",W,H,8,6,0,0,0))
png+=png_chunk(b"IDAT",zlib.compress(bytes(raw),9))
png+=png_chunk(b"IEND",b"")
ATLAS.parent.mkdir(parents=True,exist_ok=True)
ATLAS.write_bytes(png)
print("Original Sleepless 256x128 atlas:",len(png),"bytes")

def synth_heartbeat(path):
    sample_rate=24000;duration=1.55
    with wave.open(str(path),"wb") as wav:
        wav.setnchannels(1);wav.setsampwidth(2);wav.setframerate(sample_rate)
        payload=bytearray()
        for i in range(int(duration*sample_rate)):
            t=i/sample_rate
            total=0.
            for start,weight in ((.1,1.0),(.43,.65)):
                age=t-start
                if age>=0:
                    env=math.exp(-age*22)*min(1,age*65)
                    total+=weight*env*(.72*math.sin(2*math.pi*63*age)+
                                       .25*math.sin(2*math.pi*112*age))
            # Quiet residual pulse of air, never continuous loud bass.
            total+=.006*math.sin(2*math.pi*28*t)
            value=int(max(-1,min(1,total*.72))*32767)
            payload.extend(struct.pack("<h",value))
        wav.writeframes(payload)

def encode(ffmpeg,source,dest):
    subprocess.run([ffmpeg,"-hide_banner","-nostdin","-loglevel","error",
       "-y","-i",str(source),"-vn","-ac","1","-ar","24000",
       "-c:a","libvorbis","-q:a","3",str(dest)],check=True)
    if dest.stat().st_size<1000:raise RuntimeError("Empty or invalid OGG "+str(dest))

def download(name,rel,sha):
    url=PREFIX+rel
    request=urllib.request.Request(url,headers={
      "User-Agent":"SleepDebt-CC0-Audio-Build/1.0"})
    with urllib.request.urlopen(request,timeout=35) as response:
        payload=response.read()
    identity=hashlib.sha1(b"blob "+str(len(payload)).encode()+b"\0"+payload).hexdigest()
    if identity!=sha:raise RuntimeError(name+" download hash differs from pinned CC0 source")
    return payload

ffmpeg=shutil.which("ffmpeg")
if not ffmpeg:
    raise SystemExit("ffmpeg is required to remix the CC0 sound sources into OGG.")
SOUNDS.mkdir(parents=True,exist_ok=True)
with tempfile.TemporaryDirectory() as tmp:
    temp=Path(tmp)
    for name,(rel,sha) in CC0.items():
        dest=SOUNDS/(name+".ogg")
        if dest.exists() and dest.stat().st_size>1000:
            continue
        mp3=temp/(name+".mp3")
        mp3.write_bytes(download(name,rel,sha))
        encode(ffmpeg,mp3,dest)
        print("CC0 remix:",name,dest.stat().st_size,"bytes")
    heartbeat=SOUNDS/"heartbeat.ogg"
    if not heartbeat.exists() or heartbeat.stat().st_size<1000:
        wav=temp/"heartbeat.wav"
        synth_heartbeat(wav)
        encode(ffmpeg,wav,heartbeat)
        print("Original heartbeat:",heartbeat.stat().st_size,"bytes")
print("Sleepless textures and sound bank complete.")
