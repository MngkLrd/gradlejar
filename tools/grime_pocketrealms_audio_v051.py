"""Apply gritty tape-like deterioration to the user's two OGG recordings."""
from pathlib import Path
import subprocess
root=Path(__file__).resolve().parents[1]/"projects/pocketrealms/src/main/resources/assets/pocketrealms/sounds/entity"
for filename in ("void_pursuer_idle.ogg","void_pursuer_chase.ogg"):
    source=root/filename
    if not source.is_file():raise SystemExit("Missing source sound "+str(source))
    out=root/(filename+".tmp.ogg")
    subprocess.run(["ffmpeg","-hide_banner","-loglevel","error","-y","-i",str(source),
        "-af","highpass=f=65,lowpass=f=1700,acrusher=bits=9:mix=0.14,aecho=0.75:0.52:120|245:0.16|0.075,acompressor=threshold=0.15:ratio=3.0,alimiter=limit=0.92",
        "-ac","1","-ar","22050","-c:a","libvorbis","-qscale:a","2",str(out)],check=True)
    if out.stat().st_size<1000:raise SystemExit("Audio corrupted "+filename)
    out.replace(source)
    print("Grimy OGG rendered:",filename,source.stat().st_size)
