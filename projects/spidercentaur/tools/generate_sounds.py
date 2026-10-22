#!/usr/bin/env python3
"""Reproducible original Ogg/Vorbis creature audio using only Python stdlib + ffmpeg.

Produces ambient and hurt Ogg files at the resource paths required by sounds.json.
This script runs ONLY during Gradle build; the distributed JAR has no runtime dependencies.
"""
import math
import random
import shutil
import struct
import subprocess
import sys
import tempfile
import wave
from pathlib import Path

SAMPLE_RATE = 16000
RNG = random.Random(210121)


def lowpass(source, cutoff):
    ratio = 1.0 - math.exp(-2.0 * math.pi * cutoff / SAMPLE_RATE)
    state = 0.0
    result = []
    for sample in source:
        state += ratio * (sample - state)
        result.append(state)
    return result


def band_noise(length, lo, hi):
    source = [RNG.uniform(-1.0, 1.0) for _ in range(length)]
    high = lowpass(source, hi)
    low = lowpass(high, lo)
    result = [a - b for a, b in zip(high, low)]
    rms = math.sqrt(sum(x * x for x in result) / length)
    return [x / max(0.0001, rms) for x in result]


def envelope(t, start, duration, attack, release):
    if t < start or t > start + duration:
        return 0.0
    return min(1.0, (t - start) / attack, (start + duration - t) / release)


def synth(name, seconds):
    length = int(seconds * SAMPLE_RATE)
    hiss = band_noise(length, 800, 4600)
    rattle = band_noise(length, 350, 3900)
    breath = band_noise(length, 110, 950)
    out = []
    for i in range(length):
        t = i / SAMPLE_RATE
        if name == "ambient":
            crack = sum(s * envelope(t, start, dur, 0.004, 0.052)
                        for start, dur, s in [
                            (0.12, 0.07, 0.13), (0.27, 0.085, 0.115),
                            (0.67, 0.07, 0.11), (0.92, 0.060, 0.08)])
            x = crack * rattle[i]
            x += 0.064 * hiss[i] * envelope(t, 0.18, 0.74, 0.22, 0.2)
            x += 0.085 * breath[i] * envelope(t, 0.11, 0.93, 0.15, 0.27)
            x += 0.042 * math.sin(2 * math.pi * (83 * t + 8 * t * t)) * envelope(t, 0.22, 0.68, 0.09, 0.22)
        else:
            x = 0.24 * rattle[i] * envelope(t, 0.01, 0.22, 0.003, 0.16)
            x += 0.125 * hiss[i] * envelope(t, 0.035, 0.40, 0.013, 0.19)
            x += 0.077 * math.sin(2 * math.pi * (223 * t - 93 * t * t)) * envelope(t, 0.023, 0.36, 0.01, 0.2)
        out.append(max(-0.72, min(0.72, math.tanh(x))))
    return out


def write_wav(path, values):
    with wave.open(str(path), "wb") as wav:
        wav.setnchannels(1)
        wav.setsampwidth(2)
        wav.setframerate(SAMPLE_RATE)
        wav.writeframes(b"".join(struct.pack("<h", round(x * 32767)) for x in values))


def main():
    if len(sys.argv) != 2:
        raise SystemExit("Usage: generate_sounds.py OUTPUT_RESOURCE_DIRECTORY")
    ffmpeg = shutil.which("ffmpeg")
    if ffmpeg is None:
        raise SystemExit("FFmpeg required to build source; packaged JAR has no requirement")
    out = Path(sys.argv[1]) / "assets/spidercentaur/sounds/mob/spider_centaur"
    out.mkdir(parents=True, exist_ok=True)
    with tempfile.TemporaryDirectory() as tmp:
        for name, seconds in [("ambient", 1.25), ("hurt", 0.56)]:
            wav_path = Path(tmp) / (name + ".wav")
            write_wav(wav_path, synth(name, seconds))
            subprocess.run([ffmpeg, "-hide_banner", "-loglevel", "error", "-y",
                            "-i", str(wav_path), "-c:a", "libvorbis", "-q:a", "2",
                            str(out / (name + ".ogg"))], check=True)
            if not (out / (name + ".ogg")).is_file():
                raise RuntimeError(f"Missing generated Ogg: {name}")
            print(f"Generated {name}.ogg ({(out / (name + '.ogg')).stat().st_size} bytes)")


if __name__ == "__main__":
    main()
