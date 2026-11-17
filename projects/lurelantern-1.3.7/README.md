# Lure Lantern 1.3.10 | NeoForge 1.21.1

Fix for a reproducible first-person Advanced lantern rendering defect under shaders: held glass appeared opaque while placed lantern glass was transparent.

- The held Advanced item now routes blue glass through `RenderType.itemEntityTranslucentCull`, not the previous `entityTranslucent` entity layer. This supplies the item-specific translucent blending / culling path.
- The existing transparent glass PNGs (alpha 77/255) remain unchanged.
- Preserve the vanilla-style metal frame, captured entity drawing, four glass sides, right/left hand item scale, and third-person transforms.
- No changes to block models, placed-lantern rendering, basic purple lantern, recipes, game mechanics, loot or capture NBT.

Build with Java 21, Gradle 9.2.1 and NeoForge 21.1.262: `gradle clean build`.

Output: `build/libs/lurelantern-1.3.10.jar`.
Server GameTest: `gradle runGameTestServer`.

Visual confirmation: check filled Advanced in both hands under shader and vanilla rendering. Compiling and headless GameTests cannot prove shader-dependent blending.
