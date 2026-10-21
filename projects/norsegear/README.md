# Norse Gear 0.2.1 - Vanilla Clarity Pass

Minecraft 1.21.1 / NeoForge 21.1.262 / Java 21 / Gradle 9.2.1.

Complete standalone mod: 24 registered items, 24 recipes, terrain bog iron, optional JEI integration.

## Visual release

- Re-authored all 24 inventory icon sprites directly on the 16x16 pixel grid; no downscaling or image-generation atlas cutouts. Deliberate material palettes: clean steel, bright timber, warm leather, horn, gold, green and red shield paint. No transparency smoothing.
- Rebuilt three shield solid-face textures to match the icon graphics. Their 3D geometry remains untouched except **third-person Z offset 4.4 -> 2.4**, moving the held shield backwards in another plane without reducing the outboard X displacement. Validate this positioning on both Steve and Alex in game.
- Updated the 3D spear/javelin/throwing-axe material palette to align with the native sprites, while keeping projectile physics and spinning throwing-axe behavior unchanged.
- Reworked 64x32 wearable armor material palette with brighter specular surfaces, warm leather, steel and genuine transparent holes in the chainmail UV. Reduced shoulder cuboid width slightly to improve silhouette; head remains an open-face nasal helmet.
- No game rule, item IDs, recipe, loot or worldgen changes. No Curios/AzureLib/GeckoLib dependency.

## Build

Use Java 21 and Gradle 9.2.1:

```sh
gradle --no-daemon clean build
```

Output `build/libs/norsegear-0.2.1.jar`. GitHub workflow in MngkLrd/gradlejar builds a release JAR and independently checks dedicated-server start. A passed dedicated server boot does **not** verify GUI, animation or item placement on a real client.

## Test checklist

1. Remove every older Norse Gear JAR before installing 0.2.1.
2. Keep incompatible JEI++ disabled (independent UI crash if installed without a compatible JEI).
3. Confirm 24 sprites are readable at actual 16px inventory scale.
4. Equipped shield: third-person from front, both sides, walking, crouching, block animation, offhand and main-hand, Alex and Steve.
5. Equipped armor: front/back, face open, chainmail alpha gaps show player skin, legs/shoulder animation, no texture bleeding.
6. Throws: spear, javelin, thrown axe, sling stone collision and flight, pickup, sound separation.
7. Existing world reload, JEI crafting pages (if JEI is installed), bog iron generation in new chunks.
