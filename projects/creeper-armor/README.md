# Creeper Armor 0.2.0

Minecraft 1.21.1 / NeoForge 21.1.262 / Java 21.

## Changes
- Original 16x16 item textures for Creeper Plate, Creeper Powder, helmet, chestplate, leggings, boots and shield.
- Vanilla-compatible 64x32 worn armor texture UVs in textures/models/armor/creeper_layer_1.png and creeper_layer_2.png. Worn armor renders on standard Minecraft's cubic player model, not as a flat inventory sprite.
- Cuboid 3D model of Creeper Shield with back and edge textures, visible when held or in inventory.
- Total armor value 20 (helmet 3, chestplate 8, leggings 6, boots 3), and toughness 2.5 per piece: above diamond (2), below netherite (3). Knockback resistance 0.05 per piece.
- New Creeper Powder drops from creepers in addition to existing Creeper Plate (normal: 1 each; charged: 2 each).
- Chestplate is crafted in a 2x2 pattern: PG / II. P = Creeper Powder, G = Gunpowder, I = Iron Ingot.
- Full four-piece armor set blocks explosion damage; each surviving armor piece can be consumed to avert a critical hit and launch a player-safe explosion.
- Creeper Shield: 500 durability, right-click hold triggers block-breaking directional blast, costing 50 durability.

## Build
```sh
gradle --no-daemon clean build
```
Expected JAR: `build/libs/creeperarmor-0.2.0.jar`.

## Testing
Equip all armor in third person, verify 64x32 texture placement and visor. View the 3D shield in inventory, hand and raised to block, confirm correct positioning. Test drops, new chestplate recipe, explosion resistance, automatic armor-piece consumption, and shield durability in a dedicated server.

## Note
The item PNGs are exact pixel art, not downsized AI renders. The worn armor model is vanilla biped geometry with original UV textures; the shield is a baked 3D JSON cuboid model. In-game client visual verification is still required.
