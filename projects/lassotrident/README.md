# Lasso Trident (Forge 1.21.1)

A small grappling hook modification to the **vanilla trident**. The attachment is a regular lead, not a new weapon.

## Gameplay
1. Place a vanilla trident in the left anvil slot and a lead in the right slot. Spend 2 XP levels to attach it. Original enchantments and damage remain.
2. Throw the resulting trident as usual. When it embeds in a solid wall/ceiling, it creates a tether anchor.
3. Press and **hold RMB** with an empty hand to pull toward it. Release RMB to stop moving.
4. Press RMB once more to detach. The trident drops as an item, preserving its NBT/components.
5. If not detached within **30 seconds**, it automatically drops to the ground. Maximum operating range: 48 blocks.

## Design notes
- Does not add any blocks, recipes, or item textures. It preserves existing vanilla trident models.
- Only block impacts create anchors. Mob hits behave as vanilla.
- A player may have one active anchor at a time.
- The rope is a lightweight client-side 3D line shown to its owner.
- Server controls anchor validity, countdown and player velocity; all client input is validated.
- Loyalty tridents have their return suspended while anchored, and keep the enchantment upon recovery.
- The trident item is dropped when released, not automatically returned to the inventory.
- Persistent anchors across server restarts are not yet supported. The tether is an in-memory session feature.
- When climbing, avoid holding a different item with a RMB action.

## Build
Java 21, Gradle 8.8, Forge 52.1.0 / Minecraft 1.21.1:
```sh
gradle --no-daemon clean build
```
Expected output: `build/libs/lassotrident-forge-1.21.1-0.1.0.jar`

## Smoke checks
- Anvil preserves damaged, named, enchanted tridents and consumes exactly one lead.
- Throw at wall, floor, ceiling and a mob; only block hits anchor.
- Hold/release/press RMB, watch 30 second timeout.
- Verify dropped trident retains enchantments and is not duplicated.
- Try Loyalty, multiplayer with two players, dimension change, logout and reload.
- Run client and dedicated Forge server.
