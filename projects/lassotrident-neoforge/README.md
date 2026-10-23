# Lasso Trident - NeoForge 1.21.1

NeoForge port of the Forge 1.21.1 Lasso Trident mod. The vanilla trident and lead are reused, preserving original item components, enchantments and durability.

## Gameplay
- Add a **lead** to a **trident** in an anvil (2 levels).
- Throw the trident against a wall or ceiling to lodge it and make a rope anchor.
- Press and hold **right-click** to climb toward the anchor; release to pause.
- Press right-click again to detach and drop your original trident.
- Automatic drop after **30 seconds**. Maximum range is **48 blocks**.

## Build
- Minecraft: 1.21.1
- NeoForge compile version: 21.1.197
- Java: 21
- Gradle: 9.2.1
- From this folder: `gradle --no-daemon clean build`
- Output: `build/libs/lassotrident-neoforge-1.21.1-0.1.0.jar`

## Test checklist
- Both regular and enchanted tridents work in the anvil.
- Trident retains enchantments and durability on pickup.
- Throw into wall, ceiling, ground and mobs, hold/release/press RMB.
- Test 30-second timeout and 48-block range.
- Loyalty/Riptide cases, multiplayer, logout, dimension changes.
- Verify on dedicated server and client.

## Limitations
- The rope render is visible to the owner.
- Anchors are session-only and not persisted through a full server restart.
- Client gameplay has not been independently tested by CI.
