# Bearback 0.2.0 - Saddlebags (NeoForge 1.21.1)

Adds independently craftable **Bear Saddlebags** for tamed black and vanilla polar bears. All previous saddle, riding, attack, defensive behavior and black-bear visuals are retained.

## Craft
```text
Leather   .     Leather
String   Chest  String
Leather   .     Leather
```

## Use
1. Tame an adult bear with raw cod or salmon.
2. Right click it holding the crafted saddlebags to fit two visible side pouches.
3. While mounted, press the vanilla inventory key E: instead of your inventory, the bear's two-row (18-slot) chest opens.
4. From the ground, crouch + right-click the bear with an empty hand to open the cargo.
5. Cargo survives saving / reloading / chunk unloads. At death, contents and the saddlebags themselves fall as item entities. No extra UI mod and no hard dependency.

## Safety / Compatibility
- Server validates bear identity, ownership, bag equipment and distance.
- Vanilla `ChestMenu.twoRows`, no custom menu network synchronization or screen.
- Player inventories are unaffected when not riding a bear with bags.
- A weak-key cache stores a weak bear reference to prevent world-memory retention.
- Existing worlds/bears from 0.1.2 remain compatible. Packs must be equipped separately.
- Java 21; NeoForge build version 21.1.262, runtime range [21.1,) in metadata.
- Build: `gradle clean build` -> `build/libs/bearback-1.21.1-0.2.0.jar`.
- Test dedicated server, multiplayer permissions, full slots, Shift-click, reload, player death vs bear death, GUI open from riding E and ground.
