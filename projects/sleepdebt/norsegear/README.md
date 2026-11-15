# Norse Gear 0.3.3 — Unified 24-item 3D OBJ collection

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

Run `gradle --no-daemon clean build` from this directory with Java 21 and Gradle 9.2.1. Expected output: `build/libs/norsegear-0.3.1.jar`. Install **only one Norse Gear JAR** in `mods`. Remove the incompatible `jei_plus_plus` if running without ordinary JEI; it previously crashed on inventory render because of a missing JEI class.

Run `python3 tools/validate_3d_items.py`, `python3 tools/validate_recipe_data.py`, `python3 tools/validate_gear_render.py`, and `python3 tools/validate_3d_models.py` to validate assets.

Optional animation installation and honest limitations: see [ANIMATION_COMPAT.md](ANIMATION_COMPAT.md).

## QA to verify in-game

1. Open inventory in a clean NeoForge test instance: inspect all 24 models (not sprites) in grid, hotbar and creative tab; check there are no missing textures.
2. In F5 view, inspect helmet, chest, legs and boots (Steve and slim Alex), crouching, walking and holding shield in both hands.
3. Test all three shields blocking from the left hand. They should not intersect the torso; camera-facing shift may need a transform tweak on specific player models.
4. Charge and throw spear/javelin/axe while watching the model, direction and hit/pickup. Test stone shot separately: distinct 3D projectile renderer preserved.
5. Install Better Combat NeoForge 2.4.0 for 1.21.1 and its required dependencies, then verify seven animation profiles; remove Better Combat and verify standalone operation again.
6. Test JEI crafting, worldgen, server reconnect, multiplayer and tool repair.

**Five-item concept translation:** Rebuilt bearded axe, Dane axe, spear, javelin and round shield as real detailed JSON cuboid geometry with a separate 256px UV texture; upgraded the worn-armor ModelPart geometry and rebuilt its 256px UVs. Genuine source-driven previews: `PREVIEW_3D_ACTUAL_031.png` and `PREVIEW_ARMOR_ACTUAL_031.png`. Preview code in `tools/render_showcase031.py`. This first model translation is intentionally more blocky than the generated concept art; it is not a promise of 1:1 sculpt fidelity.

**Known limitation:** static GL rendering and real animated Better Combat behaviour have not yet been directly tested in Minecraft client by the build process. The supplied `PREVIEW_3D.png` is a Python orthographic schematic, not an actual game screenshot.


## 0.3.2 Concept-to-Game Geometry
- Rebuilt spear/javelin leaf with faceted, stepped steel sections and raised spine.
- Round shield gains 12 iron rim rivets, inset clamps, layered umbon and reverse grip.
- Worn armor adds animated helmet bands, cheek rims, armor straps, chest plate, pauldrons and greaves.
- Distinct 256x256 UV pixels for each worn-armor structural detail.
- Inspect PREVIEW_3D_ACTUAL_032.png and PREVIEW_ARMOR_ACTUAL_032.png, both source-rendered.
- Optional Better Combat support, all original gameplay/recipes/worldgen preserved.


## 0.3.3 — full 3D inventory unification
- All **24** registered inventory item models now have distinct native Wavefront OBJ meshes using the same 256×256 pixel material atlas. Includes seven redesigned combat objects, utility items, four armor icons and resource materials.
- Celtic and bossed shields are no longer rectangular stair-step bodies. Polygonal circular shield faces, iron/bronze rims, raised bosses, rivets and separate rear grips.
- Sword, seax and all axes have recognisable blade contours rather than rectangular cube heads. Utility items have physically shaped meshes, not flat billboard placeholders.
- **Worn armor is still animated Java ModelPart armor**. The new OBJ armor meshes are for the inventory, hand and item-frame visuals. They do not replace the animated wearable model.
- Existing crafting, item IDs, damage, projectile entities, 3D flight renderers, bog iron worldgen, JEI and optional Better Combat profiles are unchanged.
- Static tests: `python3 tools/validate_obj_033.py`, `validate_3d_items.py`, `validate_3d_models.py`, `validate_16_art.py`, `validate_recipe_data.py`, `validate_gear_render.py`.
- Build: Java 21, Gradle 9.2.1, `gradle --no-daemon clean build` → `build/libs/norsegear-0.3.3.jar`.
- A server smoke test validates datapack loading, **not** OpenGL display transforms; GUI and hands must still be inspected in a real client.
