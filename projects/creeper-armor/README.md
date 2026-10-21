# Creeper Armor 0.3.2

Minecraft 1.21.1, NeoForge 21.1.262, Java 21.

- 128x64 biped armor UV textures with intentional pixel clusters, structured shading and detailed front/back/side panels.
- Dedicated creative tab for all 7 Creeper Armor items.
- JEI 19 optional plugin: recipes from vanilla data plus armor, shield and material information pages.
- Full Creeper Armor protects from explosion HP damage and knockback, but receives 4 points of wear per piece per explosion (affected by Unbreaking). Self-generated blasts never wear owner's armor.
- Shield model rotates to face outward in third person and first person.

Build: `gradle --no-daemon clean build`. Output: `build/libs/creeperarmor-0.3.2.jar`.

Test with JEI installed and uninstalled. Check creative tab, crafting recipe R/U, information pages, explosion armor durability, in-game held shield orientation and front/back/side armor texture UV.

## 0.3.2 armor shading pass
Refined 128x64 worn armor texture with additional tonal steps on broad green panels, arms and leggings, preserving hard pixel clusters and nearest-neighbor edges.

## v0.3.2 hotfix
- Equipped armor PNG layers are forced to 8-bit RGBA (not indexed) for client rendering compatibility, same pixel design.
- Creeper Shield now has client-only block pose predicate and separate raised shield item model.
- Idle and blocking third-person/first-person transforms follow vanilla shield values.
- Release bytecode/asset verification checks RGBA type, shield property class and both model states.
