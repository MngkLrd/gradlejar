# Simple Sorter 0.9.1 (Forge 1.20.1)

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
`build/libs/simplesorter-0.9.1-forge.jar`.

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
