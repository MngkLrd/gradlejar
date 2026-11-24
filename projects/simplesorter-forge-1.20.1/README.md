# Simple Sorter 0.10.2 (Forge 1.20.1)

## 0.10.1–0.10.2 internal milestones: global adaptive planning and spatial layout

Smart Categories takes one fresh snapshot of every eligible inventory in
the selected radius. No independent 16/32-chest sorting windows. Candidate
layouts consider real slot capacity, semantic groups, material and form
boundaries, and legal placements. Small warehouses can co-locate related
groups; with spare capacity the planner may separate materials/forms.
Unknown items use exact identity, not invented similarity. Original
placement is always a valid fallback and a worse score is not accepted.

Spatial candidates group actual chest footprints into vertical columns,
top to bottom, including double-chest midpoints. Same material spread over
many columns and distant storage is penalized; related groups have a weaker
proximity preference. Irregular horizontal warehouses work too. The
semantic impurity and fragmentation score is evaluated together with
spatial quality; no mandatory warehouse shape.

Execution decomposes the single global plan into independent paths and
cycles, checking live sources, target validity and touched inventories.
The hidden buffer is emptied within each transaction and never persists
between ticks. A failed component does not cancel unrelated components.
The planning pass and an indivisible large cycle can exceed a tick budget;
performance, full audit and cancellation behavior are scheduled for
0.10.4. Label/purity presentation and re-sort stability remain 0.10.3.
No new dependencies or changes to wand recipes/UI.


## 0.10.0 internal milestone: semantic item classification

`SemanticItemProfile` records group, material, form, purpose, confidence,
source evidence, legacy family and movement permissions. It exposes a
hierarchy and a bounded affinity score for the upcoming adaptive planner.
Generic Forge/common/vanilla tags and registry path conventions support
unlisted materials and mods. Conflicting material tags are marked
ambiguous instead of choosing a random material. Unknown items retain
their exact registry identity and namespace, without claiming semantic
similarity. User categories are evaluated before built-ins, while lock
rules always take precedence. No mandatory compatibility dependencies.

The active Smart Categories snapshot now retains semantic profiles.
0.10.0 still uses legacy-family placement as a transitional planner;
global adaptive assignment, spatial columns and reorganization stability
are subsequent milestones (0.10.1–0.10.4). No player-facing UI, recipe,
scanner, or transactional storage changes are intended in this milestone.


## Core radius and classification fixes

Fresh loaded-chunk discovery on every activation; the actual square
preview and equal vertical reach. Standard lockable storage uses the
lock check rather than the eight-block menu reach check; custom
containers retain their own access policy. Scan reports show logical
inventories, physical blocks, empty storage, slot counts, unloaded chunks
and explicit rejection reasons. No chunks are force-loaded.

The 32-inventory planning window now covers a 22-chest warehouse in one
operation; larger warehouses use overlapping windows. Empty storage
can be seeded with a full semantic family rather than requiring a
single-stack affinity improvement. Fuel, ammunition, spawn eggs,
currency, ignition, explosives and botanical parts are recognized,
and known/custom categories are no longer lost in the second classifier.
No new mod dependencies. Actual modpack testing remains required.


## Inventory compatibility fix

Double-chest capabilities are measured against the sum of the two physical
Container sizes. A shared combined view is selected once; two genuinely
separate half views are joined only when their sizes prove that shape.
No fixed 27/54 assumption, mod namespace or dependency. This addresses
18/36-slot chests and other sizes as well as vanilla. Ambiguous shapes
are skipped rather than guessed. Both physical blocks must remain connected
and unchanged for the scheduled transaction.

The transient buffer now touches only changed slots, rechecks the live
source immediately before extraction and rejects an unexpectedly occupied
destination. This prevents a stale/aliased second read from creating
a phantom copy during rollback. The normal inventory validity, capacity
and item NBT checks remain enabled.

Pure-Java tests cover shared 36/54/other-sized views, split halves,
ambiguous wrappers and an alias-backed simulated warehouse. In-game tests
with real modded storage are still required before marking this stable.

Smart Categories 2.1, built for heavily modded inventories such as TerraFirmaCraft and GregTech Modern.

## Runtime

- Minecraft **1.20.1**
- Forge **47.1+** (development target: **47.3.0**)
- Java **17**
- Installed on **client and dedicated server** for networking, gameplay and 3D visuals.
- No hard dependency on TerraFirmaCraft, GregTech, JEI or EMI.

## Virtual wand buffer (0.9.16)

**0.9.16 fix:** The 0.9.15 greedy family-first planner could reserve a
restricted destination slot for the wrong item and then reject all 12
chests as one batch, leaving 0 cross-chest transfers. The layout now
starts from every exact original occupied slot, then uses safe, strictly
improving one-to-one category swaps and moves into available slots.
The original arrangement remains a valid fallback. A separate pure-Java
regression exercises 12 fully packed mixed chests, 15 families and
item-specific slot restrictions. Rejected batches now report a reason.



In **Smart Categories**, the wand now plans a complete re-layout of each
connected warehouse batch (up to 16 chests per synchronous transaction).
After confirming that the original slots support extraction and restoration,
it temporarily removes every eligible stack into a **server-only memory buffer**
and refills the same physical chests by semantic family. No empty chests
or dedicated chest for every category are required. Smaller families may
share a chest and different NBT stacks are kept intact.

The buffer is not saved inside the wand, does not have a GUI, cannot be
accessed by the player, and never remains populated across ticks. The
warehouse still needs enough real slots for its contents. Chests with
restricted, non-extractable or non-restorable slots are skipped; every
batch is planned and checked before mutation. Failed commits attempt
rollback, with visible emergency drops rather than silent deletion if a
third-party handler violates its simulated behavior. Two overlapping
players cannot sort the same chests simultaneously.

This is a **test build** for TerraFirmaCraft / TerraFirmaGreg: first make a
world backup, test on a small 4-12 chest warehouse, and inspect the final
"skipped restricted chests" and "rejected buffer batches" diagnostics.
Other sorting modes retain their previous logic.

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
`build/libs/simplesorter-0.9.14-forge.jar`.

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

## 0.9.9: reliably visible TFC crate labels, not rotating billboards

Fixes two independent client-rendering failures inherited from 0.9.7/0.9.8:

1. **Wrong glyph plane**: text was rotated 180 degrees into the chest.
   The text's front surface (+Z after negative X/Y scaling) now points
   OUT of the block for NORTH/SOUTH/EAST/WEST. New pure Java regression
   tests verify the normals of all four rotations.
2. **Missed network snapshots**: ChunkWatch can send label data before
   the client finishes loading the world. If unchanged, a later sort
   would not send the label chunk again. The client now requests a
   rate-limited 5x5 loaded-chunk refresh once after joining, and the
   server always re-sends scanned label chunks after sorting even if
   their saved contents are unchanged.

TerraFirmaCraft's crates / modded chests may not expose the same front
metadata as vanilla chests; an old world label could have been assigned
to the E/W side of a crate just because the sorting player was far
to one side of a 14-column warehouse. The client no longer trusts that
guessed direction for rendering. It selects from the actual exposed
N/S/E/W block faces, skipping solid walls and adjacent chests. All
rendered surfaces are **world-aligned 90-degree planes**, not continuously
rotating floating nametags. Single and double chest text width remains
bounded; double chests prefer the two long exterior faces.

At nighttime, captions now have a soft light-map floor so dark TFC
storerooms cannot make them visually black. Representative 3D items
still use normal world lighting and are limited to the existing client
LOD (48 text decals, 18 models, 23/14 block distances).

**Label resource requirement has not been removed**. Set Labels: Text
or Icons in the sorting wand GUI, place signs or item frames in your
player inventory, press Load Labels, then sort once. First-time label
creation consumes one sign or frame per chest; subsequent updates are
free. If none are available, the warning now appears in regular chat
instead of a temporary actionbar message overwritten by the
"sorting completed" status. Labels: OFF still removes existing marks
and refunds their backing materials.

Server/Client network protocol has advanced to version 6; both sides
must update. 0.9.9 retains the 0.9.8 64-block radius, 384-chest
scanning limit, tick-sliced one-click warehouse reorganization and
no-ping-pong routing. No changes to inventory stack semantics, world
loading, or category definitions in this release.

Forge CI compiles the production JAR, tests cardinal surfaces and
letter-front rotations, tests sorting invariants and recipes, verifies
packaged resources and boots a dedicated Minecraft server. These are
automated tests; graphical FPS and shader compatibility require
in-game TerraFirmaGreg QA.

## 0.9.10: original-style readable chest text, 10-block-only decals and real local item icons

Reworks the old bulky 0.9.5 floating text into a smaller fixed-surface label,
without camera-facing billboard rotation. Captions use the original-style
Minecraft Font renderer (depth-tested NORMAL mode), with world-aligned
NORTH/SOUTH/EAST/WEST planar orientations. A label may occupy any EXPOSED
block face that the player can approach. These are static physical planes,
never quaternions based on the camera direction. No text/entity spawning.

**Text restored on protruding TerraFirmaCraft chests:** The v0.9.7–0.9.9
caption was placed almost flush (typically only 0.526 blocks from the chest
center) and occluded by crate model geometry, while a more distant 3D icon
remained visible. This version moves the text to a safe 0.702 blocks from
the TFC block center, with a smaller 0.015 scale versus the original 0.020
floating label, and shrinks long names to fit the chest width.

**Hard distance cutoff:** Text and icons appear only when within strictly
LESS THAN 10 blocks of the camera. At 10 blocks and beyond, they disappear.
Only the closest 3x3 chunk region is searched. Max 36 world-space text decals
and 12 item models in one frame. No always-on server scanning.

**No text through walls:** Every candidate must be on an exposed physical
chest surface, facing the viewer. A per-label client collision ray from the
camera to the caption location must not hit a different block first.
Normal font depth rendering remains enabled. This also prevents erroneous
rendering of captions through rows of crates and solid building walls.

**Real content icons:** Icon selection is now PER CONTAINER, not shared
across the whole warehouse family. A bone-meal icon can never be copied to
another chest that lacks bone meal simply because both use a "botanical"
category label. A chest with no matching representative item gets TEXT
ONLY until a real item of that family is stored inside. The shared category
caption is allowed for an empty crate inheriting a clearly seeded column.

The fixed protocol and saved metadata retain normal 0.9.9 compatibility;
the client and server should both be updated so server-side icon selection
matches the new renderer. Existing labels refresh on the next sort and
re-sync on normal login. No changes to 0.9.8+ smart-sort routing,
one-click tick-sliced organization, recipes, item transfer rules or 64-block
sorting radius.

CI tests enforce 10-block cutoff, short/long name scaling, geometry and
all previous category safety checks. Dedicated Forge server smoke-tests
verify startup but cannot replace actual graphical tests with TerraFirmaGreg
or Oculus/Iris-compatible shader packs. This is a test version, not a
claim of measured client FPS.

## 0.9.11: strict item semantics, no arbitrary category intermixing

This update addresses the user-reported TerraFirmaGreg failure where sticks
were packed together with ingots, a water potion appeared among conglomerate
bricks, a pineapple bush and ladders, and a separate chest contained a knife,
stones, pickaxe head, clay, ceramic vessel, smithing template, mortar and
kaolinite clay.

**Root causes found in 0.9.10:**
- The Auto-Organize "home" profile used only the broad six shelf families:
  ore / finished material / building / crafting / misc / plants. Clay, tools,
  molds, armor and many other objects all counted as one vague "crafting"
  destination even though they are not interchangeable.
- A "mostly matching" chest counted as compatible when as few as 74% of
  its occupied slots matched, so new ingots could still be deposited beside
  sticks and other foreign items.
- Items classified as `misc` were skipped during reorganization, leaving
  sticks, water bottles, uncommon TFC objects in inappropriate storage forever.

**New strict acceptance policy:** Shelf/column proximity guides which
category gets an EMPTY chest; it never grants permission to mix unrelated
families in an occupied chest. Every occupied slot must belong to the SAME
fine category as the incoming item (100%, not 74% or 99%). A 99-ingot /
1-stick chest is NOT an ingot destination. An unclaimed empty chest may be
allocated to a new category; a claimed empty chest refuses other categories.
The same hard gate applies in ordinary Smart Categories and Auto-Organize.

**Detailed categories:** sticks/twigs; water containers (including water
potions read from ItemStack NBT); other potions; construction bricks/stairs;
botanical bushes/seeds; finished metal forms; mechanical components;
tool heads vs complete tools; smithing templates; clay and kaolinite;
mortar/binders; ceramic vessels; rocks; powders; and other recognized forms.
Unrecognized items may use a miscellaneous space but can never qualify as
a finished-metal/brick/tool home solely from vague name proximity. Names
and data are read from registry IDs, item tags and actual stack NBT, not
translated tooltip strings or manually maintained TFC compatibility lists.

**Mixed container cleanup:** A mixed source can move misplaced items to
already PURE matching containers or newly reserved EMPTY containers.
A completely full warehouse with no compatible space cannot be repaired
without an item buffer; the sorter leaves those items instead of dropping
or replacing them. Category/claim decisions are frozen per explicit sorting
action so completed pure storage is a terminal stable state. The work remains
time-sliced on the server; no continuous background scan.

This is a beta with focused deterministic regression tests for TFC/GregTech
mixes. Forge compilation and dedicated server boot do NOT substitute for
runtime testing against the actual TerraFirmaGreg pack and its chest APIs.

## 0.9.12 Forge: per-chest categories, distributed ore consolidation, loose rocks

**Fixes combined from player tests:**

1. Six vertical stacks, four chests in each, separated by a one-block gap.
   The first column contains three ore chests and has an almost empty third
   ore chest. A sixth column nevertheless receives additional ore. The
   previous planner marked every pure ore chest as a terminal home and would
   not consolidate the sixth-column material into the established first.
2. Some physical columns contain unrelated categories at different heights,
   which must remain legal. The earlier "WarehouseColumns" assumed that an
   entire X/Z column belonged exclusively to a single broad family.
3. TFC loose chalk and other stone pebbles were incorrectly grouped under
   "Ores & Minerals", and sticks beside bronze ingots could make the chest
   show the unhelpful label "Crafting & Tools".

**What changed:**
- Normal Smart Categories (even with Auto-Organize OFF) now uses the
  server-tick, one-click warehouse session. The same safe strict-family
  sorting handles both Smart Categories and optional Auto-Organize.
- A category is assigned to a **chest**, never to an entire vertical stack.
  Tools, bronze ingots, clay and sticks can each occupy a separate chest in
  one column; the mod does not reserve whole columns or require a 6x4 layout.
- Each family is assigned a canonical X/Z cluster by the amount of already
  existing **pure** inventory evidence. A strong first ore column outranks
  an isolated sixth-column ore chest. Sources outside the anchor consolidate
  into pure or empty compatible storage at the anchor, including its almost
  empty third chest. If no compatible room is available, nothing is dropped
  or force-swapped. Mixed chests evacuate non-dominant items.
- Spatial preferences are *soft*, per family. A column can simultaneously
  anchor tools, metal, clay, seed and building categories, each in a different
  chest. Existing pure source chests at their anchor are stable and don't
  ping-pong on later clicks.
- Loose TFC stones (including `tfc:rock/loose/chalk`) are classified
  strictly as `stones`, not `raw_ores`. Actual ore remains `raw_ores`.
  The old visual fallback also no longer treats loose rocks as ore.
- Chest labels are based on **this specific chest's** strict contents.
  There is a dedicated label for Tools, Crafting Materials, Ores, Ingots,
  Loose Stones, Clay, Pottery, Tool Heads, and more. A bronze-and-sticks
  chest is visibly labeled **Mixed Contents** until it is cleaned up,
  never a generic "Crafting & Tools". Empty chests have no misleading label.
- One movement attempt never mixes unrelated families; Forge simulation
  preserves NBT and respects TFC slot/capacity rules. The family's initial
  anchor stays fixed for one action. Long searches resume from the last
  target on a later server tick rather than scanning 384 chests in one tick.
  Changed inventories are revalidated before transfer.

Important: The stable category algorithm now runs for *ordinary* Smart
Categories too. Auto-Organize remains optional; it is no longer a prerequisite
for cleaning up scattered ores and mixed chests. Player/warehouse counts and
layout are inferred from actual loaded inventories.

**Client/server:** protocol 8. Both sides require the 0.9.12 JAR so
the client can resolve all newly localized per-chest category captions.
Majority ore stacks from a mixed, outlying chest are also eligible for
direct return to the established ore column; they are never redirected
to a third, unrelated empty column.

**Testing:** CI includes six-columns x four chests with 1-block gaps,
a nearly free third ore chest and surplus sixth-column ore; multiple
categories in one column; distinct TFC loose rocks; and per-chest labels.
Forge Java 17 compile, resource checks, and dedicated-server startup still
run. These tests cannot replace a full graphical TerraFirmaGreg session.

## 0.9.13: TFC/TFG/GTCEu mineral forms, ore-grade ID fallback, honest mixed status

Regression from in-game TFG screenshots: small/tiny/full dust piles, poor
and rich raw ore lumps and other ore-processing materials were stored in the
same chest but classified as `raw_ores` vs `raw_powders`, so its label read
"Mixed Contents". Poor Goethite could be classified as miscellaneous if
the item ID lacked the literal ore token; unrelated stone pebbles appeared
among mineral ores.

- `mineral_resources` is now one compatible chest family for actual ore
  pieces and normal/small/tiny mineral dusts, TFC ore-grade forms and crushed
  or washed ore. Multiple mineral forms from DIFFERENT mods may coexist
  without a false mixed label; their exact item/NBT stack identities remain
  distinct, and only real identical stacks are merged.
- TFC/TFG/GTCEu poor/normal/rich grade registry paths are recognized,
  including plain grade-prefixed names. Optional item-tag fallbacks recognize
  TFC ore-pieces and conventional Forge/C ore/raw-material tags even when
  the item path does not reveal its origin. These are ID/tag checks, not
  a requirement that TFC or GT be installed.
- Loose rock pebbles (chalk, marble, etc.) stay `stones`; metal ingots,
  finished tools, sticks, clay/kaolinite, salt, flux, mortar, organic bone
  meal and crafting powders are not admitted into the mineral resource
  family. Broad six-row positions never override strict family gating.
- If mixed chests remain after the one-click session, the player gets an
  explicit count rather than the false "storage already stable" message.
  Unclassified inventory slots are also counted for further modpack QA.
  Without a pure compatible destination, an empty chest, and permission to
  insert into its slots, the mod cannot safely separate all items; it will
  never destroy items or force TFC inventory rules.
- New regressions cover poor Goethite, rich Garnierite, Lignite, Greg
  tiny/small/full dusts, impure and purified materials, rock pebbles,
  clay, salt, flux and exact strict family admission. All 6-column
  consolidation and chest label tests run on the unified mineral family.

**Important:** tests assert classification and pure-Java storage policy, not
a running TerraFirmaGreg client with all its custom item capabilities. Use
a backup of the world and check whether the user-visible "mixed" count
falls to zero after one sort. If not, note the count and any remaining item
IDs for further classifier refinements.

## 0.9.14: one-click recovery of ALL-MIXED warehouses (0 transfers fix)

Root-cause fix for the screenshot report: `0 items moved; 7619 packed;
13 mixed chests; 59 unclassified slots`. 0.9.13 admitted a new stack ONLY
to an already pure or entirely empty chest. If every chest was mixed, the
algorithm could not move a single stack despite hundreds of free slots.
"Packed" previously counted ITEMS rearranged WITHIN the same inventory,
not items placed in more appropriate chests. This is why the user saw
visibly random assortments after "successful" sorting.

The sorter now:
- Scans all selected loaded containers once on explicit wand use;
- Builds a temporary stable **per-chest family ownership plan** from actual
  content (not a preconfigured row/column template); preserves existing
  pure chests and favors category concentrations;
- Lets an assigned family home receive its OWN family while that chest is
  still initially mixed, then evacuates incompatible items into THEIR
  assigned homes, in bounded server-tick transactions. Thus it can sort
  two mixed chests with no empty containers;
- A family is never moved back after reaching its assigned home. A
  temporary receiver may be mixed while its foreign items are leaving,
  but the transfer policy is monotonic, not repeated unconstrained swaps;
- Uses Forge item insertion/extraction SIMULATION and actual NBT-safe
  stacks, never deletes or reconstructs an ItemStack;
- Reports incomplete assignment when the warehouse has MORE distinct
  families than available physical chests. One strict family per chest
  cannot separate 20 unrelated families into only 13 chests, regardless
  of available individual empty slots.

Wand status/UI fixes:
- The wand no longer enters cooldown if no warehouse action was accepted;
  busy/cooldown/no-container actions now produce feedback.
- While one-click sorting is running, a progress indication appears
  every ~4 seconds. A safety watchdog stops a hung job instead of leaving
  it active forever.
- On completion, **inter-chest transfers** and **intra-chest compaction**
  are shown as separate values, along with unresolved mixed chests,
  unknown slots and unassigned item families.
- The first eight actual registry IDs of unclassified items, with counts,
  are printed to the user chat. This helps fix future TFC/GT item
  classifiers using grounded data rather than screenshots guesses.
- If another player's inventory changed during the multi-tick job,
  destination handlers are re-read and transfer transaction simulation
  remains authoritative. No TFC/GregTech hard dependency was introduced.

Added pure Java executable **logical movement** regressions: two initially
mixed chests become pure with no empty chest, 13 initially mixed 54-slot
chests exchange foreign stacks, all item counts stay unchanged, sorting
the already-organized chests again causes zero movement, and categories
without enough whole-chest homes are reported honestly.

For validation test on a COPY of your world, run Smart Categories once,
wait until "completed" appears, and check the INTER-CHEST count and
unknown registry IDs. Do NOT infer successful warehouse organization from
the packing counter. Real modded chest insert restrictions and FPS are
still only verifiable in a TerraFirmaGreg game client.
