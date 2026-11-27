# Visual QA and vanilla art standard

This is the canonical MineMods visual procedure under [AGENTS.md](../AGENTS.md). Use it before creating assets and when reviewing changed assets. It is not a claim that automated artistic judgment or graphical gameplay tests already exist.

## Source hierarchy

- Primary artistic reference: [Blockbench Minecraft Style Guide](https://blockbench.net/wiki/guides/minecraft-style-guide/). Read the original. It is community-maintained professional guidance, not a document published by Mojang.
- Supporting first-party art guidance: [Minecraft.net: Try the new Minecraft Textures](https://www.minecraft.net/en-us/article/try-new-minecraft-textures) (Jasper Boerstra interview) and [Caves & Cliffs developer Q&A](https://www.minecraft.net/en-us/article/caves---cliffs--part-i--dev-q-a). Consistent art direction, restrained geometry and pixel-art fundamentals reinforce our approach. Excessive anti-aliasing blurs the tiny sprites; a deliberate treatment in a comparable vanilla asset is more useful than a blanket alpha rule.
- Tool workflow: [Blockbench Overview & Tips](https://blockbench.net/wiki/guides/blockbench-overview-tips/) and Microsoft's official Minecraft Creator guides for [Blockbench](https://learn.microsoft.com/en-us/minecraft/creator/documents/blockbench) and [entity modeling and animation](https://learn.microsoft.com/en-us/minecraft/creator/documents/entitymodelingandanimation). The latter describe Bedrock: borrow modeling, UV, pivot and animation practices, not Bedrock schemas or APIs for Java mods.
- Technical reference: the actual target-version vanilla client assets and loader documentation, e.g. [NeoForge 1.21.1 models](https://docs.neoforged.net/docs/1.21.1/resources/client/models/) and [baked models/display contexts](https://docs.neoforged.net/docs/1.21.1/resources/client/models/bakedmodel/). Resolve model formats, item definitions, render types and resource paths according to the active Java Edition version. Do not copy a later-version schema unchanged.

The following review procedure is **our engineering standard**, derived from those references. It is not a Mojang certification or a reproduction of their internal guidelines.

## Mojang developer guidance: practical application

Jasper Boerstra's [June 2021 developer Q&A](https://www.minecraft.net/en-us/article/caves---cliffs--part-i--dev-q-a) emphasizes pixel-art fundamentals, pixel relationships and color theory. His [texture-update interview](https://www.minecraft.net/en-us/article/try-new-minecraft-textures) warns that excessive anti-aliasing blurs low-resolution art. Apply these as concrete reviews: use hue/value/saturation ramps, deliberate cluster placement, clear silhouettes, and readable material cues; do not generate noise or fake extra colors to satisfy metrics. Mojang's [Bedrock Copper Age changelog](https://www.minecraft.net/en-us/article/the-copper-age---minecraft--bedrock-edition-1-21-111-changelog) also explains the dye redesign in terms of visual consistency and distinguishable shapes for colorblind accessibility. Apply this general art principle to Java items and status icons: do not rely on hue alone to distinguish states. These are first-party statements; the detailed acceptance procedure and numerical warnings below are MineMods policy, not Mojang-mandated thresholds.

## Art production and review

### All assets

1. Choose a comparable vanilla reference from the target game before designing: same material, role, scale and asset type. If the exact target assets are unavailable, record the fallback version. Do not redistribute vanilla assets in the mod or reference reports unnecessarily.
2. Establish silhouette, proportions and a shared palette first; test the shape without detailed shading. Keep related variants recognizable and coherent. Give different materials distinct surface cues rather than reuse a generic noise/gradient algorithm.
3. At actual pixel size, paint meaningful contiguous areas, balance adjacent hue/value/saturation changes, and use selective contrast to describe volume and material. Review accidental jagged edges, repetitive diagonal bands, concentric shading, indiscriminate opposing highlights/shadows, redundant dithering and noise. A small isolated pixel can be intentional. Avoid automatic blur, filtered scaling, and mixed-resolution detail; do not equate every semitransparent pixel with an error.
4. Save lossless PNG and source model where applicable. Compare the rendered result next to vanilla equivalents; do not enlarge a low-detail design simply to add ornament. Preserve original editable sources if already available. Record asset-specific intentional deviations only where useful.

### Items and equipment

- Normal drawn item icons use the 16x16 canvas, with readable silhouette, controlled outline, top-left illumination and material-specific details. Inspect at native size, then with nearest-neighbor enlargement. Larger canvases, armor layers and 3D items are legitimate only when the design/reference warrants them; consistent effective texel density matters more than a universal PNG size.
- For clothing and armor icons, compare actual vanilla leather chestplate, leggings or comparable equipment and the intended material before drawing. Use the base silhouette and outline, then develop the color ramp and place fabric folds, collar, sleeve/cuff, hem or seams where recognizable at native size. Leather, linen and metal should not share identical highlights or the same flat fill. Check worn texture separately from the inventory icon. Preserve simple readable shapes; do not add meaningless detail to every pixel.
- A four-color palette is a review signal for a substantial item, not an automatic failure or a universal minimum. A base fill plus outline and two accents is insufficient if it does not depict material and form. Inspect the distribution and placement of shades, not just their number. Do not solve a flat texture by adding unused colors, random one-pixel flecks, noise or gradients.
- Check inventory/creative slots and tooltip readability, first- and third-person hands, dropped item and item frame where relevant; preserve vanilla-scale grip, angle and size. Compare left/right-hand transforms when supported. A flat generated item is acceptable for a sprite; an intended 3D item must actually render as 3D.
- Inspect layering, tint, transparency and special use states (e.g. charged, damaged, held at mouth). No clipping through the hand or invisible parts, missing-texture checkerboards or unexplained color differences between display contexts.

### Blocks and vegetation

- Work at block-scale 16 units per side; preserve texture scale on cut or rotated geometry. Preview a 3x3 tiled surface before detailed painting and after refinement, including top/side transitions and adjoining vanilla blocks. Check repeated knots, stripes, seams and directional light baked into tile edges.
- Minimize unnecessary cuboids and use purpose-built planes/cutouts for thin foliage, tendrils or small details. Check texel density, face orientation, transparent holes, tint, shading and silhouette from several angles. Check variants and neighbor states (connections, rotations, upper/lower plant halves, waterlogging when applicable); do not infer correct placement from a standalone PNG.
- For Terrain-style vegetation, compare species as a family in actual grass/forest/desert settings. They must have distinguishable silhouettes and coherent material/color treatment; verify biome tint, contact with ground, visibility through crossed planes and ordinary gameplay collision where relevant.

### Entities and animations

- Make body structure recognizable without texture; prefer economical cuboids and appropriate planar features. Set UVs and pivot hierarchy before animating. Match effective texel density on connected parts, preserve face orientation, check mirrored/asymmetric surfaces and seams; apply material shading consistently across body faces.
- Review actual pose, facing, scale, texture alignment, pivot rotation, hitbox interaction and attachment placement (saddle, equipment, held items). Test idle, locomotion and relevant attack/use/death or transition states, including areas revealed during motion. Do not certify animation from a single static pose.
- Non-grid geometry is allowed when supported by the asset format and justified by the visual target; avoid gratuitous tiny voxel detail or stretched faces.

### HUD and GUI

- Use the existing vanilla UI language for the specific target: font, icon grid, panel borders, spacing, slot geometry, hover/disabled state and readable contrast. Do not paste a generic web dashboard into Minecraft. Use the actual intended screen layout, not an image-generation mockup as evidence.
- Check normal and extreme states (empty/full, injured/healed, heat/cold, disabled, stacked effects, long tooltips). Check supported GUI scales, small/large window and relevant inventory/chat overlays; labels must not clip, overlap or cover vanilla HUD controls. Verify independent left/right limb markers and status/tooltip correspondence for anatomy UI, and synchronized values for climate/fatigue HUD when those features are affected.
- A vanilla texture reference is a visual comparator, not permission to override an unrelated interface. Keep state and display logic separate. Do not claim client UX verification from Java layout math or a successful server test alone.

## Evidence and acceptance

| Evidence | What it establishes | What it cannot establish |
| --- | --- | --- |
| Packaged asset validation | Basic local references, resource integrity and supported geometry checks in the actual JAR | Art quality or actual renderer output |
| Fast Visual QA | Measurements, explicit model/PNG checks, previews and advisory review tasks | Accurate silhouette/material judgment, all inherited or code-generated models, in-game appearance |
| Aesthetic review | Actual asset/preview compared to a relevant vanilla reference using the above checklist | Successful runtime rendering or gameplay |
| Client review | Actual target client view of affected contexts, states and animations | Untested contexts or other versions/loaders |

Record PASS only for a review actually performed; use FAIL, NOT_RUN, UNSUPPORTED or N/A as appropriate. These are review outcomes, not new automated QA-level definitions. A structural defect under the existing applicable asset gate blocks a verified release; heuristic warnings require judgment and are not automatic aesthetic failures. A material visual defect should be corrected rather than ignored. If no suitable client is available, deliver honestly labeled development evidence and report exactly which views remain untested; existing release and manual client approval rules still apply. Group screenshots or animation checks by feature rather than launching a client for every PNG. No new blocking subjective-score gate, no required manual user QA during ordinary development.

## Mandatory evidence for each changed asset

Inspect every new or changed asset, including those without lint warnings. Use `art-review-inventory.json` (all inspected textures/models, grouped by content hash), then `redraw-queue.json` for flagged candidates. Review actual native PNG/model or preview against a named vanilla asset and game version, with a specific judgment about shape, palette/material and shading/scale. If preview budget is exhausted, open the source asset directly. A zero-warning item is NOT_RUN until reviewed. Record separate structural lint, aesthetic review and real client rendering results in the normal delivery evidence; do not claim Visual QA PASS without stating the scope. A green workflow only means its executed checks succeeded. A reviewer can accept a genuinely simple icon with a reason, or reject a technically valid flat clothing sprite. Correct material flaws and review the revised hash. A screenshot of an asset thumbnail is not the original PNG or in-game evidence.

## Fast Visual QA implementation

The separate visual workflow runs on relevant texture/model/tool changes and does not start Minecraft or Gradle. It does not impose a new automatic release gate or mutate existing textures. The existing final-JAR asset gate remains active.

- Item/block/entity PNG: exact four-neighbor connected RGB clusters (ignore fully transparent pixels), isolated/small cluster fractions, palette count, equal-color neighbor fraction, adjacent HSV steps, partial alpha and alternating 2x2 patterns. Metrics and heuristic warnings are not aesthetic grades. Animated atlases/metadata are identified; frame-level style warnings and tiling omitted.
- Models: element counts, subpixel geometry and estimated unequal UV texel density for explicit JSON faces with resolvable local textures. Not inherited models, runtime/Blockbench meshes, material semantics or exact geometry certification.
- Real vanilla reference: selected item/block/entity textures from official 1.21.1 client, metadata and client SHA-1 verified, cached metrics only. Matching category/size ranges are contextual examples, not acceptance thresholds. The CI default is 1.21.1; select comparable actual-version references separately if needed. No redistribution of vanilla PNGs.
- Advisory low-complexity signals: for non-animated item textures with at least 32 visible pixels, four or fewer distinct visible RGB colors or a dominant color occupying at least 75% of those pixels prompts contextual review. These are deliberately conservative MineMods heuristics, not artistic pass/fail thresholds. Vanilla reference samples include armor icons. All inspected assets enter the review inventory even if no warning fires; automated aesthetic and client statuses remain NOT_RUN.
- Previews: native PNG, nearest-neighbor enlargement and 3x3 block tile for up to 32 unique textures. Review tiling visually. Transparent materials, thin elements, dithering and nonstandard sizes can be intentional.
- Structural errors: unreadable PNG, malformed model JSON, invalid explicit UV are job failures. Heuristic style warnings are advisory. Review the hash-bound redraw queue; make one targeted correction by default where a real flaw exists and recheck. Do not repaint good art to improve an arbitrary score.
- Full graphical gameplay validation, automatic visual regression, GUI semantic layout validation and animation analysis are not implemented by this linter; protocol bot has no renderer. No numerical threshold can certify vanilla art style.
- Run locally on changed assets: `python -m pip install Pillow==12.0.0`, `python -m tools.qa.visual_lint --paths path/to/texture.png path/to/model.json --out visual-qa`. Inspect final JAR if needed: `python -m tools.qa.visual_lint --jar build/libs/mod.jar --out visual-qa`. Generate reference with `python -m tools.qa.visual_lint --make-vanilla-reference 1.21.1 --out visual-reference.json` and pass `--reference visual-reference.json`.
- Report and previews are retained as MineMods-Visual-QA artifact. Review the actual changed images, not merely the green job status. See [QA guide](QA-V2.md) for release semantics.
