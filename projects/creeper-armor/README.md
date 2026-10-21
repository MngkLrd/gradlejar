# Creeper Armor 0.1.0

Minecraft 1.21.1 / NeoForge 21.1.262 / Java 21.

**Status:** first playable mechanics build, placeholder sprites only.

## Mechanics
- One Creeper Plate drops from a killed creeper; charged creepers drop two.
- Four craftable Creeper armor pieces, iron-ish defense.
- Full armor set blocks all explosion damage and vanilla explosion knockback.
- Any equipped Creeper armor piece acts as a one-use emergency charge: if a hit would leave health below 2 HP, that hit deals zero health damage, one piece is consumed in the order boots, leggings, chestplate, helmet and a block-breaking explosion is created. This blast cannot hurt its owner.
- Creeper Shield blocks like a vanilla shield. After holding right-click for 12 ticks, its forward explosion is activated once per hold, with block destruction and owner immunity, and costs 50 of 500 durability.
- Crafting recipes are provided in data/creeperarmor/recipe.
- Asset textures are placeholders, not final visual design.

## Build
`gradle --no-daemon clean build`

Expected JAR: `build/libs/creeperarmor-0.1.0.jar`.

## Manual tests
1. Spawn creepers and charged creepers, kill them and verify drops.
2. Craft armor and shield, verify their creative tab placement.
3. With all four armor pieces equipped, stand near TNT and creeper explosions and verify zero damage / zero knockback.
4. Reduce health to 3 HP, take a fatal hit and verify one armor piece disappears, player survives and surrounding blocks are damaged.
5. Repeat for each remaining armor piece; once all are gone, vanilla damage resumes.
6. Hold shield to block attacks, then verify forward blast and 50 durability consumption.
7. Check both hands, shield breaking after ten blasts and behavior on multiplayer dedicated server.

## Known limitations
This iteration is mechanics-first. The shield uses a placeholder flat item sprite. The armor texture UVs are placeholders. Blast destruction obeys world explosion gamerules/configuration.
