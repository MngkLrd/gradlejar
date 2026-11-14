# Halo Haven 1.0.0

Minecraft 1.21.1 / NeoForge 21.1.x / Java 21.

## Portal ritual
1. Craft the Ascension Sigil with four amethyst shards, two feathers, two ender pearls and a ghast tear.
2. Build a 3x3 platform at Y 170 or higher. Place quartz at the centre and amethyst blocks at its four corners.
3. Place a bell on top of the centre quartz block with an open view to the sky.
4. Right-click that bell holding the sigil. It takes you into Halo Haven.
5. Right-click the air with the sigil while in heaven to return to your saved Overworld location.

Heaven is a set of floating cloudstone islands with bright pastel skies and sky-meadow surfaces. New settlement islands are discovered every 512 blocks near grid centres. Each one has 4 houses, villagers, loot chests, gardens, gold roofs, trees, a lantern plaza and a fountain.

Seven 32px procedural pixel-art block textures and two 32px icons are included in the JAR. Every added block has item/block models, blockstates, loot and translations. No asset downloads are required to run.

## Build
Run: gradle --no-daemon clean build
Java: 21
Gradle: 9.2.1
Compile NeoForge: 21.1.197
Minimum declared NeoForge runtime: 21.1.80 (not fixed to latest)
Output: build/libs/halohaven-1.21.1-1.0.0.jar

Gameplay logic is dedicated-server safe. First entry may briefly stall while the starter village generates.
