# Block and vegetation art

Mandatory when changing blocks, plants, trees or related models. Follow [common Visual QA](VISUAL-QA.md), its material acceptance matrix and applicable [atlas entry](VANILLA-ART-ATLAS.md). Read this branch only for applicable assets.

## Blocks and vegetation

- Work at block-scale 16 units per side; preserve texture scale on cut or rotated geometry. Preview a 3x3 tiled surface before detailed painting and after refinement, including top/side transitions and adjoining vanilla blocks. Check repeated knots, stripes, seams and directional light baked into tile edges.
- Minimize unnecessary cuboids and use purpose-built planes/cutouts for thin foliage, tendrils or small details. Check texel density, face orientation, transparent holes, tint, shading and silhouette from several angles. Check variants and neighbor states (connections, rotations, upper/lower plant halves, waterlogging when applicable); do not infer correct placement from a standalone PNG.
- For Terrain-style vegetation, compare species as a family in actual grass/forest/desert settings. They must have distinguishable silhouettes and coherent material/color treatment; verify biome tint, contact with ground, visibility through crossed planes and ordinary gameplay collision where relevant.

