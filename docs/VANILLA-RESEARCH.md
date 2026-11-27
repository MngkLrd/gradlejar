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

Eight additional PNGs were decoded from misode/mcmeta@1.21.1-assets, local Git blob hashes verified and actual images visually inspected in a private comparison sheet. All eight hashes were subsequently confirmed against the official client in [run 46016919441](https://github.com/MngkLrd/gradlejar/actions/runs/46016919441), bringing the verified sample total to 19/19. Total direct visual inspection: 19 assets, not 3,070.

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

Issue #206 is canonical research scope. R1 corpus confirmed; R2 initial 19 visual observations and family data; R3 paired control generator and 40 pairs confirmed by run 46016919441; PR #207 merged. Next: read current PR and CI, fix any failure, inspect aggregate family distributions and real private control sheets, run independent reviewer in a fresh context without key, collect owner-rejected real samples and separate aesthetic decisions. Then validate an independent review mechanism before touching production Art Review Lock. Do not re-review existing mods or introduce new normal build jobs. No raw vanilla PNG redistribution.

## Confirmed corpus family summary (R2/R3)

Measured on official 1.21.1 client, [research run 46016919441](https://github.com/MngkLrd/gradlejar/actions/runs/46016919441). These are filename-based groups, not visually adjudicated material labels, and include different texture dimensions, masks and utility layers. Do not promote the ranges to automatic acceptance thresholds.

| Family hint | Measured / inventoried | RGB color count min / median / max | Components min / median / max |
| --- | ---: | --- | --- |
| Bark | 16 / 18 | 2 / 6 / 23 | 2 / 88.5 / 154 |
| Log end | 21 / 21 | 4 / 9 / 12 | 21 / 96 / 117 |
| Foliage | 12 / 12 | 4 / 5 / 14 | 61 / 107.5 / 145 |
| Sapling | 7 / 7 | 6 / 8 / 12 | 54 / 69 / 81 |
| Soil | 5 / 5 | 7 / 9 / 77 | 139 / 180 / 251 |
| Ore | 18 / 18 | 9 / 12 / 16 | 135 / 159 / 204 |
| Armor icon | 29 / 29 | 1 / 7 / 9 | 4 / 26 / 97 |

R3: 16 additional official source images, 40 paired perturbations (16 visible shuffle, 16 ramp flattening, 8 RGBA shuffle). 24 pairs preserve RGB palette count, all 40 preserve visible pixel count. These controls demonstrate non-identifiability from counts alone. Reviewer status NOT_RUN; no validated aesthetic labels for synthetic mutations. Do not call these a benchmark of final art acceptance.

## R4 — unfitted metric diagnostics

The benchmark now reports five prespecified single-metric directional baselines (more colors, fewer isolated pixels, more same-color neighbor edges, lower adjacent brightness delta, larger connected component). Compare original and perturbed images, count correct source-side choice, wrong choice and abstention for equal/missing values, and break down by perturbation. No fitting, no image-derived learned model, no aesthetics claim. Results are source-identification diagnostics on correlated synthetic pairs, not independent samples or proof of style quality. Never convert these numbers into release gates. Preserve actual measurements and error cases to inform a future independent image-based review. Reviewer must still inspect actual images and judge their meaning, not try to reverse these mutation algorithms.


## R4 results (fixed exploratory controls)

Verified [run 46017233045](https://github.com/MngkLrd/gradlejar/actions/runs/46017233045); PR #208 merged. All metrics are single-direction, unfitted source-choice diagnostics, not aesthetic accuracy. Ties/missing values are abstentions.

| Hypothesis | Correct | Wrong | Abstain |
| --- | ---: | ---: | ---: |
| More palette colors | 16 | 0 | 24 |
| Fewer isolated pixels | 21 | 18 | 1 |
| More same-color neighbor edges | 19 | 20 | 1 |
| Lower adjacent mean value delta | 29 | 11 | 0 |
| Larger connected component | 18 | 17 | 5 |

The more-same-color-edges heuristic incorrectly prefers all 16 flattened ramps. Larger-component and fewer-singleton hypotheses also fail frequently on this mode. This is concrete evidence against turning these numbers into artistic pass gates. It does not show that no combination or trained image model can help.

## R5 — source audit, additional item/material examples, sufficiency review

Nine PNGs decoded from the version-pinned misode/mcmeta archive (1.21.1-assets), local Git blob SHA-1 recomputed and matched, viewed at 1x and nearest-neighbor enlargement. Official-client hash cross-check added for all nine. See [atlas](VANILLA-ART-ATLAS.md) for material-specific observations.

| Texture | RGB colors | Visible pixels | Components | Largest component |
| --- | ---: | ---: | ---: | ---: |
| item/glass_bottle | 8 | 42 | 25 | 5 |
| item/bucket | 8 | 146 | 38 | 18 |
| item/copper_ingot | 8 | 135 | 63 | 9 |
| item/iron_ingot | 8 | 135 | 51 | 30 |
| item/brick | 7 | 135 | 42 | 33 |
| item/leather | 6 | 128 | 52 | 18 |
| item/bread | 7 | 138 | 57 | 18 |
| item/apple | 10 | 114 | 53 | 24 |
| block/oxidized_copper | 8 | 256 | 110 | 37 |

All are 16x16, with no partially transparent pixels. The bottle represents transparent glass via cutout space and edge cues, not partial alpha in this image. Copper, iron and brick share closely comparable shapes but differ substantially in surface treatment. Do not generalize a fully oxidized block to all patina states. Glass_bottle was already in the fixed R3/R4 exploratory benchmark, so it must not be used as an unseen holdout in subsequent reviewer evaluation.

### Source status (2026-11-27)

- [Texture update interview with Jasper Boerstra](https://www.minecraft.net/en-us/article/try-new-minecraft-textures): read; coherence across pack, restrained AA and geometry, iterative feedback.
- [Caves & Cliffs developer Q&A](https://www.minecraft.net/en-us/article/caves---cliffs--part-i--dev-q-a): read; pixel relationships and color theory.
- [Blockbench style guide](https://blockbench.net/wiki/guides/minecraft-style-guide/): read; community guide, not Mojang-authored. Covers proportional geometry, texel scale, HSV/ramps, deliberate shading, banding, tiling and entity UVs.
- [Meet the Phantom](https://www.minecraft.net/en-us/article/meet-phantom): read; first-party art redesign case, intentional alpha for added body detail, willingness to iterate beyond a rigid style assumption.
- [Maker Series: Goblin Chef](https://www.minecraft.net/en-us/creator/article/minecraft-maker-series-episode-1--making-goblin-chef): official introduction read; Peter Sheff's modeling/texturing/animation walkthrough identified, full videos NOT_REVIEWED.
- [Lookin' Good: How We Make Minecraft](https://www.youtube.com/watch?v=UlVZgIdoR_A) and [Minecraft Live: The Art of Caves & Cliffs](https://www.youtube.com/watch?v=4WJ7LmY3mZA): official video references located; full contents NOT_REVIEWED. Do not attribute detailed rules before inspecting.
- [Microsoft entity modeling and animation](https://learn.microsoft.com/en-us/minecraft/creator/documents/entitymodelingandanimation): inspected texturing, UV/bones and animation process; Bedrock schemas not Java.
- [Java snapshot 25w08a](https://www.minecraft.net/en-us/article/minecraft-snapshot-25w08a): first-party change note for spawn egg readability/accessibility, later-version design example, not a 1.21.1 texture source.
- [Bedrock Copper Age changelog](https://www.minecraft.net/en-us/article/the-copper-age---minecraft--bedrock-edition-1-21-111-changelog): previously inspected dye design/accessibility example; version and edition differ. No claim of Mojang's unpublished internal art manual.

### Research sufficiency contract

Do not declare the art system proven merely because corpus metrics or CI are complete.

| Evidence layer | Current evidence | Remaining work |
| --- | --- | --- |
| Provenance/inventory | 3,070 official-client PNG paths; 2,886 statically measured, 96 metadata-bearing and 88 large explicitly inventoried | Analyze metadata frames and large/UV/GUI assets according to their function; audit decode/exclusion reasons |
| Material/role knowledge | 28 directly viewed source examples; family statistics for 7 filename groups | Stratify by category, material, role, scale, tint, overlays and atlas/animation usage; expand representative samples and counterexamples, track direct visual vs numeric coverage separately |
| Practical application | Atlas drawing methods, observed references and exceptions | Draw and review actual rejected mod families, compare before/after in native inventory and placed/rendered contexts |
| Independent evaluation | Fixed exploratory 40 synthetic pairs; simple baseline results | Fresh held-out layout groups and real owner-approved/rejected mod assets; separate artist/reviewer contexts; label per-image accept/reject/unsure with concrete region evidence; freeze decisions, report false acceptance/rejection and uncertainty; repeat with fresh reviewer |
| Release assurance | Existing hash-bound review evidence lock and structural tests | Actual reference provenance and independent review evidence must be verified, not just filled text; no automated artistic verdict yet; in-game evidence separate |

Sufficiency is scoped to the intended art categories, not a demand to have a human look at every utility mask or redundant variant. Before marking a category ready, inventory its exact source assets, inspect a diverse set of examples and exceptions covering each meaningful material/role, evaluate a fresh independent reviewer against actual labeled accept/reject cases, show a corrected real mod example and record the renderer contexts tested. Pre-register sample selection, success criteria, treatment of uncertain outcomes, and confidence limits before the definitive evaluation. Owner-reviewed borderline examples are stronger than obvious shuffled textures; do not tune on the final holdout. A pilot cannot justify a global accuracy or production guarantee. Full video source review and model/UV/animation/GUI coverage are explicitly open. Do not add a heavier production gate before calibration and owner approval.

R5 updates the contradictory vertical-only / blanket palette-variant language in Visual QA using the official verified bark example; no structural/release check is relaxed. All variant evidence remains required.
