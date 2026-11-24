#!/usr/bin/env python3
"""Original seasonal score, synthesized offline to Ogg Vorbis during build.
No external recordings, MIDI files, music libraries, or runtime audio dependencies.
"""
from pathlib import Path
import math, wave, array, subprocess, tempfile
RATE=22050
TAU=math.tau
ROOT=Path(__file__).resolve().parent/"src/main/resources/assets/midnighteve/sounds/music"
ROOT.mkdir(parents=True,exist_ok=True)

# Two deliberately original 16-bar motifs, arranged in 3/4 and 4/4.
# MIDI notes, zero indicates a rest. The sequence is our own composition.
MEADOW=[(67,1),(71,1),(74,1),(69,2),(67,1),(64,1),(67,1),(71,1),(72,2),(69,1),
 (66,1),(69,1),(74,1),(76,1),(74,1),(71,1),(67,2),(64,1),(62,3),
 (67,1),(69,1),(72,1),(76,2),(74,1),(71,1),(67,1),(64,1),(69,2),(66,1),
 (62,1),(66,1),(69,1),(71,2),(69,1),(67,2),(64,1),(67,3)]
NIGHT=[(74,2),(78,1),(76,1),(71,3),(0,1),(69,1),(71,1),(74,2),(73,3),(0,1),
 (66,2),(69,1),(71,1),(74,2),(76,2),(78,1),(76,1),(73,2),(71,4),
 (69,2),(74,1),(76,1),(78,3),(0,1),(76,2),(73,2),(71,1),(69,1),(66,2),
 (67,1),(71,1),(74,2),(73,3),(69,1),(71,2),(66,2),(62,4)]
def synth(name,melody,beat,chords,bar):
    dur=sum(d for _,d in melody)*beat+5
    buf=array.array("f",[0.0])*int(dur*RATE)
    def note(midi,start,dur,amp,voice):
        if not midi:return
        f=440*2**((midi-69)/12)
        off=int(start*RATE)
        tail=1.4 if voice=="bell" else .8
        for i in range(min(len(buf)-off,int((dur+tail)*RATE))):
            t=i/RATE
            if voice=="bell":
                env=min(1,t/.014)*math.exp(-t/1.25)
                if t>dur:env*=math.exp(-(t-dur)/.36)
                sample=(math.sin(TAU*f*t)+.34*math.sin(TAU*f*2.01*t)+.13*math.sin(TAU*f*3.99*t))*.57
            else:
                env=min(1,t/.13)*min(1,max(0,(dur+tail-t)/tail))
                sample=(math.sin(TAU*f*t)+.21*math.sin(TAU*f*2*t)+.06*math.sin(TAU*f*3*t))*.72
            buf[off+i]+=amp*env*sample
    t=1
    for m,d in melody:
        note(m,t,d*beat,.21,"bell")
        if m and d>=2:note(m-12,t+.025,d*beat,.044,"soft")
        t+=d*beat
    for k,start in enumerate(range(0,int(sum(d for _,d in melody)),bar)):
        chord=chords[k%len(chords)]
        for i,m in enumerate(chord):
            note(m,1+start*beat+i*.18,bar*beat-.15,.066 if i==0 else .035,"soft")
        note(chord[-1]+12,1+(start+bar-1)*beat,beat*.7,.045,"bell")
    # Quiet reflections; two feedback-free delay taps and fade both ends.
    for offset,gain in [(int(.21*RATE),.14),(int(.43*RATE),.075)]:
        for i in range(len(buf)-1,offset-1,-1):buf[i]+=buf[i-offset]*gain
    peak=max(abs(x) for x in buf) or 1
    pcm=array.array("h")
    for i,v in enumerate(buf):
        fade=min(1,i/(RATE*.7),(len(buf)-1-i)/(RATE*2.4))
        pcm.append(int(max(-1,min(1,v*.68/peak*fade))*32767))
    with tempfile.TemporaryDirectory() as tmp:
        src=Path(tmp)/"score.wav"
        with wave.open(str(src),"wb") as w:
            w.setnchannels(1);w.setsampwidth(2);w.setframerate(RATE);w.writeframes(pcm.tobytes())
        subprocess.run(["ffmpeg","-hide_banner","-loglevel","error","-y","-i",str(src),"-c:a","libvorbis","-q:a","4",str(ROOT/(name+".ogg"))],check=True)
    print(name,round(dur,1),"seconds")
synth("lanterns_at_dusk",MEADOW,.72,[(48,55,60),(45,52,57),(50,57,62),(43,50,59),(48,55,64),(45,52,60),(50,57,66),(43,50,62)],3)
synth("snow_over_the_fjords",NIGHT,.79,[(47,54,59),(43,50,55),(45,52,57),(42,49,57),(47,54,62),(43,50,59),(45,52,61),(42,49,54)],4)
