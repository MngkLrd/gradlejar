# Simple Sorter 0.9.8 (Forge 1.20.1)

Smart Categories 2.1, built for heavily modded inventories such as TerraFirmaCraft and GregTech Modern.

## Runtime

- Minecraft **1.20.1**
- Forge **47.1+** (development target: **47.3.0**)
- Java **17**
- Installed on **client and dedicated server** for networking, gameplay and 3D visuals.
- No hard dependency on TerraFirmaCraft, GregTech, JEI or EMI.

## Player controls

Use the sorting wand (right-click). Shift+right-click opens its settings.
Smart Categories is the default mode for newly created wands; old wands retain their saved mode.
Toggle **Flying Items** in the existing visuals settings. The radius ring and container
highlight effects remain independently toggleable.

Modes:
- **Smart Categories:** canonical exact-item homes, then high-confidence fine-grained category matching.
- **Match Existing:** consolidate only into already occupied homes, no category guessing.
- **Compact Stacks:** merge exact NBT-compatible partial stacks.
- **Player Deposit:** move player inventory items into chests where that same registry item already exists.

## Smart Categories 2.0 rules

1. Snapshot eligible containers before moving anything. All source and target roles
   are frozen during a sorting action, preventing ping-pong.
2. For an item seen in multiple storage blocks, select the canonical home based on
   its category/context, compatible mineral family, matching quantity and stable
   block position tie-break.
3. Every chest has a multi-category profile. Compare the current chest to all
   indexed compatible chests, including when the current chest already qualifies
   as an acceptable destination. Ore must move from an ingot-heavy chest to a
   better raw-ore chest if capacity and score permit it.
4. Exclude each queried stack type from its own chest's context votes; a pile
   of accidentally placed ores must not teach an ingot chest to accept more ore.
5. Evaluate 0..1 semantic compatibility between ore, different forms of dust,
   metal forms, plants, and incompatible categories. Only significant improvements
   trigger category-based moves; exact variants use ID + NBT as identity.
6. Unknown/misc items, empty chests, ambiguous categories and unsupported inventories
   are never guessed into random storage. Item identity always outranks broad categories.
7. Forms are differentiated: tiny/small/normal dust, raw ore, ingot, nugget,
   metal sheet, plate, rod, wire, gear, gems, loose stones, molds, adhesives,
   fruit tree parts, tools and the existing vanilla groups.
8. Category matching uses registry tags first and ID *token boundaries*, not
   arbitrary substrings. Custom category JSONs still override built-in IDs.

An exact-item home uses registry-item identity; the actual item transfer respects
the full ItemStack NBT and the destination IItemHandler insertion rules.
It never forcibly merges unequal NBT stacks.

## Containers and safety

The scanner considers loaded chunks only, up to 160 eligible storage blocks.
By default it handles vanilla chests/barrels/shulker boxes, TFC chests and crates,
and clearly identified general-purpose modded chests/crates/drawers/cabinets.
TFC processing barrels/vessels and industrial machines are **not** automatic targets.

Block datapack tags can override eligibility:
- `simplesorter:sortable_storage` explicitly opts in a storage block
- `simplesorter:unsortable_storage` takes precedence and opts out

Note: Third-party land-claim plugins may enforce their own access policies.
This mod must not be assumed to bypass or support all protection systems; test
protected regions separately before deployment.

All transfers occur server-side through simulation-first extraction/insertion.
If a third-party handler unexpectedly rejects actual insertion, leftover stacks
are returned to the source or dropped at the source rather than destroyed.
Inventory rules and ownership constraints remain the final authority.

## 3D flying items

The server sends up to 24 grouped, successfully completed item transfers per sort
to nearby players. The client renders their original Minecraft models on
0.4-0.75 second eased arcs with short stagger and spinning. No ItemEntity is
spawned, no packet is sent for a blocked move, and max 48 active visual objects
are displayed. Rendering is client-side only.

## Repro tests before release

1. Seed Chest A with bronze/copper ingots. Seed Chest B with tiny kyanite
   dust, other powders, cassiterite and chalcopyrite. Put one kyanite dust into
   Chest A. Smart Sort must return it to Chest B.
2. Seed a mixed chest with bones, candles and native gold. TFC glue with no
   existing home must **stay where it is**, not attach to an unrelated material.
3. Seed separate ingot, raw-ore, dust and tool chests. Mix their contents,
   sort twice. Each item must settle at one canonical home with no second-pass
   ping-pong.
4. Test unknown mod items, non-stackable tools, different NBT on identical
   registry items, TFC size-limited item stacks and full destination chests.
5. Verify machines, sealed vessels, input/output buses, AE2 networks and
   untouched empty containers are excluded.
6. Verify client animation and its toggle on dedicated server + two clients,
   including with and without Iris/Oculus shaders.
7. Test player deposit and compact mode independently; verify no dupes or loss.
8. Compare server item counts/NBT before/after and monitor main-thread latency
   with a large (80+ container) loaded storage wall.

**Known limitations:** Full TerraFirmaGreg in-game regression tests are not part
of the Gradle build. GregTech-specific automation handlers remain excluded until
explicit opt-in. External protections cannot be universally detected via Forge's
generic inventory capability API. Very large modded NBT stacks are not promised
to be compatible with all third-party insertion handlers.

## Build

From `projects/simplesorter-forge-1.20.1`:

```bash
gradle --no-daemon clean build
```

Java: 17. Gradle: 8.14.3. Expected output:
`build/libs/simplesorter-0.9.8-forge.jar`.

Our GitHub **Minecraft Mod Build Hub** CI performs the authoritative Forge
compilation and JAR verification. A successful Gradle build is not equivalent
to a passed in-game regression test.

## 0.9.1 regression focus

- Place five different raw-ore stacks in a chest of bronze and copper ingots.
  With free slots in a neighboring chest of other raw ores, all five must move
  to the raw-ore chest even if the source chest is superficially compatible.
- Dust can share a raw-mineral chest but must prefer dedicated dust storage
  over ingots. Unrelated adhesives/coal should not be routed into random junk.
- Same registry IDs with different NBT must retain their complete tags and must
  never be merged accidentally. The match-existing mode remains available.
- Only actual source/destination blocks are highlighted (max 10).
- 3D flying item scale has increased from 0.31 to 0.54, with the same 48-item
  client render limit.

## 0.9.2: Auto-assign empty chests

Smart Categories now reserves previously empty chests when an item is in an
incompatible mixed chest and there is NO sufficiently appropriate occupied
destination. Related forms share a newly allocated chest (for example
raw ores and dusts), rather than allocating one chest per item type.

An empty chest is *not* chosen if:
- An already filled container has a strong matching profile.
- The current chest contains only the item itself, or is already appropriate.
- The category is unknown/misc or blocked.
- The destination's simulated insert rejects the actual ItemStack.

This prevents items from bouncing across empty chests on repeated sorts.
TFC `tfc:powder/salt` is classified as `salts`, before generic dusts.
A dedicated powder chest can receive salt; a metal ingot chest cannot.
If the salt is misplaced among metal bars and an empty chest is available,
the empty chest becomes its new salt storage.

Pure Java regressions validate empty allocations and anti-oscillation.
Dedicated Forge startup is tested by CI. Full TerraFirmaGreg in-game sorting
and external protection plugins still require manual integration tests.

## 0.9.3: category quality audit + automatic compacting + visible item bursts

**Smart Categories** is now the main combined action: assigns storage from
multi-category contextual scores, merges NBT-identical partial stacks,
and shifts stacks into open slots in each eligible general-purpose chest,
even when all the original categories were already correctly placed.
No second click on Compact Stacks is needed. The old saved
`compact_stacks` preference automatically maps to Smart Categories;
the redundant mode button is removed from the wand UI. Match Existing and
Player Deposit retain their separate purposes.

Compact-pass optimization: a linear slot scan first checks for either
compatible partially filled stacks or holes; already tidy chests are skipped.
The actual merge and pack stages use indexed partial stack lookups rather
than comparing every pair of slots, always transferring with
simulate/extract/insert/rollback semantics through the modded inventory API.

Category-math regression suite checks 529 pairwise compatibility scores
and 2,000 deterministic synthetic chest-routing cases. This is a *routing
rule test*, not a measured in-game classification accuracy rate.
Additional regression cases cover pure and mixed raw minerals, TFC salt,
ingots, glue, false substring matches and no-ping-pong thresholds.
Compatibility candidates are cached per category across all stack/NBT
variants during one sort action, avoiding repeated category-index scans.

Flying Items visual budget: up to 64 distinct grouped inter-container
routes in one bounded clientbound message, sampled from a maximum
of 96 recorded successful transfers. Each transferred item type
creates 1-4 animated models depending on quantity, with up to 160
simultaneous models on the client (previously 48). Models are
0.70-scale (previously 0.54), fly along a slightly higher arc and
remain visible for 17-30 ticks, with staggered start times.
No physical item entities, additional item drops or server render
ticks are created by the visual effect. Internal chest compaction
does not spawn flying visual effects or highlight particles.

Minecraft 1.20.1 Forge 47.x, Java 17.
CI: `gradle --no-daemon clean build`, standalone pure Java routing
regressions, JAR verification and dedicated Forge server smoke test.
Full TerraFirmaGreg runtime validation and graphics compatibility
must still be tested in-game before marking as stable.

## 0.9.4: automatic no-setup Smart Layout (experimental)

The Smart Categories button now automatically considers storage geometry
without assigning labels to the blocks, placing signs or manually organizing
empty chests. A one-action analysis groups adjacent chests into warehouse walls,
automatically chooses vertical (top -> bottom) or horizontal (low -> high
block coordinate) shelves, and creates an ephemeral row layout. Clear, already
stocked rows (at least 3 slots with >=72% confidence) are respected before a
default order is used for all other rows:

1. Raw ores, mineral dusts, salt and gems
2. Finished metals (ingots, sheets, gears, wires)
3. Building materials (stone, blocks, wood)
4. Crafting equipment and components
5. Miscellaneous
6. Plants, trees, food and agriculture

Any number of shelves can participate: fewer than six combine families, and
more than six permit expansion. Separate disconnected storage walls retain
independent auto-layouts. Small unrelated pairs of chests are ignored by the
spatial layer and keep normal Smart Categories behavior.

**Safety:** Category fit still dominates spatial preference. Layout only
breaks semantically close ties, guides first allocation of empty chests and
can consolidate matching categories onto a clearly better shelf. A strict
insertion simulation checks all candidate inventories, including TFC storage.
The layout is calculated once per click and does not run in background,
save block positions or require player setup. No persistent category/chest
mapping is written to the world.

**Constraints for this initial implementation:** Layout uses coordinate
orientation, not a customized front/back view; a horizontal row follows
increasing X or Z. Large complicated rooms may split into regions with no
clear warehouse geometry. A completely full set of incompatible chests
cannot automatically split itself without free slots or an available buffer.
The mod never force-drops items for purely cosmetic shelf ordering.

The CI suite additionally tests six-level and two-level vertical racks,
horizontal X/Z rows, already-seeded reversed layouts, distinct walls,
and sparse-storage fallback. These are deterministic geometry tests, not
a substitute for visual in-game tests with TerraFirmaGreg.

## 0.9.5: Auto-Organize and Automatic Signs / Item Icons (Forge 1.20.1)

**Auto-Organize Warehouse** is an optional wand toggle, OFF by default.
With it enabled, the sorter can treat the existing organization as disposable
and route items to the conventional top-to-bottom / horizontal category layout
based on geometry, without pre-classifying a single chest manually.
The best-effort reorganizer makes up to two passes and at most 160 successful
cross-container operations per click. It requires valid destination capacity;
it never forces cyclic swaps in completely full inventories and never drops
items merely to make a shelf layout look better. If there is no recognized
warehouse wall, the standard Smart Categories algorithm runs instead.

**Auto Labels** has three wand options: OFF / Text / Icons.
Text labels display a localized, wooden-colored category caption. Icons
render a real representative in-game item model plus the localized category
name. Displayed ItemStacks are *client-only cosmetic copies*: they cannot be
picked up or used to duplicate any ores or modded items.
They do not take block space, block right clicks, or require walls for signs.
This lets labels work on vertically stacked double chests.

Load actual vanilla sign items and vanilla item frames from the player's
main inventory into the wand with **Load Labels** in the settings screen.
Storage is NBT-based and capped to 128 signs plus 128 item frames.
Text labels cost one stored sign and icon labels cost one stored frame on the
first creation. Updating a label after a further sort is free. Switching modes
requires the other material type and refunds the prior material to the wand.
When a labelled chest is broken by a player, the spent sign/frame is dropped
at the chest. If the wand breaks, unused supplies drop at the player.
Wand materials are server-authoritative; no items are generated by the GUI.

Labels are saved per dimension using `SavedData`, automatically synchronized
to clients when a chunk becomes watched and resynchronized when labels change.
Each chunk packet is bounded to 128 labels. At most 64 nearby labels are
rendered in view at once. Labels are removed locally when a chunk is unwatched.

Important: these are floating, Minecraft-styled render labels, **not physical
wooden SignBlocks or ItemFrame entities**; the materials pay for their creation.
This avoids blocked chest access, unsupported attachment surfaces and item
duplication. Placement is restricted by normal world/player interaction
checks, but third-party protection plugins should be tested before using
automatic labels on a claimed multiplayer server.

No background item sorting. The large reorganizer and label refresh are
executed only when the wand is activated. This version is a testing release.
Verify both client and dedicated-server behaviour on TerraFirmaGreg before
calling it production-ready.

## 0.9.6: seeded vertical-column preservation and TFC clay/plant fixes

**Regression: ore and plant columns were mixed by 0.9.5.**
The default Smart Layout inferred Y rows across the warehouse and optional
Auto-Organize ignored independently established X/Z vertical columns.
With 3 full ore chests plus a fourth junk chest in column A, and a
top botanical chest with two empties below in column B, earlier sorting
sometimes put ore in B and saplings into metal column C.

The new *WarehouseColumns* engine recognizes vertical columns using their
existing contents. Confident mineral, botanical and metal columns preserve
their purpose even with Auto-Organize enabled: an empty chest *under seeds*
stays a botanical destination, an ore column's lightly mixed fourth chest
is a mineral destination, and filled metal columns cannot capture saplings,
clay or unrelated resources. No manual setup or persistent chest category
assignment is needed. Geometry and container content are analyzed once per
sort action. Unclassified columns may still be used for new categories.

TFC `seeds/<crop>`, fruit pits and saplings are recognized as botanicals.
Clay and kaolin lumps are classified as `clays` (ceramic crafting materials),
independent of ingots and ores. Exact inventory acceptance still uses Forge
simulation, so a full ore column remains full: no item is forcibly swapped
or destroyed to satisfy a spatial ordering rule.

**Recipes:** The original eight-coal-plus-stick shaped recipe was already
in 0.9.5's JAR, but a vanilla recipe can be hidden/removed by TerraFirmaGreg
scripts. 0.9.6 adds a parallel eight-charcoal-plus-stick shaped recipe and a
recipe-book advancement that unlocks both variants when the player carries
a stick. Standard Minecraft recipes are automatically readable by both JEI
and EMI when the modpack retains them. The CI job verifies all three JSON
files exist in the final compiled JAR. No mandatory JEI/EMI dependencies.

This release includes a standalone test with three ore chests, a junk
fourth ore chest, seeds plus two free botanical chests, a misplaced ore,
misplaced saplings, an ingot column and a clay example.
CI includes Forge compilation, source-independent Java routing assertions,
verified recipe/advancement resources and dedicated server boot.
TerraFirmaGreg modpack-specific recipe removal and inventory behavior must
still be checked on a copy of the user's world.

## 0.9.7: attached chest-face labels and render performance

Client label display no longer rotates with the viewer. Each persistent
warehouse label has a **fixed horizontal face direction** chosen once when
created. The actual block's "facing" state takes priority; a non-directional
modded crate gets the side facing the player during first label creation.
Direction persists in dimension SavedData and is synchronized to clients.
Older saved labels without a direction load safely with a NORTH fallback
and get their correct face on the next sorting action.

The text and representative Minecraft/TFC/GregTech ItemStack are rendered
as a small depth-tested decal just outside the **front surface of the chest**,
not a floating billboard. Single-chest text width is capped at 0.78 blocks;
a vanilla double chest gets a single centered label capped at 1.53 blocks.
Labels are vertically centered and long localized captions shrink to fit.
Item models shrink from 0.56 to 0.275 scale, with a small icon above the
text and both kept within the physical chest height. There is no camera
rotation, animated text movement, added item entity or persistent
per-tick update. Direction and chest labels are only updated by sorting
or by normal chunk synchronization.

FPS measures and limits:

- Only labels from the 5 x 5 chunk neighborhood around the camera are
  inspected; the renderer does not iterate through all world SavedData.
- 23-block maximum caption draw distance. Beyond 14 blocks, icon models are
  not rendered. This cuts the highest GPU item-render cost.
- Maximum 48 captions and 18 3D item models in one frame, nearest first.
- Back-facing labels are culled before resolving block entity state.
- All localized Component strings and glyph widths are cached when the
  server updates a chunk, not recomputed per frame.
- All visible text/icon output uses a single endBatch(), with light sampled
  from the chest rather than forced maximum glow.
- FaceLabelDimensions pure Java CI checks 2,000 long/short labels, all scaled
  to fit a single/double chest within the front-face boundary.

These are designed to minimize draw overhead. **No real in-game FPS
benchmark has been run in TerraFirmaGreg**, so test with F3 using labels
OFF, Text, and Icons at the same camera position and graphics settings.
Shader packs can affect decal depth/blending; test both shaderless and with
your usual Iris/Oculus-compatible setup. Labels require the client mod,
not just a server installation.

Networking protocol upgraded to version 4. **Update the mod on both
client and server**; 0.9.6 clients cannot parse the new fixed-facing
label packets. Crafting, categorization, NBT and warehouse sorting
behaviour otherwise follow 0.9.6.

## 0.9.8: visible fixed labels, 64-block radius and one-click stable sorting

Hotfix for tester-reported 0.9.7 regressions:

- **Fixed labels invisible from the front.** Front-face culling compared the
  camera-to-chest vector with the face normal with the wrong sign. Every
  correct frontal view was wrongly discarded. 0.9.8 uses a cardinal-facing
  tested front/back rule. The small fixed chest-face design from 0.9.7
  remains: text and icon stay attached to the box, never billboard toward
  the camera. Legacy labels and non-vanilla crate facing directions are
  recalibrated once on the next sorting action.
- **Sorting radius:** new steps 8, 12, 16, 24, 32, 48, 64 blocks; default
  32. Scanner hard cap raised to 64, and maximum loaded container refs
  per player click raised to 384. The scanner still inspects loaded chunks
  only and measures a 3D sphere around the player, not a chunk-bound area.
- **Auto-Organize uses ONE explicit click for a COMPLETE job.** Instead of
  dropping work after 9 ms per click and re-inferring roles each time, the
  server records a category/warehouse geometry snapshot and processes the
  whole bounded job over subsequent server ticks. Each tick scans at most
  120 slots with an approximately 4-ms budget; compaction runs in a
  separately bounded phase. Additional clicks while sorting do not restart
  work. End-of-job status reports moved items and packed stacks.
- **No-ping-pong routing:** An item already in its confirmed family column
  cannot be moved to another warehouse section for a minor preference.
  Existing column roles stay fixed for the duration of one sort. Newly
  claimed chests reserve a category family and reject incompatible incoming
  stacks. Unclassified items never spill to nearby metal/stick chests.
  Category routing is monotonic, with at most three bounded passes and a
  hard 4096 successful transfer-operation cap.
- Smart Categories without Auto-Organize also respects terminal category
  homes and requires a meaningful improvement before relocating an item.
  InventoryMover preserves NBT and uses Forge item-handler insert/extract
  simulation. This version does not force swaps of full chests.
- No background automatic scans. A task exists only after explicit wand
  activation, runs on the game's server main thread, and cancels when its
  player leaves/dies or the server stops.

Regression CI includes front/back visibility for all four cardinal chest
faces, 6,000 seeded-column no-reverse-move scenarios, geometry / category /
recipe tests, JAR verification and dedicated-server startup.

**Important:** this remains a beta until 14-column TerraFirmaGreg runtime
testing confirms that actual sorting stabilizes after ONE activation.
Try on a copy of the world; measure FPS with Icons/Text/OFF and compare
server MSPT via F3 or a profiler. The per-tick workload is bounded but has
not been benchmarked on the user's modpack.
