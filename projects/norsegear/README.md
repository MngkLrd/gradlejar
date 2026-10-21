# Norse Gear 0.1.10 — 32x32 weapon silhouettes (batch 1)

One standalone mod. Minecraft 1.21.1, NeoForge 21.1.262, Java 21; build with Gradle 9.2.1.

### What's new

- `norsegear:bog_iron_deposit` is a naturally generated environmental block, **not** another craftable item. It generates across **all biomes tagged `c:is_overworld`** (including ordinary plains, forests, and taiga as well as swamps) but replaces **only dirt, mud and clay**. 1 in 2 chunks passes the rarity filter, with 2 placement attempts in eligible chunks and up to 4 blocks per deposit. A terrain heightmap places attempts roughly 2–4 blocks below the **local** surface instead of a fixed Y-level. Actual abundance depends on viable soil. Grass blocks are never replaced; air-exposed ores are discarded. No natural generation in Nether or End. The biome list can be overridden with a datapack using `norsegear:has_bog_iron`.
- Only stone, iron, diamond or netherite **shovels** drop bog iron; 1–2 nodules per block. Item and biome tags are datapack extensible (modded tools and wet biomes can be added).
- Bog iron **cannot be crafted from raw iron/clay/gravel** and **cannot be smelted directly**. In the crafting grid, **one bog iron + a stone-or-better pickaxe = one vanilla iron ingot**. Pickaxe is retained with 1 damage; breakage destroys it. Works with either 2×2 or 3×3 grids.
- The separate `forged_iron` item remains craftable from an iron ingot and charcoal (for existing weapons and armor); 0.1.1 items and other recipes preserved.
- Optional JEI API integration: standard crafting/smelting recipes auto-appear, plus a custom **JEI crafting view** and a JEI **ingredient information** page for bog iron deposit mining, tool reuse and location. JEI is **NOT required** for Norse Gear to start.

### Build

```
gradle --no-daemon clean build
```

Expected: `build/libs/norsegear-0.1.10.jar`. Sources include `build-profile.json` for the MngkLrd/gradlejar CI. NeoForge ModDevGradle 2.0.148 and JEI 19.51.0.418 API (compileOnly, not bundled). The worldgen is data-driven via NeoForge biome modifier; pack authors can adjust chance, count, deposit size, surface depth and overworld biome tags. **New chunks only** for natural deposits. `/setblock ~ ~ ~ norsegear:bog_iron_deposit` allows testing in existing worlds.

### JEI integrity

The JEI shapeless recipe is display-only; the actual crafting recipe is `norsegear:bog_iron_refining` backed by `BogIronRefiningRecipe`, which returns a damaged pickaxe. This prevents accidentally consuming pickaxes while supporting a familiar JEI crafting UI. JEI may not dynamically show extra modded pickaxes added via datapack tags in the showcase until JEI integration is extended.

### Status

Source JSON, asset integrity and static invariants checked locally. **v0.1.9 compiled in GitHub Actions, but crashed during world creation due to an invalid `block_match` JSON key. v0.1.10 fixes that resource; CI build and world creation testing are required to verify the fix.**

### Art iteration 0.1.9 (32×32, first five items)

- Changed `seax`, `bearded_axe`, `dane_axe`, `celtic_shield`, and `bossed_shield` from 16×16 to genuine hand-authored 32×32 transparent PNG textures. No upscaling or resampling.
- Seax: sharp broken-back spine bend with a cleaner, straight cutting edge, distinct from a standard knife.
- Bearded axe: lowered beard that hangs beneath the socket, with negative space behind the extended blade.
- Dane axe: long haft and thinner, swept crescent edge with distinct upper and lower corners.
- Shields: stronger central bosses, readable rim, broad wooden and painted segments, no microscopic ornament.
- These are flat item sprites only, not Blockbench 3D geometry or entity/armor models.
- Review `PIXEL_ART_PREVIEW.png` using nearest-neighbor scaling, and test the HUD inventory at 1x.


Batch 1 replaced placeholder textures for `seax`, `bearded_axe`, `dane_axe`, `celtic_shield` and `bossed_shield` with native 32x32 RGBA pixel art. The three weapons now use the vanilla `handheld` item model parent; the shield items keep flat `generated` inventory models. Other textures have not been redesigned yet. Gameplay behavior unchanged.

### Design pass 0.1.9
- Reworked 4 sprites at native 32x32 resolution: bearded axe with clean black outlines and a curved beard; Dane axe with a continuous wide cutting arc (no projecting bottom spike); Celtic and bossed shields with thinner rims and smaller visible metal bosses.
- The bossed shield has only vertical wood plank seams. All four sprites are drawn on the pixel grid (RGBA transparent, nearest-neighbor preview), not shrunk from AI illustrations. Seax from 0.1.6 is unchanged.
- Adjusted JAR verification to support both 16x16 placeholders and five 32x32 item textures. All gameplay code unchanged. Not compiled in Gradle.

### Design batch 2 (0.1.9)
Hand-drawn native RGBA 32x32 sprites for Viking Sword, Spear, Javelin, Throwing Axe, and Round Shield. They follow the previously approved 32x32 axe and shield palette, silhouette rules and 1-pixel outline. Gameplay, recipes, and registrations unchanged. Vanilla item JSON parent for weapons is now `minecraft:item/handheld` so they hold like weapons. The shield remains a flat generated-item visual, not a 3D shield model. Gradle build/game test has not been run.

## Equipped armor visuals in v0.1.9

Actual UV-mapped in-game vanilla humanoid equipment uses 128×64 textures `assets/norsegear/textures/models/armor/ironweave_layer_1.png` (helmet/chest/boots) and `ironweave_layer_2.png` (leggings). They are not screenshots or generated promotional 3D renders. Vanilla model geometry; no custom protruding shoulders or rigid geometry. Four 32×32 armor inventory icons replace placeholders. Verify on normal/slim players and armor stands in a running Minecraft client.

## Design batch 4 + worldgen hotfix (0.1.10)

- Added five original manually-authored native 32×32 transparent RGBA sprites: sling, stone shot, war horn, field whetstone, and travel ration. They are pixel-grid drawings, not AI renders resized from larger images.
- Preserved all existing registered items, recipes, gameplay systems, and equip armor UV textures. 19 of 24 item icons now use approved-style 32×32 art; 5 icons remain placeholders.
- Fixed the **verified runtime world-creation crash** from 0.1.9: `minecraft:block_match` must use singular `block`, **not** `blocks`, in each entry of `data/norsegear/worldgen/configured_feature/bog_iron.json`. Verified against the actual user-provided Minecraft 1.21.1 crash log and vanilla examples.
- The resource still replaces only `minecraft:dirt`, `minecraft:mud`, `minecraft:clay`, excluding grassy surface blocks. Generation remains biome-tag-based across the Overworld.
- Enhanced JAR verification to assert correctly encoded worldgen block match predicates and all 19 full-size item textures.
- This build requires live client world-creation test, because static Gradle compilation alone cannot test data-driven worldgen.
