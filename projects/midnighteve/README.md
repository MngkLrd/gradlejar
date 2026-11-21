# MIDNIGHT: The Endless Eve v0.4.0

Minecraft 1.21.1, NeoForge 21.1.262, Java 21. Six native custom winter biomes, biome-specific flora, rare 9x9 winter workshops, Winter Keeper and Courier (one entity with two synchronized variants), exchanges, single-spawn resident persistence. All blocks, items and NPC skins use original vanilla-style assets developed under the [Blockbench Minecraft Style Guide](https://blockbench.net/wiki/guides/minecraft-style-guide/).

Java sources and textures are rebuilt from the original source-b64 and v020/v030/v040 CI overlays in **.github/workflows/build.yml**. Local complete source project and artwork source scripts are available in the ZIP deliverable.

Default Minecraft worlds are permanently winterized by the legacy conversion system; create a NEW world and select `MIDNIGHT: The Endless Eve`. Always back up saves.

Build: `gradle --no-daemon clean build`; output: `build/libs/midnighteve-0.4.0.jar`. The worldgen building placements are Feature-generated settlements, not registrable /locate structures. Client appearance and settlement spawn behavior remain subject to in-game testing.
