# Halo Haven: analysis of vanilla Minecraft texture language

Research target: **Minecraft Java Edition 1.21.1**, native 16×16.

## Sources inspected

- [Blockbench Minecraft Style Guide](https://blockbench.net/wiki/guides/minecraft-style-guide/):
  mixed resolutions, hue/value ramps, banding, pillow shading, and block tiling.
- [PrismarineJS Minecraft Assets, version 1.21.1](https://github.com/PrismarineJS/minecraft-assets/tree/master/data/1.21.1/blocks):
  exact 16×16 PNGs examined for Stone, Calcite, Tuff, Moss Block,
  Grass Block Top/Side, Oak Planks, Stone Bricks, Iron Ore and Oak Leaves.
  Only analytical measurements are retained; **no Mojang texture pixels
  are included in this repository or our shipped JAR**.

## Research outcomes

| Asset face | Used RGBA colors | Dominant share | Different adjacent pixels | Largest single-color connected region |
| --- | ---: | ---: | ---: | ---: |
| Minecraft Stone | 4 | 46% | 60% | 48 |
| Minecraft Calcite | 6 | 31% | 68% | 12 |
| Minecraft Tuff | 5 | 30% | 68% | 12 |
| Minecraft Moss | 6 | 28% | 84% | 7 |
| Minecraft Oak Planks | 7 | 20% | 74% | 8 |
| Minecraft Stone Bricks | 7 | 23% | 65% | 18 |
| Minecraft Iron Ore | 9 | 29% | 78% | 9 |
| Minecraft Oak Leaves | 5 | 33% | 75% | 15 |
| Halo Haven 1.6 Cloudstone | 4 | 66% | 36% | 168 |
| Halo Haven 1.6 Skygrass | 3 | 70% | 34% | 178 |
| Halo Haven 1.6 Moonstone | 4 | 67% | 32% | 171 |
| Halo Haven 1.6 Azure Planks | 4 | 51% | 42% | 49 |

The 1.6 texture treatment often reduced a material to an almost blank color
field with two clusters. That is why it looked vector-smooth despite 16×16
resolution. Sparse speckle is **not** an adequate correction.

## Specific lessons

1. **Surface coverage:** Minecraft natural materials fill almost every
   region with subtly connected 1–5 pixel mottling, not giant smooth
   illustrations or isolated symbols. A four-color stone can be much more
   textured than a seven-color cartoon mineral.
2. **Material hierarchy:** Chalk / marble has softer, desaturated patches;
   moss and turf use granular soft clumps; worn stone uses short chipped
   facets; wood follows horizontal grain, not circular decorative shading.
3. **Short ramps:** Use 4–9 colors per face and selective hue changes; color
   count alone does not predict quality. Vanilla Stone uses just four.
4. **Structure:** Dressed bricks keep offset courses and thin mortar;
   wood planks have four mostly 4px-high horizontal boards. Inlaid
   mosaics and magical portals can be more decorative than ground.
5. **Alpha:** Opaque blocks are alpha 255; leaves / plants need exact
   cutouts 0 or 255; actual glass panes need interior alpha <255.
6. **Tiling:** A sprite is not ready until repeated 4×4 and compared beside
   existing vanilla blocks under ordinary light. A repeated diagonal pattern
   across a wall is an artifact even if statistics match vanilla.
7. **Native size:** Author at 16×16; never blur, bilinearly downsample or
   upscale for fictitious detail.

## Working method

Compare a representative Minecraft block in the **same material family**,
then author distinct, original pixel clusters. Drafts and sketches may use
software, but the shipped pixel palette index maps are explicit, frozen
16×16 assets in `art/pixel_atlas.json`; no procedural noise runs in
the resource compiler. In particular, rejected drafts included diagonal
striping in Moonstone, symmetric heart-shaped leaf silhouettes and
oversized flat Cloudstone patches.

### Automated diagnostics

`python3 ci/art_quality.py` checks palette usage, local color transitions,
connected flat patches and alpha modes. Thresholds are material-specific.
This is a guardrail for regression, not a substitute for human art direction.

### Manual visual acceptance still required

- Single texture zoomed ×12 **and** native-size rendering.
- 4×4 repeated surface for every natural block and every brick/plank.
- Mixed builds with vanilla calcite, tuff, moss and stone bricks.
- Transparent foliage at distance and in Fancy/Fabulous graphics modes.
- No strong repeated diagonal bands, isolated dots or symmetric icon motifs.
