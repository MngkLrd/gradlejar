# Creeper Armor 0.3.4

Minecraft 1.21.1, NeoForge 21.1.262, Java 21.

- 128x64 biped armor UV textures with intentional pixel clusters, structured shading and detailed front/back/side panels.
- Dedicated creative tab for all 7 Creeper Armor items.
- JEI 19 optional plugin: recipes from vanilla data plus armor, shield and material information pages.
- Full Creeper Armor protects from explosion HP damage and knockback, but receives 4 points of wear per piece per explosion (affected by Unbreaking). Self-generated blasts never wear owner's armor.
- Shield model rotates to face outward in third person and first person.

Build: `gradle --no-daemon clean build`. Output: `build/libs/creeperarmor-0.3.3.jar`.

Test with JEI installed and uninstalled. Check creative tab, crafting recipe R/U, information pages, explosion armor durability, in-game held shield orientation and front/back/side armor texture UV.

## 0.3.3 armor shading pass
Refined 128x64 worn armor texture with additional tonal steps on broad green panels, arms and leggings, preserving hard pixel clusters and nearest-neighbor edges.

## v0.3.3 hotfix
- Equipped armor PNG layers are forced to 8-bit RGBA (not indexed) for client rendering compatibility, same pixel design.
- Creeper Shield now has client-only block pose predicate and separate raised shield item model.
- Idle and blocking third-person/first-person transforms follow vanilla shield values.
- Release bytecode/asset verification checks RGBA type, shield property class and both model states.

## v0.3.3: exact vanilla ShieldModel alignment

- The custom shield is now rendered using Minecraft's own `ShieldModel` with its original hand grip and pivot.
- `shield.json` and `shield_blocking.json` use the exact 1.21.1 vanilla transforms.
- Client-only NeoForge BEWLR uses a new 64x64 shield entity texture: Creeper face is on the outward (-Z) face; plain green back faces the player.
- Existing armor graphics, items, recipes, durability and explosion behavior remain unchanged.

Test: third person offhand, mainhand, holding right-click (both hands), GUI preview, dedicated server.

### 0.3.3 acceptance checklist
- [ ] Offhand: Creeper face points away from player, shield rests on forearm, not in free air.
- [ ] Mainhand: identical native shield attachment with inward-facing grip.
- [ ] Raised shield: vanilla 1.21.1 blocking transforms and projectile coverage.
- [ ] Other mechanics unchanged: 500 durability, 50 per blast, full armor set explosion immunity.

## 0.3.4 hotfix
- Original 11-part custom Creeper Shield shape restored, with custom model layer built around the vanilla ShieldModel handle pivot; exact existing Minecraft 1.21.1 hand and blocking poses preserved.
- The face is outward, the handle is inward, using a purpose-built green 64x64 RGBA texture atlas.
- Creeper Plate now crafted from 1 Creeper Powder + 1 Gunpowder + 2 Iron Ingots (PG / II).
- Creeper Chestplate craft restored to 8 Creeper Plates (P P / PPP / PPP).
- JEI uses normal recipe JSON; armor and blast mechanics unchanged.
