# Spider Centaur 0.2.2 Alpha

Minecraft 1.21.1 / NeoForge / Java 21. Original single-entity spider-skeleton monster.

## Audio redesign 0.2.2
The original quiet/sandy hiss is replaced with distinctly sharper, threatening audio:
- Two randomized ambient variants: sudden brittle bone snaps, scraping metallic resonance, unsettling high-frequency squeal; 1.24-1.36 seconds.
- Two randomized hurt variants: immediate loud bone-breaking transient followed by a short distressed metal rasp; 0.62-0.64 seconds.
- Audio was synthesized from scratch using deterministic standard-library Python and encoded as 24 kHz mono Ogg/Vorbis using FFmpeg (no third-party samples).
- Minecraft playback volumes ambient 0.66-0.68 and hurt 0.83-0.86, versus 0.36 / 0.55 in previous version. Intentional dramatic transients, normalized peak -0.6 dBFS.
- Four variants live inside the JAR. No Python or FFmpeg needed to **play** the mod.
- Movement particles remain barely visible. All model, red eye, cowboy hat, spawn, damage and AI logic unchanged.

## Testing
1. Remove earlier `spidercentaur` JARs, install `spidercentaur-1.21.1-0.2.2.jar`, NeoForge Minecraft 1.21.1, Java 21.
2. Summon with `/summon spidercentaur:spider_centaur ~ ~ ~`.
3. With hostile creature sound slider near 70-100%, listen for ambient snap+scrape.
4. Strike the entity several times to hear randomized high-impact hit clips. Volume should be strong but should not clip on normal settings.
5. Confirm particles are still minimal, bow behavior and natural spawns unchanged.

## Build
`gradle clean build` with Java 21 + Gradle 9.2.1 + Python 3 + FFmpeg on PATH. Build pipeline automatically installs FFmpeg. Output `build/libs/spidercentaur-1.21.1-0.2.2.jar`.

## Known limitations
Audio mix and sound spatialization should be judged by an in-game listening test; static build/codec checks are not a runtime test.
