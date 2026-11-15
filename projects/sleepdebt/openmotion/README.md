# OpenMotion 0.3.1

A lightweight, independent NeoForge mod inspired by the lost utility-machine era of Minecraft.
**This code and every bundled texture are original.** No OpenBlocks/OpenModsLib files or assets are included.

## Requirements
- Minecraft **1.21.1**
- NeoForge **21.1.262+** for Minecraft 1.21.1
- Java **21**
- Gradle **9.2.1** (CI build profile)

## Machines

### Air Fan
- Place the fan facing an open corridor. It automatically blows items, creatures, and players up to 5 blocks away.
- **Redstone ON = fan ON.** Use a lever to disable it. No block entity or GUI.
- Solid collision blocks block airflow. Server computes motion every 3 ticks; capped entity checks.

### Item Cannon
- Right-click while holding an item to shoot **one** item. Creative mode keeps the held item.
- Put a vanilla/modded item inventory **directly behind** its nozzle, such as a chest. Right-click with an empty hand to extract and fire 1 item.
- A **rising redstone edge** extracts and launches exactly 1 item from that inventory. Holding power does not continuously drain inventories; switch OFF then ON to fire again.
- Queries NeoForge `Capabilities.ItemHandler.BLOCK` for generic modded containers. It has no internal storage, GUI or block entity.

## Building
From inside this directory:

```bash
gradle --no-daemon clean build
```

Output: `build/libs/openmotion-1.21.1-0.3.1.jar`. The repository CI uses `build-profile.json` with Java 21 / Gradle 9.2.1.

## Test checklist
1. `/give @s openmotion:air_fan` and `/give @s openmotion:item_cannon`; confirm textures and creative Redstone tab.
2. Fan: throw items in front, stand near nozzle, walk behind wall. Check redstone disables it, and leave/reload chunks.
3. Cannon: right-click with a stack, check 1 item removed; repeat in creative, check item keeps stack.
4. Put a chest directly behind the cannon, add items, pulse a lever. Verify one item per edge and no item loss when empty.
5. Verify modded item inventories where block-side item capabilities are exposed.
6. Repeat on a dedicated server with a remote client; confirm no duplication, client-only errors, or crashes.
7. `gradle runClient` optional for local testing.

## Limitations
- Both blocks have horizontal orientations only; they do not rotate after placement.
- Airflow pushes entities, not blocks, liquids or projectiles; uses a narrow centerline and stops at colliding solid blocks.
- Cannon uses 1-item bursts, no trajectory predictor or built-in inventory; container must be immediately behind it.
- Build success does **not** replace actual in-game testing.

## Licence / credits
Released under MIT. Original code, block textures and design by OpenMotion contributors.
The historical OpenBlocks project is an inspiration, not a dependency or source of bundled content. Not an official OpenBlocks port.


## 0.1.1 - first 3D models

Replaces the original flat orientable cubes with detailed baked Java Edition block models.
The fan now has an open frame, recessed four-blade rotor, grille, raised motor and status lamp.
The cannon has a physically hollow square barrel, muzzle rim, external copper bands, chassis and top-loading chamber.
The item models reference the full 3D block geometry. Redstone states change status indicator colors.
Models are stored as editable JSON in `assets/openmotion/models/block/` and can be imported in Blockbench.
This release has static geometry: blade rotation animation is NOT implemented yet.
Collision and selection shapes are reduced to fit the new compact mesh.
No original OpenBlocks asset files or code are used.

## 0.1.2 - visual redesign
- Replaced tiny competing cuboids with clean, purpose-built mechanical silhouettes.
- Air Fan: open housing, 32x32 teal rotor, motor enclosure, clear state indicator.
- Item Cannon: hollow brass muzzle, copper barrel straps, top loading chamber, steel carriage.
- 15 original pixel-art material textures, 32x32. Static baked JSON models (no rotor animation yet).
- Core machine logic unchanged. Can open exported model JSON in Blockbench for editing.

## 0.1.3 - redstone and fan internals

- Air Fan requires redstone (powered ON, unpowered OFF); reconciles on placement, neighbor change and server ticks.
- Four *real* 3D rotor vanes, 3D axle, raised central hub, grille foremost, dark cavity behind the blades.
- Item Cannon extracts one item from rear or top inventory per redstone rising edge; no repeated shots on held signal.
- Native vanilla Container fallback for dispenser; modded sided item capabilities respected.
- Audible failure click on an empty cannon.
- A dispenser or hopper above is treated as a supply inventory, not an automatic trigger. Power the cannon itself.
- Static rotor for now. Actual spinning animation still not implemented.

## 0.2.0 industrial visual refresh
- Air Fan uses a static refrigeration grille: five physical louvers, a recessed duct, a steel frame and a redstone lamp. There is no rotor or spinning animation.
- Item Cannon has a hollow brass muzzle, copper bands, a blue-steel carriage and a top loading chamber.
- The 20 new hand-selected 32x32 pixel material tiles are derived from the approved visual concepts. No OpenBlocks assets are included.
- Gameplay stays identical to 0.1.3; redstone ON powers airflow and cannon fires on rising pulses.
- Real client gameplay still requires testing beyond Gradle compilation.

## 0.2.0 - 3D animated fan and richer material study
- Four client-rendered rotor blades turn while redstone powers the fan, without extra server blockstate updates.
- Inventory model includes static blades, while the world model uses BlockEntityRenderer.
- 20 custom 32x32 material panels with directional lighting, rivets, panel joints, rear vents and metalwork.
- Item Cannon gameplay remains rising-edge firing; continuous hold fire is not yet implemented.
- Gameplay must be tested in a real Minecraft client.

## OpenMotion 0.2.0 - next two machines

### Vacuum Hopper
- Collects dropped items within approximately 3.5 blocks while respecting solid walls.
- Nine persistent slots; auto-sends items to the inventory immediately underneath via NeoForge's item-handler capability.
- Compatible with Item Cannon when placed directly behind or above the cannon (redstone triggers its launch).
- Right-click with an empty main hand to remove the first stored stack.
- Redstone power **pauses** collection and export. Breaking the hopper spills its contents.
- No GUI in this initial release; small capped entity checks for performance.

### Sprinkler
- Static industrial irrigation nozzle with visible water particles.
- Requires a nearby **water source block** within two horizontal blocks and from the same level to two blocks below.
- Automatic when supplied with water; **redstone pauses** irrigation.
- Searches a four-block horizontal radius and up to three blocks below.
- Uses the standard BonemealableBlock contract, so many modded crops work with no direct dependency.
- Processes at most one successful growth operation every five seconds, without consuming water.

### Quick start
`/give @s openmotion:vacuum_hopper` and `/give @s openmotion:sprinkler`.
Crafting is enabled for both. Test on NeoForge 21.1.262, Java 21.

## 0.3.0 Visual polish and collector fixes

- Vacuum Hopper V1+V2: sloped intake, open copper-rimmed funnel, dense collector base and nine persistent slots.
- Sprinkler S1+S2: four large directional nozzles, central blue pipe, heavy industrial base, active/inactive lamp.
- Sixteen purpose-designed 32x32 pixel-art texture tiles, no antialiasing and no original OpenBlocks assets.
- Collector collision shape leaves the intake physically open; raycast ignores the collector itself but respects other walls.
- Sprinkler collisions follow the base, central riser and cross-arm head, not a solid block.
- Removed unused ingredient symbol from air fan shaped recipe.
- Server-authoritative inventory and crop logic preserved; game-world testing is still required.

## 0.3.0 - Water Supply (MVP)

- Place a **Water Pump** on the shore, next to or above an existing water **source** block.
- Connect it with Water Pipes along horizontal or vertical corridors, even underground. Tee and cross junctions connect automatically.
- Use a **Water Valve** inline. Right-click its built-in lever to close/open; neighboring redstone signal also closes the valve. This affects only branches that require passage through it.
- One pump can supply multiple Sprinklers. Sprinklers no longer irrigate from nearby free-standing water; a connected pump and live water line are mandatory.
- **Blue animated inspection windows** indicate live water flow; dry pipes are dark. Blue fluid is a modeled visual indicator, not vanilla flowing water blocks.
- Pump needs a source-water block within 2 horizontally and 2 below; the source is not consumed. Redstone on the pump disables it.
- Pipe network recomputes through a bounded component of up to 384 nodes without loading chunks.
- Existing item machines work unchanged. No tanks, buckets of delivered water or fluid APIs in this MVP; water is a network transport state.

### Creative commands
`/give @s openmotion:water_pump`
`/give @s openmotion:water_pipe 32`
`/give @s openmotion:water_valve`

### Smoke tests
1. Pump at lake; link 12 pipes to Sprinkler, confirm blue flow and crop growth.
2. Break middle pipe: downstream dries, sprinkler stops. Reattach: resumes.
3. Branch into two sprinklers. Insert Valve in one branch; closing it stops only that branch.
4. Put lever on solid block next to valve, redstone ON closes, OFF reopens if manual lever open.
5. Remove water source; all indicators dry within next network refresh.
6. Place water pipes underground and across chunk boundary; only loaded sections process.
7. Restart world, check connections and valve mode persist; repeat on dedicated server.

## OpenMotion 0.3.1 - Farm Automation & Stability

### Air Fan automatic harvest
- Powered Air Fan breaks SAPLINGS and MATURE CROPS along its horizontal output lane.
- Immature crops are not broken. Uses standard block loot/drop logic; plant seeds are not replanted.
- Only one block is harvested per configurable scan, by default every 12 ticks.
- Placing a Vacuum Hopper at the end of the fan's airflow creates a simple item farm.
- Datapacks can add plants to `#openmotion:fan_harvestable` or protect them with `#openmotion:fan_protected`.
- To prevent sapling cutting, set `fan.breakSaplings=false` in the generated config.

### Vacuum Hopper and Item Cannon
- Sneak + right-click Vacuum Hopper **with an item** to set an exact-item pickup filter (item not consumed).
- Sneak + empty-hand right-click to clear the filter; normal empty-hand click extracts its next stored stack.
- Filter and 9-slot inventory persist across game saves and are server-owned.
- Item Cannon base throw speed is now configurable; stronger redstone signals increase range.
- Vacuum collection interval and radius are configurable.

### Water graph & diagnostics
- Water pipes recompute on topology changes; no periodic standalone tick per pipe.
- Pumps and sprinklers periodically reconcile networks. All gameplay logic stays server-side.
- Deterministic graph traversal: a closed valve blocks only paths through that valve.
- Network capacity is configurable (default 4096 loaded nodes). Oversized components fail dry.
- No new chunk loading; pump water sources must be in loaded chunks.
- Right-click pump, pipe or sprinkler with an empty hand for a short status hint.
- Fluid is still a **transport state**, not millibucket storage. Full fluid capabilities remain future work.

### Build and tests
- Minecraft 1.21.1, NeoForge 21.1.262, Java 21, Gradle 9.2.1.
- Run `gradle --no-daemon clean build`; JUnit 5 hydraulic graph unit tests run in CI.
- CI also runs `python3 tools/check_assets.py` to validate textures, models, recipes and blockstates.
- Verify crops, saplings, hopper filtering, branch valves, save/reload and dedicated server in game.
