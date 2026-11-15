# TraumaCraft 0.2.0

Minecraft **1.21.1** · **NeoForge 21.1.262** · **Java 21** · Gradle **8.14.3**

Seven local body health pools and injury mechanics, plus a vanilla-style inventory health panel.

## Features

- 7 localized HP pools (total 440) with bleeding, fractures, location-based hits and leg slowdown
- Vanilla inventory expands **to the right** with an anatomy mannequin, status bars and mouse-over injury details
- Pick up a medical item with the mouse and **left/right-click an anatomy zone** to treat that exact zone
- Server validates the actual carried stack and body-part bounds, then consumes item on success
- Semi-transparent lower-left anatomy HUD
- Independent **TraumaCraft | Medicine** Creative inventory tab
- Optional **Just Enough Items (JEI)** plugin: standard crafting recipes + per-item usage info pages; JEI is not a required dependency

## Items
- Bandage: stops bleeding on one zone
- Splint: fixes a fracture
- Medkit: restores 25 HP on one zone

Right-click with item in hand to auto-select an appropriate injured zone.

## Controls

1. Open survival inventory (E).
2. Pick up Bandage / Splint / Medkit from a vanilla inventory slot with the mouse cursor.
3. Click a body part or its HP bar on the new right panel.
4. No applicable trauma? Treatment is rejected without consuming the item.

The normal crafting slots, armor slots, inventory and recipe book remain vanilla.

## Tests

Operator commands:
- `/trauma`
- `/trauma hit left_leg 50`
- `/trauma fracture right_leg`
- `/trauma bleed left_arm 2`
- `/trauma reset`

1. Run command to injure; inspect updated bar / HUD.
2. Carry a medical item over matching zone; click; check count and updated HP.
3. Close/open inventory, relog and respawn; verify sync.
4. Remove JEI entirely; TraumaCraft should still start.
5. Install JEI 19.x for Minecraft 1.21.1 NeoForge; check info and recipes in JEI.
6. Check dedicated server no client classes loaded.
7. Test inventory with GUI Scale 2-4 and recipe book.

## Build
`gradle --no-daemon clean build`

Expected: `build/libs/traumacraft-0.2.0.jar`

## Scope limits
- Local HP supplements vanilla hearts in this phase.
- Impact point for melee is approximated from attack ray.
- This build is a gameplay prototype, not a final medical overhaul.
- Other GUI overhaul mods may conflict with the InventoryScreen replacement.
