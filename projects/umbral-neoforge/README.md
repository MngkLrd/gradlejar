# UMBRAL 0.1.0 — Physical Shadows
Minecraft 1.21.1 / NeoForge 21.1.228 / Java 21. No external runtime dependency.

The emitter projects opaque full-block silhouettes from a point at its top center into free space ahead. Cross-sections are projected analytically, with merged vertical shadow intervals. Each surface is sampled on a half-block horizontal / quarter-block vertical grid. Actual generated blocks use four independent thin collision plates and corresponding multipart meshes; no preset strip of blocks, no decal, no shaders required. Multiple casters can merge or create separate levels. Ground and other non-air blocks are never replaced. Physical shadow surfaces are not ordinary collectible blocks.

## First test
Create a new creative flat world. Face east and place an Umbral Projector on a raised platform; its beam faces the direction you looked. Use a full opaque block two blocks ahead at the same Y as the projector, with air underneath and beyond. The caster's top is at the emitter height, forming a near-flat widening shadow. Jump onto the caster or the surface from its side. Move or reshape the caster; remove it or the emitter; the geometry recomputes. Raise or lower the emitter for inclined shadows. Make sure there is free air where the projection would appear (not solid terrain). Use empty-hand right-click on emitter for 8/12/16 block range. Sneak empty-hand use toggles the emitter, and a redstone signal disables it.
For a clean coordinate test use a superflat world with air at Y=80:
```
/setblock ~ ~5 ~ umbral:projector[facing=east,enabled=true,lit=true,range=1]
/setblock ~2 ~5 ~ minecraft:stone
```
Fly up and walk on the generated silhouette. Place additional stone blocks sideways or above the first to change its shape. Remove the caster and verify cleanup. If using commands, the projector registers on block placement. Test redstone, saving/reloading, overlapping emitters and chunk boundaries.

## Limits
Only opaque full blocks in the near casting zone (depth 1–5, vertical -2 to +3) contribute. Maximum 16 blocks range, lateral cap 6 and vertical cap ±5, 640 cells per emitter. Work is budgeted to two emitters per four ticks per dimension; changes are not instantaneous with many projectors. No natural sun shadows, mobile entities, vertical walls or carrying motion yet; existing solids occlude by occupying their own cells, but distant terrain does not perform full secondary light occlusion. The projector uses actual vanilla block light around its housing, not a world-wide directional lighting rewrite. Generated surfaces are solid matte voxel meshes, not shader-rendered sunlight. Saved ownership prevents two emitters removing each other's surfaces; cleanup waits until affected chunks load. No destructive replacement of normal blocks; no forced chunk loading. First alpha, use a disposable world.
