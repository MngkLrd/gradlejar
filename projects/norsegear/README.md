# Norse Gear 0.1.12 — 1.21.1 Recipe Hotfix

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

Expected: `build/libs/norsegear-0.1.12.jar`. Sources include `build-profile.json` for the MngkLrd/gradlejar CI. NeoForge ModDevGradle 2.0.148 and JEI 19.51.0.418 API (compileOnly, not bundled). The worldgen is data-driven via NeoForge biome modifier; pack authors can adjust chance, count, deposit size, surface depth and overworld biome tags. **New chunks only** for natural deposits. `/setblock ~ ~ ~ norsegear:bog_iron_deposit` allows testing in existing worlds.

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
- Preserved all existing registered items, recipes, gameplay systems, and equip armor UV textures. All 24 registered item icons use hand-authored 32×32 art; no 16×16 inventory placeholders remain.
- Fixed the **verified runtime world-creation crash** from 0.1.9: `minecraft:block_match` must use singular `block`, **not** `blocks`, in each entry of `data/norsegear/worldgen/configured_feature/bog_iron.json`. Verified against the actual user-provided Minecraft 1.21.1 crash log and vanilla examples.
- The resource still replaces only `minecraft:dirt`, `minecraft:mud`, `minecraft:clay`, excluding grassy surface blocks. Generation remains biome-tag-based across the Overworld.
- Enhanced JAR verification to assert correctly encoded worldgen block match predicates and all 19 full-size item textures.
- This build requires live client world-creation test, because static Gradle compilation alone cannot test data-driven worldgen.

## 0.1.11 - Final five item textures

- Complete native 32x32 (RGBA) art for `pitch_flask`, `bog_iron`, `forged_iron`, `leather_binding`, `carved_horn`. Each is constructed from colored pixel polygons on the target grid with one-pixel outlines and no image resampling.
- **All 24 item inventory icons now 32x32.** The environmental `bog_iron_deposit` block has its own separate terrain texture and is not one of the 24 items. Armor-on-player UV textures remain 128x64.
- Retains 0.1.10 critical world-creation fix (`minecraft:block_match` singular `block`), data-driven Overworld generation and gameplay systems.
- CI `verify_release.py` now rejects non-32x32 item sprites. The release is not gameplay-certified until the actual client tests described in TESTING.md.

## 0.1.12 recipe hotfix

- Fixes 17 invalid shaped recipes using string ingredients in the `key` map. Minecraft 1.21.1 requires structured ingredients such as `{ "item": "norsegear:forged_iron" }`.
- Leaves all 24 texture designs, gameplay and worldgen untouched.
- Adds CI validation of recipe ingredient formats to prevent silent recipe failures when a world loads.
- The user-reported client crash in `latest(5).log` comes from **JEI++ 1.0.7** applying an incompatible mixin to **JEI 19.64.0.455**; remove JEI++ or install a version explicitly compatible with the user's JEI. This is not a Norse Gear mixin problem.
- Additional errors for recipes from OpenMotion are outside this project.


## 0.1.15: combat and 3D armor prototype

- Cubic custom worn armor on a normal humanoid renderer (NeoForge client extensions; no optional renderer library). Head guard, shoulder volumes, shaped chest, belt, leg/knee/boot details follow normal arm/leg animations. Uses conventional 64x32 UV armor sheets. This is an initial client-facing volumetric model and must be evaluated in actual game for clipping with slim skins/other armor.
- Charging (hold use at least 10 ticks) for all three spear-like/throwable weapons: melee spear, javelin and throwing axe. Melee spear now throws its own stack, with a +1 durability cost on return; launched item can be recovered where it lands. Throws spawn server-side only.
- New in-flight renderer aligns projectiles along heading and spins throwing axes. JSON cuboid item models replace flat billboard visuals for spear/javelin/throwing axe; three shields also get thick wooden model + central metal boss. Existing 32x32 inventory PNGs preserved as model texture resources.
- War horn plays a real vanilla goat horn variant instead of an empty synthetic sound event. Carved horn outline darkened to close open edges.
- 0.1.12 recipe fixes and 0.1.10 worldgen fix retained.
- Requires Minecraft 1.21.1, NeoForge 21.1.x, Java 21.

**Test in game:** third-person armor front/rear and slim/default skins; charge/release, recover stacked javelins and throwing axe; throw worn spear and retrieve without duplicates; listen to war horn; blocking stance and item models in both hands.


## 0.1.15: combat and 3D armor prototype

- Cubic custom worn armor on a normal humanoid renderer (NeoForge client extensions; no optional renderer library). Head guard, shoulder volumes, shaped chest, belt, leg/knee/boot details follow normal arm/leg animations. Uses conventional 64x32 UV armor sheets. This is an initial client-facing volumetric model and must be evaluated in actual game for clipping with slim skins/other armor.
- Charging (hold use at least 10 ticks) for all three spear-like/throwable weapons: melee spear, javelin and throwing axe. Melee spear now throws its own stack, with a +1 durability cost on return; launched item can be recovered where it lands. Throws spawn server-side only.
- New in-flight renderer aligns projectiles along heading and spins throwing axes. JSON cuboid item models replace flat billboard visuals for spear/javelin/throwing axe; three shields also get thick wooden model + central metal boss. Existing 32x32 inventory PNGs preserved as model texture resources.
- War horn plays a real vanilla goat horn variant instead of an empty synthetic sound event. Carved horn outline darkened to close open edges.
- 0.1.12 recipe fixes and 0.1.10 worldgen fix retained.
- Requires Minecraft 1.21.1, NeoForge 21.1.x, Java 21.

**Test in game:** third-person armor front/rear and slim/default skins; charge/release, recover stacked javelins and throwing axe; throw worn spear and retrieve without duplicates; listen to war horn; blocking stance and item models in both hands.


## 0.1.15: combat and 3D armor prototype

- Cubic custom worn armor on a normal humanoid renderer (NeoForge client extensions; no optional renderer library). Head guard, shoulder volumes, shaped chest, belt, leg/knee/boot details follow normal arm/leg animations. Uses conventional 64x32 UV armor sheets. This is an initial client-facing volumetric model and must be evaluated in actual game for clipping with slim skins/other armor.
- Charging (hold use at least 10 ticks) for all three spear-like/throwable weapons: melee spear, javelin and throwing axe. Melee spear now throws its own stack, with a +1 durability cost on return; launched item can be recovered where it lands. Throws spawn server-side only.
- New in-flight renderer aligns projectiles along heading and spins throwing axes. JSON cuboid item models replace flat billboard visuals for spear/javelin/throwing axe; three shields also get thick wooden model + central metal boss. Existing 32x32 inventory PNGs preserved as model texture resources.
- War horn plays a real vanilla goat horn variant instead of an empty synthetic sound event. Carved horn outline darkened to close open edges.
- 0.1.12 recipe fixes and 0.1.10 worldgen fix retained.
- Requires Minecraft 1.21.1, NeoForge 21.1.x, Java 21.

**Test in game:** third-person armor front/rear and slim/default skins; charge/release, recover stacked javelins and throwing axe; throw worn spear and retrieve without duplicates; listen to war horn; blocking stance and item models in both hands.


## 0.1.15: combat and 3D armor prototype

- Cubic custom worn armor on a normal humanoid renderer (NeoForge client extensions; no optional renderer library). Head guard, shoulder volumes, shaped chest, belt, leg/knee/boot details follow normal arm/leg animations. Uses conventional 64x32 UV armor sheets. This is an initial client-facing volumetric model and must be evaluated in actual game for clipping with slim skins/other armor.
- Charging (hold use at least 10 ticks) for all three spear-like/throwable weapons: melee spear, javelin and throwing axe. Melee spear now throws its own stack, with a +1 durability cost on return; launched item can be recovered where it lands. Throws spawn server-side only.
- New in-flight renderer aligns projectiles along heading and spins throwing axes. JSON cuboid item models replace flat billboard visuals for spear/javelin/throwing axe; three shields also get thick wooden model + central metal boss. Existing 32x32 inventory PNGs preserved as model texture resources.
- War horn plays a real vanilla goat horn variant instead of an empty synthetic sound event. Carved horn outline darkened to close open edges.
- 0.1.12 recipe fixes and 0.1.10 worldgen fix retained.
- Requires Minecraft 1.21.1, NeoForge 21.1.x, Java 21.

**Test in game:** third-person armor front/rear and slim/default skins; charge/release, recover stacked javelins and throwing axe; throw worn spear and retrieve without duplicates; listen to war horn; blocking stance and item models in both hands.


## 0.1.15: combat and 3D armor prototype

- Cubic custom worn armor on a normal humanoid renderer (NeoForge client extensions; no optional renderer library). Head guard, shoulder volumes, shaped chest, belt, leg/knee/boot details follow normal arm/leg animations. Uses conventional 64x32 UV armor sheets. This is an initial client-facing volumetric model and must be evaluated in actual game for clipping with slim skins/other armor.
- Charging (hold use at least 10 ticks) for all three spear-like/throwable weapons: melee spear, javelin and throwing axe. Melee spear now throws its own stack, with a +1 durability cost on return; launched item can be recovered where it lands. Throws spawn server-side only.
- New in-flight renderer aligns projectiles along heading and spins throwing axes. JSON cuboid item models replace flat billboard visuals for spear/javelin/throwing axe; three shields also get thick wooden model + central metal boss. Existing 32x32 inventory PNGs preserved as model texture resources.
- War horn plays a real vanilla goat horn variant instead of an empty synthetic sound event. Carved horn outline darkened to close open edges.
- 0.1.12 recipe fixes and 0.1.10 worldgen fix retained.
- Requires Minecraft 1.21.1, NeoForge 21.1.x, Java 21.

**Test in game:** third-person armor front/rear and slim/default skins; charge/release, recover stacked javelins and throwing axe; throw worn spear and retrieve without duplicates; listen to war horn; blocking stance and item models in both hands.

## 0.1.15 — equipment animation and flight orientation fixes

- Armor renderer copies the current vanilla humanoid animated pose (crouch, walking, swings) into the custom model before drawing it.
- All extra armor ModelPart children have explicit per-slot visibility, so drawing boots after leggings or a helmet after a chestplate cannot carry stale visibility.
- Corrected 3D projectile pitch/yaw to face the actual flight vector. Throwing axe still tumbles end over end.
- Spear throw refuses a nearly-broken spear rather than silently clamping projectile damage at max-1.
- No new dependencies. 24 items/recipes, JEI support, worldgen and original art remain unchanged.
- Build and server smoke do **not** verify appearance in a real client. Check Steve/Alex, crouching, walking, in-flight models and shields in-game.

## 0.1.15 visual model / inventory-crash triage

- Three 3D shield JSONs now use **dedicated fully opaque 32x32 front-face UV atlases** instead of sampling transparent inventory sprites into horizontal cuboids. The decorative top and bottom gray tab strips have been removed; the central bosses and shield thickness remain. The original 24 inventory sprite files and gameplay shield mechanics are untouched.
- The spear and javelin now have **distinct modeled silhouettes**, a wood-grain shaft, leather bindings, socket, tapered steel head and pronounced metal highlight using a newly authored `norse_weapon_materials.png`. The throwing axe receives a matching faceted blade with a downward beard. These are actual model elements, not a flat `item/generated` billboard; the existing entity renderer displays the 3D baked item in flight.
- All new assets use the exact 32x32 pixel grid and 100% opaque 3D-face textures. No extra rendering library is required.
- `tools/validate_3d_models.py` checks the geometry/material connections and guards against the removed gray top rim reappearing.
- **Important: the user's observed inventory crash is external to Norse Gear**: `jei_plus_plus` calls the missing `mezz.jei.api.ingredients.IIngredientType` because standard JEI is missing. The user's test folder also included both Norse Gear 0.1.11 and 0.1.14 simultaneously. Before testing, remove `jei_plus_plus-1.0.7-1.21.1.jar` and every older `norsegear` JAR, then install only 0.1.15. Standard JEI is optional. This mod cannot safely patch JEI++'s missing dependency.

### Test in a client

1. Launch Minecraft 1.21.1 / NeoForge 21.1.x with only one Norse Gear JAR and JEI++ removed. Enter a new/existing test world. Open inventory in both Survival and Creative, several times.
2. Equip each of three shields in the offhand; look at front/back/top in first/third person, check blocking pose and for any gray horizontal bar or alpha bleed. These should no longer come from the model.
3. Charge and throw spear and javelin. Confirm the shaft and point are 3D in flight and returned item is retrievable, with no gray monochrome rectangle. Check top/bottom orientations and throwing axe rotation.
4. Visually inspect equipped armor: 0.1.14's cubic model remains unchanged in this release; armor shader/clipping is a separate user-client visual review.
