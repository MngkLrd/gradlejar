# Vanilla visual research: R1 pilot (Minecraft Java 1.21.1)

Research issue [#206](https://github.com/MngkLrd/gradlejar/issues/206). On 2026-11-27, 11 PNG files were retrieved as Base64 from both `misode/mcmeta@1.21.1-assets` and `InventivetalentDev/minecraft-assets@1.21.1`, using the same assets/minecraft/textures path. The byte strings and returned Git blob SHA-1 matched across both archives. All 11 were decoded locally, each Git blob SHA-1 independently recomputed, and actual images inspected in a private nearest-neighbor comparison sheet. Direct Mojang download was unavailable locally (DNS failure); official-client equivalence for these 11 was subsequently verified in [R1 run 46016223782](https://github.com/MngkLrd/gradlejar/actions/runs/46016223782). No raw PNGs or sheets committed.

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

## R1 official-client results and R2 expanded observations

R1 CI verified client SHA-1 `30c73b1c5da787909b2f73340419fdf13b9def88`. 3,070 PNGs inventoried, 2,886 measured, 96 metadata-bearing, 88 large, zero decode failures. All eleven first pilot Git blob hashes match the official client. General self-test initially failed because Pillow was absent; the optional image test imports are now guarded, while the dedicated research workflow runs the tests with Pillow installed.

Eight additional PNGs were decoded from misode/mcmeta@1.21.1-assets, local Git blob hashes verified and actual images visually inspected in a private comparison sheet. Their hashes are added to the official-client cross-check (pending next run). Total direct visual inspection: 19 assets, not 3,070.

| Sample | RGB colors | Visible pixels | Same-color x adjacency | Same-color y adjacency |
| --- | ---: | ---: | ---: | ---: |
| oak/spruce/dark_oak/acacia log (each) | 6 | 256 | .108 | .600 |
| birch_log | 8 | 256 | .417 | .163 |
| jungle_log | 9 | 256 | .467 | .058 |
| oak_leaves | 4 | 172 | .352 | .212 |
| spruce_leaves | 6 | 160 | .023 | .287 |
| birch_leaves | 4 | 144 | .324 | .284 |
| acacia_leaves | 5 | 148 | .205 | .183 |
| spruce_sapling | 6 | 83 | .227 | .203 |
| jungle_sapling | 8 | 86 | .075 | .088 |
| birch_sapling | 6 | 98 | .234 | .293 |

**Material archetypes, not universal constraints.** Four bark textures have identical color partitions, unlike birch's horizontal lenticels and jungle's layered horizontal streaks. Reuse an art-approved structural archetype where appropriate; do not require unique pixel positions for every wood species. Birch has light broad planes and localized dark horizontal marks; jungle has narrow ochre/olive horizontal steps with interrupted darker seams. Spruce leaf texture uses sparse narrow directed motifs, unlike oak's wider leaf clusters; acacia and birch vary both cutout mask and cluster arrangement. Foliage raw grayscale must be interpreted with tint; empty pixels are shape information. Saplings differ markedly in whole-plant structure: spruce layered taper, jungle tall irregular stem with lateral foliage, birch bright leafy asymmetry. Do not resize a mature tree silhouette or just recolor a crown. These are visually inspected examples, not fitted acceptance thresholds. Before broad rule changes, expand actual family comparisons and note counterexamples.

## R3 preparation: independent reviewer benchmark

`python -m tools.qa.vanilla_benchmark --minecraft 1.21.1 --out vanilla-benchmark` builds deterministic paired controls from 16 additional official source images in eight families, without publishing any image. Perturbations include visible-pixel shuffling (exact RGBA histogram and alpha mask preserved for these opaque/cutout static assets), shade flattening and alpha-mask disruption through full RGBA shuffling. They are experimental controls, not automatically human-labeled bad art. CI publishes aggregate measurements only. The optional local `--private-deck` writes randomized A/B image sheets and a separate PRIVATE-key.json. Keep key away from reviewer and all images local/private. Record blind choices plus per-image actual aesthetic findings; score source recognition separately from aesthetic verdicts, including UNSURE. Same-family holdout is not generalization to unknown material families. No reviewer has completed this test; no accuracy claim. Do not use any of these results as release evidence.

## Continuation checkpoint

Issue #206 is canonical research scope. R1 corpus confirmed; R2 initial 19 visual observations and family data; R3 paired control generator implemented, CI pending. Next: read current PR and CI, fix any failure, inspect aggregate family distributions and real private control sheets, run independent reviewer in a fresh context without key, collect owner-rejected real samples and separate aesthetic decisions. Then validate an independent review mechanism before touching production Art Review Lock. Do not re-review existing mods or introduce new normal build jobs. No raw vanilla PNG redistribution.
