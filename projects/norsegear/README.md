# Norse Gear 0.3.0 — 3D Equipment and Optional Combat Animations

Minecraft **1.21.1**, **NeoForge 21.1.262** (tested compile target), **Java 21**, **ModDevGradle 2.0.148**, **Gradle 9.2.1**.

## Release changes

- Replaced 24 `models/item/*.json` generated/flat inventory items with 24 distinct cuboid-based Minecraft item models. Every item has real 3D volume, including tools, shields, weapons, resources and armor icons.
- New `textures/item/norse_3d_materials.png` 64×64 material atlas: steel, iron, wood, leather, bronze, bone, stone, mail, painted shield colors. Face-based directional value gives readable sides without thick black outlines.
- Shield hand transform retains the outside-of-arm offset and shifts backward by 0.6 model units relative to v0.2.1; face geometry remains convex with central metallic boss.
- Long weapons keep their shaft aligned with +Y and preserve their existing throwing/rotating entity renderer. No 2D billboard fallback for held models.
- All 24 previous flat **16×16 PNG** remain as unused art/fallback source resources; rendered models now use the shared 3D material atlas.
- Armor item icons are 3D; worn armor was already volumetric and continues using a custom humanoid model with the two dedicated armor UV layers.
- Added dedicated optional **Better Combat 2.4.0 + Minecraft 1.21.1 NeoForge** weapon profiles for seven weapons: dagger, axe, heavy axe, sword and spear animations. The mod has **no mandatory runtime animation-library dependency**.
- Preserved recipes, JEI integration, forge-tier statistics, bog iron worldgen, projectiles, pickup behaviour, cooldowns and vanilla standalone behaviour.

## Build

Run `gradle --no-daemon clean build` from this directory with Java 21 and Gradle 9.2.1. Expected output: `build/libs/norsegear-0.3.0.jar`. Install **only one Norse Gear JAR** in `mods`. Remove the incompatible `jei_plus_plus` if running without ordinary JEI; it previously crashed on inventory render because of a missing JEI class.

Run `python3 tools/validate_3d_items.py`, `python3 tools/validate_recipe_data.py`, `python3 tools/validate_gear_render.py`, and `python3 tools/validate_3d_models.py` to validate assets.

Optional animation installation and honest limitations: see [ANIMATION_COMPAT.md](ANIMATION_COMPAT.md).

## QA to verify in-game

1. Open inventory in a clean NeoForge test instance: inspect all 24 models (not sprites) in grid, hotbar and creative tab; check there are no missing textures.
2. In F5 view, inspect helmet, chest, legs and boots (Steve and slim Alex), crouching, walking and holding shield in both hands.
3. Test all three shields blocking from the left hand. They should not intersect the torso; camera-facing shift may need a transform tweak on specific player models.
4. Charge and throw spear/javelin/axe while watching the model, direction and hit/pickup. Test stone shot separately: distinct 3D projectile renderer preserved.
5. Install Better Combat NeoForge 2.4.0 for 1.21.1 and its required dependencies, then verify seven animation profiles; remove Better Combat and verify standalone operation again.
6. Test JEI crafting, worldgen, server reconnect, multiplayer and tool repair.

**Known limitation:** static GL rendering and real animated Better Combat behaviour have not yet been directly tested in Minecraft client by the build process. The supplied `PREVIEW_3D.png` is a Python orthographic schematic, not an actual game screenshot.
