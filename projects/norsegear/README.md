# Norse Gear 0.2.0 — Native 16×16 Art Overhaul

Minecraft 1.21.1 · NeoForge 21.1.262 · Java 21 · Gradle 9.2.1 / ModDevGradle.

This is the complete **standalone** Norse Gear mod, not an image or resource pack. Original
item icons were redesigned at the final 16×16 resolution directly on the pixel grid,
not reduced or cropped from 32×32 art. 24 registered items and 24 recipes remain.

## Visual design

- 24 independent, native 16×16 transparent PNG item sprites, designed as a consistent
  early-medieval survival set (cold iron, walnut, worn leather, linen, muted bronze).
- Updated 16px 3D item-model material palettes and opaque shield-face textures.
- Shields repositioned to the **outer** side of the player arm in third/first person;
  all three keep solid face geometry and vanilla shield behavior.
- `stone_shot` is now drawn as a rounded item sprite in the inventory. In flight a
  separately modeled three-cuboid pebble follows the sling entity (not a full cube).
- Armor keeps existing volumetric attachments and translucent chainmail openings. The
  helmet now has a narrower nasal guard and shorter cheek guards; pauldrons are trimmed.
- All server-side mechanics (bog iron, crafting, projectiles, JEI optional API, horns,
  armor attributes) retained from v0.1.16.

**Limitation:** Spear, javelin and throwing axe still use 3D item JSON for equipped/inventory
views; their new 16×16 sprite textures are supplied as `layer0` art but Minecraft's
cube-model renderer takes priority in the inventory. Dedicated per-display render
models are a separate compatibility task. 3D projectile meshes retain the approved flight
mechanics. Client-side visual quality requires in-game approval.

## Build

```bash
gradle --no-daemon clean build
```

Real result: `build/libs/norsegear-0.2.0.jar`. Release jar is supplied through the
connected GitHub CI Build Hub after a successful Java 21 compile and dedicated server boot.

## Tests

```bash
python3 tools/validate_recipe_data.py
python3 tools/validate_gear_render.py
python3 tools/validate_3d_models.py
python3 tools/validate_16_art.py
```

Use a clean mod list with exactly one Norse Gear JAR. JEI++ must not be loaded without a
compatible regular JEI installation; an earlier user crash was traced to JEI++, not
Norse Gear. Testing that crash scenario requires the actual Minecraft client.

See `TESTING.md` for the release checklist. The art generator source is included as
`tools/art/draw_16.py` for future changes, but textures are delivered as regular PNG.
