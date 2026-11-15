# Creeper Armor 1.20.1 - Forge, Java 17
Port of 0.3.4: four armor pieces, powder, plates, crafting recipes, creative tab, blast immunity (armor wears), critical-health rescue, custom 3D shield with 500 durability, 50 per explosive use. Optional JEI information pages.
Build: `gradle --no-daemon clean build`.
Game-side verification for 1.20.1 renderer and damage events required.


## Loader minimum (compatibility release 1.0.1)
Build and verification target: Forge 47.1.0; metadata minimum 47.1.0. The declared minimum is no longer automatically tied to the newest installer. Keep compile-loader and minimum-loader separate; do not increase either without API or runtime evidence. In-game smoke test on the minimum loader still recommended.
