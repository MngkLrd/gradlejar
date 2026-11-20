# MIDNIGHT: The Endless Eve 0.3.0

Minecraft 1.21.1, NeoForge 21.1.262, Java 21, Gradle 9.2.1.

Added **native worldgen features**: custom holiday landmarks and biome-dependent surface patches in all six winter biomes, placed during generation rather than on the first chunk tick. A real custom MIDNIGHT world type remains included, with six data-driven winter biomes, climate regions and vanilla terrain/caves/ores. Custom terrain noise-router is **not** part of 0.3.0.

## Play
Create a **new world**, select world type **MIDNIGHT: The Endless Eve**. Travel and inspect F3, or use `/locate biome midnighteve:crystal_crown`. Features are rare and require natural open terrain. Old Overworld automatic biome-conversion behavior remains and is **irreversible**; always back up saves before testing.

## Build
Sources are unpacked by CI from `source-b64/` followed by v0.2 and v0.3 upgrades. Run Gradle 9.2.1 with Java 21: `gradle --no-daemon clean build`. Output `build/libs/midnighteve-0.3.0.jar`.

The full source ZIP and details are supplied separately. No external runtime mod dependencies. Dedicated-server runtime and worldgen registry testing are required before calling this stable.
