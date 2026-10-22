# Ember Shelter 1.0.1

NeoForge 1.21.1 / Java 21. One Camp Kit includes its building supplies; no extra inventory checkout.

## Crafting
First craft Camp Bundle with 8 logs around 1 chest. Then craft Camp Kit with Camp Bundle, 2 logs, 4 white wool, 1 campfire, and 1 torch. Total cost: **10 logs, 4 wool, 1 chest, 1 campfire, 1 torch**. Recipes appear in JEI and vanilla recipe book.

## Use
Right-click a solid block from above. The 7x7 footprint must be flat and solid at the floor and allow 4 layers of replaceable blocks above it. A stone cave floor is fine and is NEVER mined. Water, lava, waterlogged blocks or hard obstacles abort the whole build before consuming the kit. After validation, grass/flowers/logs/leaves inside the airspace are cleared, then camp is built in stages. WARNING: player-placed logs in the campsite airspace can also be cleared; player-placed stone, machinery, chests and other hard blocks are never touched.

## Protection
At night and near a completed lit campfire, basic zombies, husks, drowned, skeletons, strays, spiders and creepers drop targets for protected players. Does not affect Wither, Ender Dragon, Warden, Wither Skeleton, witches or raid mobs. An enemy that the player intentionally damaged can retaliate for ten seconds. Passive land animals also defend themselves when injured by a player (basic pursuit and 2HP attack); they otherwise remain peaceful.

## Server config
`<world>/serverconfig/embershelter-server.toml` contains `allowReplacingGrass`, `allowReplacingFlowers`, `clearLogs`, `clearLeaves`, `buildDelayTicks`, `maxActiveCampsPerPlayer`, `protectCampFromWeakMobs`, `protectionRadius`, `allowProtectionOutsideOverworld`. Obsolete `consumeMaterials` from 1.0.0 is ignored.

## Build
Java 21, Gradle 9.2.1: `gradle --no-daemon clean build`. Output `build/libs/embershelter-1.21.1-1.0.1.jar`. Needs installation on both client and dedicated server.

## Limits
No in-game QA guaranteed. Active build tasks are not saved across restart. Kits are spent once a full valid build begins, even if interrupted afterwards. Manual ignition of a creeper is not blocked. Modded animal AI may require separate integrations.
