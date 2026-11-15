# Creeper Armor 26.3 - NeoForge, Java 25
Port of 0.3.4. Separate gradle project. Build: `gradle --no-daemon clean build`.
Creeper Plate and Powder, 4 armor pieces, full-set explosion immunity with durability wear, automatic blast sacrifice, shield with directional blast (50 per use / 500 total), recipes, separate creative tab. Original 3D shield item model and artwork. Optional JEI info pages.
Game-side visual/behavior verification still required.

## 1.0.1 world creation fix
The original 26.3 port used pre-26.3 ingredient entries such as `"P": {"item": "creeperarmor:creeper_plate"}`. Minecraft 26.3's recipe registry rejects this object and stops world creation with `Registry Loading`. All six shaped recipes now use string ingredient IDs. The project's release verification now parses every packaged recipe and checks ingredient syntax, pattern coverage and the Plate -> Chestplate progression. Java gameplay behavior, textures, and original 3D shield were not changed.


## Loader minimum (compatibility release 1.0.2)
Build and verification target: NeoForge 26.3.0.76; metadata minimum 26.3.0.76. The declared minimum is no longer automatically tied to the newest installer. Keep compile-loader and minimum-loader separate; do not increase either without API or runtime evidence. In-game smoke test on the minimum loader still recommended.
