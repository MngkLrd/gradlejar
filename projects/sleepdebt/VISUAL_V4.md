# Sleep Debt Visual Overhaul 3.0 Alpha 6

## Why this asset pass exists
The previous models had mismatched arm/leg/hood UVs and mostly formulaic textures.
This pass replaces visible surfaces with material-specific pixel clusters. No texture
uses anti-aliasing, resampling or stock photographic images. Textures are
real native Minecraft resources, not standalone concept art.

## Art and engineering contract
- 16x16 textures for blocks/items, eight distinct coffee crop stages.
- 128x64 original Sleepless atlas. Every required cube face resolves to valid,
  opaque texels; the custom MobModel texOffs and texture size match it.
- Six block models have intentional per-face texture selection and explicit UVs.
- Camp Lantern uses translucent glass RGBA and a separate inner flame.
- Dreamcatcher uses a cutout woven net with transparent gaps, not a solid slab.
- Coffee machine, bedroll, awning, lantern and dreamcatcher geometry refined.
- Inventory UI, HUD and journal share a palette and native Minecraft game fonts.
- Coffee maker GUI slots remain aligned with CoffeeMenu coordinates.

## Production workflow
The reviewed PNGs are version-controlled under src/main/resources/assets/sleepdebt/textures.
Legacy procedural generators in tools/ now fill only absent textures and never overwrite
these reviewed assets during a Gradle build.

## Quality gates
tools/verify_release.py checks:
1. Presence of all required files in the compiled JAR
2. PNG dimensions and internal PNG chunk CRCs
3. Opaque UV coverage for every Sleepless part in the 128x64 atlas
4. Block model cuboid bounds, texture aliases, explicit UV ranges
5. Item models, blockstates, recipe and Patchouli translations

## Remaining runtime QA
A successful Gradle build proves the textures and models are well formed, but not that
every face looks right under Minecraft lighting. Test:
- Day/night/shader pack with translucent lantern glass
- Hood, legs and arms of Sleepless, front/back and during animations
- Model seams at Minecraft GUI scales 2-4
- Steve/Alex hallucinations against fog and dynamic lighting
- Item textures in hand, on the ground and in inventory
- All eight crop stages and previous world save compatibility

## Non-goals
No new mandatory dependencies, unrelated gameplay rewrite, or changed block/item IDs.


## Final 3D refinement
The Dreamcatcher was revised after a second isometric geometry preview to use an octagonal frame, genuinely perforated mesh, a centered suspension loop and three feather pendants. This model uses 16 cuboids with explicit face UV coordinates.


## Inventory consistency pass
All 22 registered items with standalone PNG models have reviewed native 16x16 icon art. Strong, long, caramel and standard coffee have different silhouettes; herbal and dream teas have distinct marks. Cloth, medicine, botanicals and ingredients are rendered as readable item clusters without anti-aliasing. Original block items continue to inherit the revised 3D block models.

