# Mob and animal art standard — R12

Mandatory for new or changed living entity visual assets. Baseline for research: Minecraft Java 1.21.1 / NeoForge / Java 21 / official mappings. Art principles carry across versions; renderer, model and resource APIs must match the actual target. Read [common Visual QA](VISUAL-QA.md) first, then this guide, not every unrelated art guide. Use the actual target creature's reference images and model rather than a generic animal generator.

## Research basis

28 directly viewed vanilla entity PNG paths from misode/mcmeta 1.21.1-assets, with archive Git blob IDs matching the official-client R11 corpus ([run 46099337967](https://github.com/MngkLrd/gradlejar/actions/runs/46099337967)). The R12 catalog records the official SHA-256 values for independent CI reverification. Samples include base textures, state variants and overlays; they are not 28 distinct species. They were viewed as actual source atlases, not rendered on live entities. The four version-pinned model-class references below expose structure and APIs, not full source/animation bodies. No claim of exhaustive model or graphical client analysis.

Technical/art references:
- [Blockbench Minecraft Style Guide](https://blockbench.net/wiki/guides/minecraft-style-guide/) — box UV, texel consistency, proportions, deliberate planes and transparency, face-relative shading.
- [Mojang: Meet the Frog](https://www.minecraft.net/en-us/article/meet-frog) — environmental role, variants and animation/behavior design; developer describes adjusting an animal mechanic when real-world assumptions were wrong.
- [WolfModel 1.21.1](https://mappings.dev/1.21.1/net/minecraft/client/model/WolfModel.html), [SalmonModel](https://mappings.dev/1.21.1/net/minecraft/client/model/SalmonModel.html), [FrogModel](https://mappings.dev/1.21.1/net/minecraft/client/model/FrogModel.html), [WardenModel](https://mappings.dev/1.21.1/net/minecraft/client/model/WardenModel.html) — named model parts and version-specific structure. Verify actual geometry, UV and method bodies in the target development environment.
- [NeoForge 1.21.1 layer registration](https://lexxie.dev/neoforge/1.21.1/net/neoforged/neoforge/client/event/EntityRenderersEvent.RegisterLayerDefinitions.html) and [1.21.2 migration primer](https://docs.neoforged.net/primer/docs/1.21.2/) — client layer definition event and later renderer API changes. Do not use 1.21.2+ render-state signatures in 1.21.1. Bedrock model/animation formats are not Java runtime schemas.

## Behavior groups, not anatomy templates

Classify player-facing hostility independently of body plan, spawn category, damage capability, taming or hunting other species. These are our design groups, not Minecraft's internal MobCategory enumeration. Preserve actual AI conditions and version-specific exceptions. A peaceful frog may hunt non-player prey; a neutral goat may ram without being hit; a spider is conditionally hostile (often attacks in darkness); Warden is a hostile threat with detection/anger states, not continuously attacking everything at first sight. The sample classes below are illustrative of typical 1.21.1 behavior toward players, not an exhaustive taxonomy.

| Group | Definition | Design target |
| --- | --- | --- |
| Peaceful | Does not deliberately attack players under normal intended behavior | Readable species, relaxed posture, appropriate defensive/fleeing cues, feeding, breeding or ambient behavior where implemented |
| Neutral | Can attack depending on triggers, territorial conditions, light, anger, taming or other state | Readable transition from normal to alert/attack and back; preserve species and avoid permanent hostile cues if misleading |
| Hostile | Intended as an active danger to players, even if detection, cooldown or special conditions precede attack | Distinct threat silhouette and readable attack windup, direction, effect and recovery consistent with actual mechanics |

Do not impose a universal palette, eye color or facial expression on any group. A realistic predator is not necessarily hostile to players. Classify behavior and anatomy separately in design; no requirement that all peaceful animals look cute or all hostile mobs use red eyes.

## Observed peaceful examples

| Actual PNG(s) | Observations and implications |
| --- | --- |
| entity/cow/cow; entity/pig/pig | Both 64x32, 28 and 12 visible RGB colors. Cow has irregular broad brown/white patches; pig has quieter warm pink planes, muzzle and local dark details. Place coat patches across actual faces, not a random atlas checkerboard. Four legs are not sufficient for species recognition. |
| entity/sheep/sheep; entity/sheep/sheep_fur | 64x32 base 13 colors, wool layer 5. Base face/legs and wool volume are different roles; compose dyed wool and sheared states rather than baking all fleece into skin. |
| entity/chicken | 64x32, 14 colors. Small white body regions, beak/wattle and darker foot cues; distinguish head, wings and legs in geometry. |
| entity/fish/cod; entity/fish/salmon | Both 32x32; 10 vs 38 colors. Cod muted tan/brown with small fins, salmon distinct warmer flank/back markings and greenish head. Preserve species silhouette, mouth, fin placement, back/flank/belly and articulated swimming axis; do not make fish mere recolored cuboids. |
| entity/frog/temperate_frog | 48x48, 18 colors. Warm body planes with distinct eye, head and limb regions. Frog model exposes land/swim motion-related members; implement coherent crouch/jump/swim transitions where required. |

## Observed neutral and conditional examples

| Actual PNG(s) | Observations and implications |
| --- | --- |
| entity/wolf/wolf; wolf_angry; wolf_tame; wolf_collar | 64x32. Base/angry/tame have 26/30/26 colors; collar 6. Coat and overall anatomy persist; facial regions change, collar is sparse separate layer. WolfModel exposes real head, body/upper body and tail parts. Preserve identity, align ears, muzzle, tail and legs, and connect actual anger/taming/pose transitions to state. |
| entity/bee/bee; bee_angry; bee_nectar | 64x64, 17/17/24 colors. Body stripes and wing UV regions remain stable; local eye and nectar changes communicate state. Preserve wing attachment, hover and landing behavior; real-world scale is not a universal rule. |
| entity/goat/goat | 64x64, 15 colors. Cream/brown coat regions and face/horn parts; actual horn silhouette, head rotation and charge behavior matter more than extra pixels. |
| entity/enderman/enderman; enderman_eyes | 64x32, only 3 and 2 colors. Near-black base and sparse eye effect; contrast intentionally concentrated on eyes. Tall long-limbed geometry and state/pose are important. Do not inflate palettes to satisfy an arbitrary score. |
| entity/spider/spider | 64x32, 13 colors. Dark brown head/body and red eye details; leg segmentation and crawl/climb articulation communicate identity. Its conditional hostility is separate from its body shape. |

## Observed hostile examples

| Actual PNG(s) | Observations and implications |
| --- | --- |
| entity/zombie/zombie; entity/skeleton/skeleton | Zombie 64x64/37 colors, skeleton 64x32/11. Skin/clothing blocks vs bone segments; keep body-part anatomy, held item/equipment and attack posture correct. |
| entity/creeper/creeper; creeper_armor | 64x32. Base has 692 visible RGB colors in the official corpus; charged overlay 142. Complex mottling is intentional here, not a recipe for random pixel noise in ordinary animal art. Separate visual effect and actual charge state. |
| entity/blaze; entity/phantom | 64x32 and 64x64, 12 and 19 colors. Blaze has hot bands and dark marks; phantom blue/dark wing/body regions. Flying forms require correct pivot/planes and flight poses; do not reuse quadruped movement. |
| entity/warden/warden; warden_bioluminescent_layer; warden_heart | 128x128, 18/20/5 colors. Dark base with local cyan/bone-like regions; emissive and heart layers are sparse and distinct. WardenModel exposes root, bone, body, head and additional parts. Readability depends on model, actual effect layers and animation timing. No general requirement for emissive textures or huge resolution. |

## Mandatory construction sequence

1. Choose actual species/anatomy and gameplay-role references. Specify dimensions in world units, hitbox, body plan, movement medium, natural variants, age/sex differences only when implemented, and intended behavior. Compare with real-world species anatomy when relevant, but use Minecraft's deliberate stylization, not photorealistic proportions.
2. Build recognizable geometry without texture: primary body, head, limb/fin/wing/tail shape and attachment points. Keep economical cubes/planes; preserve consistent texel scale (typically one texel per model unit at ordinary scale), avoid gratuitous tiny voxel detail, stretched UV faces and needless tiny independent parts. Model size depends on the actual animal, not 16x16 for everything. Use planes for appropriate thin features and intentional transparency.
3. Set actual pivot hierarchy and UV regions before painting: head/neck, shoulders/hips, legs, fins, wing root, tail, jaws or relevant parts. Inspect front/back/left/right/top/bottom and mirrored/asymmetric surfaces. Derive UVs from real registered geometry, not an arbitrary template. Make material and markings follow anatomical surfaces; keep joins continuous, avoid visible atlas bleeding, repeated arbitrary stamps and accidental mirrored identity loss.
4. Paint material in place: fur follows coat direction and broad clusters; scales use modest localized patterns instead of uniform grid; feathers follow wing/body structure; skin, horns, hooves and bone each have their own ramps. For entity faces, top/front usually read lighter than underside/back as Blockbench guidance describes; use local plane shading, not hardcoded scene lighting on every face. Preserve readable eyes/muzzle, appropriate natural countershading and species differences.
5. Compose actual base + variant + layers (fur, collar, saddle, armor, glowing eyes, emissive/effect surfaces). Empty UV pixels and sparse layers are valid when intentional. Check UV alignment, alpha/render type, visibility/tint/conditions and color coherence. Review base and composed model, not just individual atlas.
6. Implement and preview each relevant motion: grounded walk/run, idle, head look, sleep/sit/feeding, jump, swim, flight, climb, charge, attack, hurt and death as applicable. Use correct limb phase, body-relative pivots and transitions; do not animate all parts with a single sine wave. Match apparent displacement to movement speed; avoid foot sliding, detached joints, intersections and wrong rotations. Show model in its normal habitat and at ordinary gameplay distance.
7. For 1.21.1 register entity renderer and layer definitions on logical client, bake the actual model and apply appropriate model setup/pose animation and render layers. Keep AI, damage, movement authority and synchronized state on server/common code; visual motion must follow actual observable/synchronized state, not implement attack damage client-side. Use version-specific APIs; custom animation dependency only when justified and compatible. Do not assume a Blockbench export alone runs in Java.
8. Test actual spawn and registration, dimension/habitat/water compatibility, hitbox, normal and conditional behavior, motion/state transitions, variants, save/reload and mixed states, with actual final JAR and client evidence where supported. For passive creatures do not invent attacks just for tests. Group checks by changed family; don't launch a client per PNG. No model or animation PASS from screenshots of a generated concept, flat UV sheet or server assertion.

## R12 executable evidence

Existing schema-1 records and atlas revision remain compatible. For changed PNGs since R12 adoption tree 73464b88ad7af3089a30fd21244f4688d5c2346e, choose `mob_body` for base/state variant or `mob_layer` for secondary visual layer. Use matching atlas rule and `context_type: model_uv`. A genuinely uncovered anatomy may use the specialized profile with the existing custom-reference fields, not evade it with generic other. Conventional textures/entity PNGs need one of these profiles or a concrete `non_mob_reason` tied to actual render role (boats, projectiles, paintings, other non-living assets). Nonstandard paths still require semantic classification. Use actual reference for behavior/anatomy; behavior can intentionally differ from comparator.

In addition to existing per-PNG fields and distinct profile findings, require `mob`:
- `entity_id`: actual namespaced registry identifier (the primary reviewed entity where a texture is shared).
- `behavior`: peaceful, neutral or hostile; `behavior_conditions`: actual triggers and exceptions toward players.
- `role`: base or variant for mob_body, overlay for mob_layer.
- `model_binding`: actual renderer/model/resource relationship; `motion_review`: concrete preview observations of applicable states and pivots, including layer alignment.
- `client_status`: PASS, NOT_RUN or UNSUPPORTED; `client_evidence`: actual client evidence or explicit reason. A self-attestation cannot substitute for separate release/client QA.
- For variant and overlay, `base_texture`: repository-relative actual PNG path and SHA-256, within project or declared shared roots. If intentionally reusing vanilla, use catalog `vanilla_reference` ID (no minecraft prefix), its official SHA-256 and `reuse_reason`. Do not claim original base as an overlay.

The existing pre-Gradle Art Review Lock validates required fields, profile/role and behavior values, exact base-file bytes or known vanilla hash, and model_uv context. It does not infer actual Java registration, verify that model bindings were executed, discover all registered mobs, prove animation quality or certify graphical rendering. No added normal build jobs or forced migration of unchanged historical art. Use existing QA and final-JAR checks. No independent reviewer accuracy claim.

## Scope still open

This pilot covers 17 creature examples across 28 PNG paths (including layers and variants), not every vanilla mob and not every life stage. More direct research is needed for horses/riders, bears, cats, reptiles, bats, aquatic variants, slime deformation, custom body plans, baby models, layered equipment, animation code and actual in-client render. Don't generalize static texture observations as confirmed animation behavior.

## Existing common entity requirements

- Make body structure recognizable without texture; prefer economical cuboids and appropriate planar features. Set UVs and pivot hierarchy before animating. Match effective texel density on connected parts, preserve face orientation, check mirrored/asymmetric surfaces and seams; apply material shading consistently across body faces.
- Review actual pose, facing, scale, texture alignment, pivot rotation, hitbox interaction and attachment placement (saddle, equipment, held items). Test idle, locomotion and relevant attack/use/death or transition states, including areas revealed during motion. Do not certify animation from a single static pose.
- Non-grid geometry is allowed when supported by the asset format and justified by the visual target; avoid gratuitous tiny voxel detail or stretched faces.

