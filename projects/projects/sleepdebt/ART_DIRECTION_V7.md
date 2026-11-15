# Sleep Debt 3.0 Alpha 7 | Cluster Discipline Art Direction

This revision replaces **45 actual PNGs**: 15 block materials, 8 coffee crop
stages, and 22 inventory items. All are **native 16x16**, RGBA, nearest-neighbor,
with intentionally authored geometry. The monster's 128x64 atlas and the
176x166 / 138x58 / 228x188 GUI atlases remain unchanged from Alpha 6.

## Non-negotiable pixel-art rules

**Cluster discipline.**
Create coherent 2–7 px masses before adding highlights. Clusters have one
surface/lighting purpose. Never sprinkle independent colors to simulate detail.
Restrict working palettes to a small, intentional number of colors; this
revision uses 3–14 per material/subject (including transparent).

**Broken edges.**
Not every edge should be jagged: metal hardware still needs rigid right angles.
Break organic contours, cloth wrinkles, leaf boundaries, worn areas and
nonuniform reflection highlights. Avoid equal, repeated 2-pixel steps.

**Selective detail.**
Spend the few available pixels where they explain form: copper recesses,
pillow compression, coffee machine display, fruit against leaves and
worn lantern metal. Leave broad midtone shapes quiet. Secondary surfaces
must not compete with the primary silhouette.

**Pixel-level material storytelling.**
Copper shows hammered broad facets and local patina; wood has interrupted
grain with one chosen growth direction; canvas has taut and slack folds,
cloth has stitches and compression; clay has rough firing scars; steel has
sharper specular cuts; glass uses alpha and directional reflections.
A 16x16 asset should convey material with the *position* of a highlight,
not just its color.

**Banding control.**
Avoid concentric stair-step outlines and equally spaced full-width lines.
Use interlocking light/dark clusters. Thin isolated shine is permissible only
on genuinely reflective material or on designed fasteners.

**Anti-pillow shading.**
Pillows are not glowing circles surrounded by shadow. Specify a broad top-left
plane, a compressed lower/right edge, and irregular seam segments. Never
draw a uniform one-pixel outline around every curved soft object.

**Tile rhythm.**
One tile should have a broad dominant plane plus uneven secondary groupings.
For the current single-block furniture models, avoid repetitive 4-pixel
stripes and large seamless gradients. If these textures are later reused in
large worldgen structures, repaint edges for true multi-tile transitions.

## Implemented changes
- Metal: coffee_machine, copper_top, coffee_screen, iron_rim
- Textiles and wood: stitched_fabric, bedroll_edge, bedroll_pillow,
  camp_canvas, wooden_pole
- Glass and dream ornaments: rest_lamp, lantern_flame, dream_ring, dream_woven
- Ceramics: incense_clay, incense_coals
- Botanical silhouettes: 8 stages of coffee_bush, cherries, seeds,
  roasted beans and herbs
- 22 inventory icons, individually recognizable by content and shading
- Materials respect original 3D model UV coordinates and existing IDs.

## Quality gate
The project now has tools/verify_pixel_art.py, run from verify_release.py in
GitHub Actions. It decompresses PNG RGBA rows (including PNG filters 0–4),
verifies per-asset palette limits, alpha requirements, item silhouette
occupancy, and uniqueness of the 8 crop stages.

No automated test can grade artistic quality. Preview renders and an
independent in-game review remain mandatory before stable release.

## Compatibility
No changes to mod ID, network protocol, savegame data, Java classes, registry
IDs, recipes, loader metadata beyond version number, or external dependencies.
Existing pixel art generators fill absent files only and do not overwrite
these checked-in PNGs during Gradle build.
