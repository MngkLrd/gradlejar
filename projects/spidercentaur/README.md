# Spider Centaur 0.2.1 Alpha

Minecraft 1.21.1 • NeoForge • Java 21.

## 0.2.1 updates

- One faint vanilla `WITCH` purple particle approximately once every 17 ticks **only while moving**. It appears just behind the spider body; no constant smoke cloud, trail emitter, or server packet spam.
- Two original short procedurally generated sounds: quiet bone clicks / obsidian hiss (ambient), sharper brief brittle hurt.
- Audio is deterministically generated during the Gradle build from a small Python standard-library script, encoded to Ogg/Vorbis with FFmpeg. Both Ogg files are included in the final JAR; no runtime Python or FFmpeg requirement.
- The cowboy hat, red eye, obsidian geometry, bow AI, spawn behavior and existing loot remain intact.

## Install/test

1. Remove old `spidercentaur-1.21.1-0.2.0.jar` from your `mods` folder.
2. Install this `spidercentaur-1.21.1-0.2.1.jar` with NeoForge 21.1.x on Minecraft 1.21.1, Java 21.
3. Spawn using `/summon spidercentaur:spider_centaur ~ ~ ~`.
4. Watch the **moving** spider from behind in darkness: extremely sparse violet sparkles at abdomen height.
5. Enable Minecraft subtitles: listen to the quiet ambient rattle, then hit the mob once to hear the distinct hurt crack.
6. Verify the sounds and particles vanish when the mob is gone; test dedicated servers and vanilla gameplay.

## Build

Run `gradle clean build`. Output at `build/libs/spidercentaur-1.21.1-0.2.1.jar`.
Building from *source* also requires Python 3 and FFmpeg on PATH to generate the two Ogg assets. The GitHub Actions runner performs this generation automatically. The installed compiled JAR requires neither.

## Known limitations

- Particle emission respects Minecraft's particle settings; shaders and very dark environments can make the deliberately subtle dust hard to spot.


First-pass sound balance and cosmetic placement have not yet been checked inside Minecraft; CI verifies the build and resulting resources.
