# Everybody Is Watching 0.2.0

Minecraft 1.21.1, Java 21, NeoForge / Fabric. Mojang official mappings.
No blocks, items, new models, assets, screen effects or mandatory client-only code.

## Gameplay

A village encounter unfolds in two distinct phases:

1. **GATHERING (7-10 s)**: villagers begin approaching with a randomized 0-3 s departure spread. They use normal vanilla pathfinding, not teleporting. They stop near a loose 5-7 block ring around the moving player. More loaded villagers who come into range during gathering can join, up to 24.
2. **STARING (10-14 s)**: all participants freeze at once and lock head/body rotation to the player. They continue tracking the player while standing. In 25% of events one villager watches for 3-5 extra seconds.

The arrival stage is designed to feel gradual and unnatural, while the final stare stays synchronous. Obstacles, doors, missing paths, and the limited gathering time can prevent individual villagers from reaching the player; these villagers stare from wherever they end up. No teleportation or block manipulation.

- Natural trigger: 65% roll after a 3-8 second wait on village entry (if 4+ eligible villagers are nearby).
- Re-entry requires 6 seconds outside village detection and respects a 4-minute per-player cooldown.
- Event detection radius: 48 blocks; participant cap: 24; one active event per dimension.
- Works from server-side hooks on both loader versions. No dependencies other than Fabric API on Fabric.
- Villager movement speed is temporarily modified only once they stop. Normal AI remains on to keep head/body rotation network-visible.
- Event cleanup restores speed and AI navigation on timeout, unload, reset, disconnection and server shutdown. Crash-recovery tag retained.

## Commands (cheats/operator level 2)

- `/watching trigger`: full sequence (gathering then staring). Works outside villages and ignores cooldown, requires 1+ eligible villagers within 48 blocks.
- `/watching stare`: skip the gathering phase to test the direct look animation.
- `/watching status`: village detection, phase, spectator count, remaining phase time and cooldown.
- `/watching reset`: cancel this player's event and reset entry/cooldown state.

## Test

1. Create an open village in Minecraft 1.21.1 with 4-12 villagers separated by several blocks.
2. Run `/watching trigger`; watch the staggered approach and use `/watching status` to confirm GATHERING.
3. Once the approach timer expires, villagers should simultaneously stop and turn to track you; confirm STARING.
4. Walk around to verify persistent turning for 10-14 seconds; optionally note a delayed final villager.
5. Run `/watching stare` to test just the synchronized gaze.
6. Test inside and outside villages, unreachable participants, closed trade screens, chunk unload/reload, server reload, and multiplayer.

## Build

Requires Java 21.
- NeoForge: Gradle 9.2.1; `gradle clean build`.
- Fabric: Gradle 8.14.3; `gradle clean build`.
- Output: `build/libs/everybodyiswatching-<loader>-1.21.1-0.2.0.jar`.

## Known limitations

- Paths must be physically accessible; villagers do not teleport or phase through obstacles.
- A strongly obstructed villager may fail to approach, but will still stare from its current position.
- One event per dimension at a time. Commands require a player context.
- Gradle compilation does not replace real in-game testing.
