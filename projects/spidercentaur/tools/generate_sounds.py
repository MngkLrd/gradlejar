#!/usr/bin/env python3
"""Generate original, deliberately sharp/brittle Spider Centaur horror audio.

Uses Python stdlib and FFmpeg at build time. Produces deterministic Ogg Vorbis
assets for NeoForge. No runtime dependencies or sampled third-party audio.
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

RATE = 24000
TWOPI = math.tau


def clamp(x):
    return max(-1.0, min(1.0, x))


def decayed(t, at, speed):
    return math.exp(-(t - at) * speed) if t >= at else 0.0


def burst(t, at, length, attack=0.001):
    if t < at or t >= at + length:
        return 0.0
    d = t - at
    return min(1.0, d / attack, (at + length - t) / 0.035)


def transient(t, at, noise, strength=1.0, pitch=1.0):
    """A bone snap: near-instant full-spectrum attack + 3 inharmonic rings."""
    d = t - at
    if d < 0.0 or d > 0.18:
        return 0.0
    # No rounded / sandy attack: sub-millisecond impulse + hard early decay.
    hit = noise * math.exp(-d * 65) * (1.0 if d > 0.00055 else d / 0.00055)
    ring = (0.39 * math.sin(TWOPI * 1730 * pitch * d) +
            0.27 * math.sin(TWOPI * 2870 * pitch * d) +
            0.19 * math.sin(TWOPI * 4690 * pitch * d)) * math.exp(-d * 29)
    knock = 0.27 * math.sin(TWOPI * (320 - 130 * d) * d) * math.exp(-d * 37)
    return (1.15 * hit + ring + knock) * strength


def scrape(t, start, length, noise, flavor):
    """Tight high-frequency scraped-metal howl with nonharmonic movement."""
    env = burst(t, start, length, 0.016)
    if env <= 0.0:
        return 0.0
    d = t - start
    # Shifting inharmonic peaks feel strained and uncanny, not musical.
    f = 1040.0 + flavor * 140 + 620.0 * d / length + 105 * math.sin(d * 13)
    phase = TWOPI * (f * d + 95 * d * d)
    harmonic = math.sin(phase) * 0.33 + math.sin(phase * 1.59 + 0.7) * 0.27
    broken = math.sin(phase * 2.78 + 0.3) * 0.22
    stutter = 0.7 + 0.3 * max(0.0, math.sin(TWOPI * (20 + flavor * 3) * d))
    return env * stutter * (0.49 * noise + harmonic + broken)


def mix_sample(t, variant, white, bright):
    """Brittle snap front, rasping high dissonance behind, brief cold tail."""
    if variant == 'ambient':
        x = (transient(t, 0.075, bright, 0.78, 1.00) +
             transient(t, 0.168, bright, 0.66, 1.21) +
             transient(t, 0.241, bright, 0.48, 0.91) +
             transient(t, 0.683, bright, 1.05, 1.08))
        x += 0.69 * scrape(t, 0.30, 0.58, bright, 1)
        x += 0.29 * scrape(t, 0.79, 0.39, white, 2)
    elif variant == 'ambient_alt':
        x = (transient(t, 0.056, bright, 0.96, 1.19) +
             transient(t, 0.182, bright, 0.89, 0.94) +
             transient(t, 0.492, bright, 0.66, 1.07) +
             transient(t, 0.625, bright, 0.86, 1.25))
        x += 0.83 * scrape(t, 0.23, 0.70, bright, 3)
    elif variant == 'hurt':
        x = (transient(t, 0.004, bright, 1.38, 1.15) +
             transient(t, 0.028, bright, 1.11, 1.44) +
             transient(t, 0.082, bright, 0.78, 0.93))
        x += 1.00 * scrape(t, 0.022, 0.44, bright, 4)
        x += 0.35 * scrape(t, 0.17, 0.25, white, 2)
    else:  # hurt_alt
        x = (transient(t, 0.003, bright, 1.43, 0.97) +
             transient(t, 0.045, bright, 0.99, 1.42) +
             transient(t, 0.123, bright, 0.85, 1.14))
        x += 1.1 * scrape(t, 0.015, 0.49, bright, 5)
    return x


def synth(name, seconds):
    count = round(seconds * RATE)
    rng = random.Random(210122 + {'ambient': 0, 'ambient_alt': 17,
                                  'hurt': 31, 'hurt_alt': 47}[name])
    # Bright noise is high-passed rather than low-passed: no soft sand rustle.
    slow = 0.0
    samples = []
    for i in range(count):
        t = i / RATE
        white = rng.uniform(-1.0, 1.0)
        slow += 0.11 * (white - slow)
        bright = (white - slow) * 1.22
        x = mix_sample(t, name, white, bright)
        # Light waveshaping adds break-up, but transient stays intelligible.
        samples.append(math.tanh(1.34 * x))
    # Dry, short slapback reflections, not a washed-out reverb.
    for delay_ms, gain in ((49, 0.11), (103, 0.075), (161, 0.045)):
        delay = round(RATE * delay_ms / 1000)
        for i in range(delay, count):
            samples[i] += gain * samples[i - delay]
    peak = max(1e-7, max(abs(x) for x in samples))
    return [clamp(0.93 * x / peak) for x in samples]


def write_wav(path, values):
    with wave.open(str(path), 'wb') as wav:
        wav.setnchannels(1)
        wav.setsampwidth(2)
        wav.setframerate(RATE)
        wav.writeframes(b''.join(struct.pack('<h', round(v * 32767)) for v in values))


def main():
    if len(sys.argv) != 2:
        raise SystemExit('Usage: generate_sounds.py OUTPUT_RESOURCE_DIRECTORY')
    ffmpeg = shutil.which('ffmpeg')
    if ffmpeg is None:
        raise SystemExit('FFmpeg is required only when building from source')
    dest = Path(sys.argv[1]) / 'assets/spidercentaur/sounds/mob/spider_centaur'
    dest.mkdir(parents=True, exist_ok=True)
    with tempfile.TemporaryDirectory() as temp:
        for name, seconds in [('ambient', 1.36), ('ambient_alt', 1.24),
                              ('hurt', 0.62), ('hurt_alt', 0.64)]:
            wav = Path(temp) / (name + '.wav')
            samples = synth(name, seconds)
            write_wav(wav, samples)
            output = dest / (name + '.ogg')
            subprocess.run([ffmpeg, '-hide_banner', '-loglevel', 'error', '-y', '-i', str(wav),
                            '-c:a', 'libvorbis', '-q:a', '4', str(output)], check=True)
            if not output.is_file() or output.stat().st_size < 1000:
                raise RuntimeError(f'Ogg output missing or broken: {output}')
            rms = math.sqrt(sum(s * s for s in samples) / len(samples))
            print(f'Generated {output.name} ({output.stat().st_size} bytes, peak 0.93, RMS {rms:.3f})')


if __name__ == '__main__':
    main()
