# Spider Centaur 0.2.3

NeoForge Minecraft 1.21.1, Java 21.

## Audio
- Replaces alien/high-pitched 0.2.2 sounds with the **exact Bone Hunter (B)** ambient and hurt algorithms approved in the user preview (same seed 99, filter bands, synthesis and normalizing chain).
- No unnatural metallic whining, electronic sounds or fake reverb.
- Alternates occasionally with the **vanilla skeleton ambient and hurt** events to keep the vanilla undead identity.
- Surface-aware footsteps use `BlockState.getSoundType(level, pos, entity).getStepSound()`, preserving grass, dirt, stone, sand, snow, wood and modded blocks automatically.
- Low-volume vanilla skeleton bone-step accents plus a short cropped section of B walking audio occur occasionally; no continuous overlapping loops.
- All cosmetic assets, bow mechanics, movement-only light purple particles and spawn behavior remain unchanged.

## Testing
1. Remove 0.2.2, install `spidercentaur-1.21.1-0.2.3.jar` and NeoForge 21.1.x, Java 21.
2. Spawn: `/summon spidercentaur:spider_centaur ~ ~ ~`.
3. Walk the mob over stone, grass, dirt, sand, snow, wood and modded surfaces. Listen for the primary material sound with sparse bone clicks on top.
4. Hit the mob repeatedly to sample both B's dry hurt clip and vanilla skeleton hurt; allow time for idle ambient sounds.
5. Confirm dedicated-server loading, arrow AI and model are unchanged.

## Build
`gradle clean build` from this directory. Output `build/libs/spidercentaur-1.21.1-0.2.3.jar`.
Building from source requires Python 3.12, numpy==1.26.4, scipy==1.14.1, soundfile==0.12.1 and FFmpeg. CI installs these for this project only. No Python or sound libraries needed to run the JAR.

## Limitations
Needs an in-game listening test to tune gait frequency and loudness. GitHub CI confirms source compilation and valid resources, not subjective audio realism.
