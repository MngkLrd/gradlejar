# Cowardly Mobs

Lightweight server-authoritative AI extension for **Minecraft 1.21.1 / NeoForge 21.1.x / Java 21**.

## Gameplay

- **Zombies:** Under 40% health, a zombie periodically evaluates nearby survival players (within 13 blocks). If the player wears solid armor (12+ armor points) or has at least 6 attack damage with their held weapon, it has a 40% chance to retreat for up to 4 seconds. It uses its normal movement speed (pathfinding multiplier 1.0), with a cooldown on further fear checks. No permanent cowardice.
- **Bow skeletons:** Retreat from a target inside 8 blocks, aiming for roughly 11 blocks of space. They maintain their normal movement speed, and move to rejoin another bow skeleton targeting the same player when separated by 8–15 blocks. At very short spacing, they move apart to avoid standing on top of one another. All these moves yield to normal bow fire whenever repositioning finishes.
- **Wounded bow skeletons:** Also receive the same occasional fear/retreat behavior as wounded zombies.
- **Wither skeletons:** Preserve their melee combat AI; bow tactics only activate when holding a vanilla bow.
- No new blocks, items, recipes, textures, packets, or client-side code.

This is tactical behavior, not an exact formation system: obstacles, terrain, existing AI goals, and mob target selection will change where the group stands. Skeletons do not gain any movement-speed bonus or fire-rate bonus.

## Build

From this project folder, run `gradle clean build` with Java 21 and Gradle 9.2.1, or use the parent repository's **Minecraft Mod Build Hub** Actions workflow with `projects/cowardly-mobs` as project_dir.

Expected output: `build/libs/cowardlymobs-1.21.1-0.1.0.jar`.

## Test in a world

1. Confirm the mod loads on a NeoForge 1.21.1 client and a dedicated server.
2. Wear full iron armor, hold an iron sword, summon a zombie, and lower its HP below 40%; some should retreat, not all.
3. Try the same while unarmored and bare-handed; the wounded zombie should keep attacking.
4. Spawn a bow skeleton 4–7 blocks away; it should move backward at its normal speed, then resume arrows.
5. Spawn 3–4 bow skeletons 8–12 blocks apart with a shared player target; watch them collect into a loose firing group without stacking on one coordinate.
6. Confirm skeletons still shoot when not repositioning, and melee wither skeletons keep their vanilla attacks.
7. Verify reload/chunk reentry does not duplicate goals or crash.

All changes are server-side; the mod belongs on the server in multiplayer. Client installation is not required for AI synchronization.

## Known limitations

- No configurable values in 0.1.0 (balance constants live in `ai/AiBalance.java`).
- Formation is opportunistic, not a synchronized squad system.
- The build pipeline verifies compilation and JAR integrity, not gameplay in a running client.
