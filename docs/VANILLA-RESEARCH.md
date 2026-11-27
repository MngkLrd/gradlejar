# Vanilla visual research: R1 pilot (Minecraft Java 1.21.1)

Research issue [#206](https://github.com/MngkLrd/gradlejar/issues/206). On 2026-11-27, 11 PNG files were retrieved as Base64 from both `misode/mcmeta@1.21.1-assets` and `InventivetalentDev/minecraft-assets@1.21.1`, using the same assets/minecraft/textures path. The byte strings and returned Git blob SHA-1 matched across both archives. All 11 were decoded locally, each Git blob SHA-1 independently recomputed, and actual images inspected in a private nearest-neighbor comparison sheet. Direct Mojang download was unavailable locally (DNS failure); official-client equivalence is a separate CI check, not yet established by this pilot. No raw PNGs or sheets committed.

| Resource (under textures/) | Visible RGB colors | 4-connected components | Largest component | Dominant color % |
| --- | ---: | ---: | ---: | ---: |
| block/spruce_log | 6 | 91 | 21 | 42.2 |
| block/spruce_log_top | 8 | 101 | 33 | 27.3 |
| block/spruce_leaves | 6 | 133 | 3 | 30.0 |
| block/oak_leaves | 4 | 110 | 6 | 33.1 |
| block/oak_log | 6 | 91 | 21 | 42.2 |
| block/oak_log_top | 9 | 102 | 33 | 27.3 |
| block/dirt | 7 | 139 | 18 | 41.8 |
| block/stone | 4 | 77 | 48 | 46.1 |
| block/iron_ore | 9 | 151 | 9 | 28.9 |
| item/leather_chestplate | 6 | 44 | 24 | 28.3 |
| block/spruce_sapling | 6 | 54 | 7 | 32.5 |

All are 16x16; fully transparent pixels excluded. Metrics describe raw images, not render colors. Leaves and leather armor use grayscale base textures with game tint/overlays; do not infer final colors from raw image alone.

## Observations and counterexamples

- Oak and spruce log side have exactly the same partition of all 256 pixels into colors under a one-to-one palette mapping. Oak-to-spruce top best per-source-color match is 253/256 (not a bijection). Their visual difference is substantially palette, not geometry. Recoloring a suitable high-quality base can be authentic; forbid careless reuse, not reuse itself. Both side textures have same-color vertical adjacency 0.600 vs horizontal 0.108: pronounced grain, but interrupted by secondary clusters. Both top textures have almost equal same-color adjacency along x/y (~0.32–0.33); rings use local offsets and shade variation.
- Spruce leaves have 160 visible pixels, 133 components, max component 3, 96 transparent pixels; oak leaves 172 visible, 110 components, max 6, 84 transparent. Very small components and ordered diagonals can be deliberate, especially for needles. A simplistic anti-singleton rule would misclassify legitimate texture design.
- Stone's four-color palette is authentic; its large connected regions communicate rock structure. Dirt uses varied irregular aggregates plus small gray inclusions. Iron ore has lighter mineral regions, darker boundaries and a structured rock field. Never require palette complexity across unrelated materials.
- Leather chestplate's grayscale base uses shaded edges, collar, sleeves and folded central planes; actual leather color comes from tinting. Spruce sapling: vertical dark trunk, tiered asymmetrical foliage and silhouette. Different categories require distinct criteria.
- These are qualitative observations and exploratory measurements, not universal thresholds or an independent reviewer benchmark. Confirm with larger family samples and held-out examples before changing automatic policy.

## Reproduce / expand

`python -m tools.qa.vanilla_corpus --minecraft 1.21.1 --out vanilla-corpus` downloads and verifies the official client via the existing art_compare helper. Optional --client reuses the exact local client. Inventories every PNG under the vanilla texture tree, hashes all, measures supported static PNGs <=128px per side, explicitly marks metadata/large images and reports family hints. Layout hashes detect exact color-partition reuse independently of palette, not near-identical or semantically equivalent art. Output metrics only; no source art redistribution. Standalone research workflow, no Gradle or game startup. Full human/vision review, animation/UV/GUI analysis, negative controls and held-out reviewer calibration remain subsequent stages.
