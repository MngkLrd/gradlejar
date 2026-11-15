#!/usr/bin/env python3
"""The exact accepted Bone Hunter (B) Foley synthesis used for preview.

Outputs 3 short, reproducible Minecraft Ogg sounds from the same seeded sequence:
ambient (dry skeletal joints), hurt (bone snaps), step_bone (quiet foot clicks).
Only build-time dependencies: numpy, scipy, soundfile and FFmpeg.
"""
from pathlib import Path
import math
import subprocess
import sys
import tempfile

import numpy as np
from scipy import signal
import soundfile as sf

SR = 44100
B = dict(seed=99, heavy=.85, clack=1.04, scruff=.61,
         step_rate=.130, hollow=.48, eerie=.25, count=6)


def filtfilt_noise(rng, n, lo=None, hi=None):
    x = rng.standard_normal(n)
    if lo is not None and hi is not None:
        sos = signal.butter(2, [lo, hi], fs=SR, btype="bandpass", output="sos")
    elif hi is not None:
        sos = signal.butter(2, hi, fs=SR, btype="lowpass", output="sos")
    else:
        sos = signal.butter(2, lo, fs=SR, btype="highpass", output="sos")
    y = signal.sosfilt(sos, x)
    r = np.sqrt(np.mean(y * y))
    return y / max(r, 1e-8)


def overlay(out, clip, start, volume=1):
    i = int(round(start * SR))
    if i >= len(out):
        return
    src, dst = max(0, -i), max(0, i)
    n = min(len(out)-dst, len(clip)-src)
    if n > 0:
        out[dst:dst+n] += clip[src:src+n] * volume


def bone_hit(rng, weight=.55, sharp=.75, roundness=.35):
    n = int(SR * .155)
    t = np.arange(n) / SR
    crack = filtfilt_noise(rng, n, lo=780, hi=6800)
    head = crack * np.exp(-t/(.009+weight*.003))
    grain = filtfilt_noise(rng, n, lo=210, hi=1700) * np.exp(-t/(.025+weight*.011))
    offsets = rng.uniform(.94, 1.07, 3)
    modes = (np.sin(2*np.pi*(410*offsets[0])*t)*np.exp(-t/.024)*.17 +
             np.sin(2*np.pi*(830*offsets[1])*t)*np.exp(-t/.014)*.10 +
             np.sin(2*np.pi*(1470*offsets[2])*t)*np.exp(-t/.008)*.055)
    hollow = np.sin(2*np.pi*(175*rng.uniform(.93, 1.07))*t) * np.exp(
        -t/(.028+.012*weight)) * roundness
    click = head*(.21+sharp*.22) + grain*(.068+.018*weight) + modes*.43 + hollow*.26
    cracklets = 0.0
    for loc in [rng.uniform(.017, .028), rng.uniform(.033, .052)]:
        d = t-loc
        cracklets += np.where(d >= 0, np.exp(-np.maximum(d, 0)/.006)*crack, 0) * (.025+sharp*.015)
    return click+cracklets


def ground_contact(rng, style, weak=False):
    n = int(SR * .16)
    t = np.arange(n)/SR
    dirt = filtfilt_noise(rng, n, lo=170, hi=2200)
    sand = filtfilt_noise(rng, n, lo=1200, hi=6400)
    thud = filtfilt_noise(rng, n, hi=390)
    env = (1-np.exp(-t/.002))*np.exp(-t/.030)
    grit = np.zeros(n)
    count = 5 if style["scruff"] < .7 else 7
    for _ in range(count):
        start = rng.uniform(.006, .075)
        width = rng.uniform(.003, .012)
        grit += np.exp(-.5*((t-start)/width)**2) * rng.uniform(.04, .11)
    weight = .57 if weak else 1.
    return weight * (dirt*env*.063*style["scruff"] +
                     sand*grit*.10*style["scruff"] +
                     thud*env*.087*style["heavy"])


def scrape(rng, duration=.15, intensity=.16):
    n = int(SR*duration)
    t = np.arange(n)/SR
    x = filtfilt_noise(rng, n, lo=380, hi=2400)
    hiss = filtfilt_noise(rng, n, lo=1800, hi=4500)
    rough = np.interp(t, np.linspace(0, duration, 32), rng.uniform(.2, 1, 32))
    env = np.sin(np.pi*np.minimum(1, t/duration))**.9
    return intensity*env*rough*(x*.19+hiss*.04)


def ambient_clip(rng, s):
    out = np.zeros(int(SR*1.38))
    at = [.12, .155, .25, .33, .48, .54]
    for j, t in enumerate(at):
        overlay(out, bone_hit(rng, weight=s["heavy"], sharp=s["clack"],
                              roundness=s["hollow"]), t,
                s["clack"]*(.66 if j%2 else .83))
    overlay(out, scrape(rng, .27, .18+s["eerie"]*.19), .44)
    overlay(out, scrape(rng, .23, .19), .78)
    return out


def steps_clip(rng, s):
    out = np.zeros(int(SR*2.04))
    offsets = [.0, .035, .071, .109]
    cursor, k = .06, 0
    while cursor < 1.84:
        group = cursor
        strength = .78 if k%2 else 1.0
        for p, offset in enumerate(offsets):
            if p >= 2 and k%2:
                continue
            t = group + offset + rng.uniform(-.006, .006)
            overlay(out, ground_contact(rng, s, weak=p > 1), t, .67*strength)
            overlay(out, bone_hit(rng, weight=s["heavy"]*.65,
                                  sharp=s["clack"]*.81, roundness=s["hollow"]),
                    t+.009, .16*s["clack"]*strength)
        if k%3 == 0:
            overlay(out, scrape(rng, .16, .13*s["scruff"]), group+.035)
        cursor += s["step_rate"]*2.65
        k += 1
    return out


def hurt_clip(rng, s):
    out = np.zeros(int(SR*.88))
    for k, t in enumerate([.037, .088, .17]):
        overlay(out, bone_hit(rng, weight=.86, sharp=s["clack"]*1.16,
                              roundness=s["hollow"]*.85),
                t, (1.22, .81, .51)[k])
    overlay(out, scrape(rng, .19, .17+.08*s["eerie"]), .11)
    overlay(out, ground_contact(rng, s), .049, .22)
    return out


def ogg(out_dir, label, audio, temp):
    clip = audio*(.83/max(np.max(np.abs(audio)), 1e-6))
    compressed = np.tanh(clip*2.8)
    compressed *= .85/max(np.max(np.abs(compressed)), 1e-6)
    wav = Path(temp)/f"{label}.wav"
    target = out_dir/f"{label}.ogg"
    sf.write(wav, compressed, SR, subtype="PCM_16")
    subprocess.run(["ffmpeg", "-hide_banner", "-loglevel", "error", "-y",
                    "-i", str(wav), "-c:a", "libvorbis", "-q:a", "4",
                    str(target)], check=True)
    if not target.is_file() or target.stat().st_size < 1000:
        raise RuntimeError(f"Invalid generated audio {target}")
    print(f"Generated {target.name}: {target.stat().st_size} bytes")


def main():
    if len(sys.argv) != 2:
        raise SystemExit("Usage: generate_sounds.py OUTPUT_RESOURCES_DIR")
    out = Path(sys.argv[1])/"assets/spidercentaur/sounds/mob/spider_centaur"
    out.mkdir(parents=True, exist_ok=True)
    rng = np.random.default_rng(B["seed"])
    # The sequence/order is the SAME as accepted 'B_bone_hunter' preview:
    ambient = ambient_clip(rng, B)
    walking = steps_clip(rng, B)
    hurt = hurt_clip(rng, B)
    with tempfile.TemporaryDirectory() as temp:
        ogg(out, "ambient", ambient, temp)
        ogg(out, "hurt", hurt, temp)
        # Extract one short four-leg group, not the 2-second preview sequence,
        # to avoid muddy overlapping sounds at Minecraft step cadence.
        ogg(out, "step_bone", walking[:int(SR*.27)], temp)


if __name__ == "__main__":
    main()
