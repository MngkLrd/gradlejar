# OpenMotion 0.1.0

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
- **Redstone ON = fan OFF.** Use a lever to disable it. No block entity or GUI.
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

Output: `build/libs/openmotion-1.21.1-0.1.0.jar`. The repository CI uses `build-profile.json` with Java 21 / Gradle 9.2.1.

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


## 0.1.1 - real 3D models

Replaces the original flat orientable cubes with detailed baked Java Edition block models.
The fan now has an open frame, recessed four-blade rotor, grille, raised motor and status lamp.
The cannon has a physically hollow square barrel, muzzle rim, external copper bands, chassis and top-loading chamber.
The item models reference the full 3D block geometry. Redstone states change status indicator colors.
Models are stored as editable JSON in `assets/openmotion/models/block/` and can be imported in Blockbench.
This release has static geometry: blade rotation animation is NOT implemented yet.
Collision and selection shapes are reduced to fit the new compact mesh.
No original OpenBlocks asset files or code are used.
