# Everybody Is Watching 0.1.1

Minecraft 1.21.1 (Java 21), NeoForge / Fabric, Mojang official mappings.
No items, blocks or custom assets. Server-side game logic (both client and server must install the mod).

## Natural event

- Checks loaded players once every second.
- Detects a village with ServerLevel#isVillage (occupied points of interest).
- After first village entry, waits 3–8 seconds, then makes **one 65% roll** if at least four eligible villagers are within 48 blocks.
- Villagers freeze for 10–14 seconds; their heads track the player, while their original body yaw is preserved.
- In 25% of events, one villager watches another 3–5 seconds.
- After each entry roll, a 4-minute cooldown applies; leaving village detection for 6 seconds rearms entry detection.
- No global chat messages, sounds, extra textures or screen effects.
- Natural events are independent from command trigger events.

## Commands (operator permission 2 / singleplayer cheats)

- `/watching trigger`: force event instantly on 1+ eligible villagers within 48 blocks. Works outside villages and ignores normal cooldown.
- `/watching status`: shows village detection, nearby eligible villagers, pending delay and cooldown.
- `/watching reset`: reset tracking and cooldown for the issuing player. If the player's event is active, restore affected villagers.

## Test

1. Create a 1.21.1 creative world with cheats enabled and find a village.
2. Spawn four villagers with `/summon minecraft:villager ~ ~ ~` if desired.
3. Run `/watching trigger`. All eligible villagers in radius should stop and look at you.
4. Move while they are frozen: heads should follow within vanilla head rotation limits.
5. Wait up to 10 seconds: they should resume. Occasionally one keeps watching longer.
6. Run `/watching status`; leave the village at least 6 sec, return, and observe a chance-based event.
7. Try an open trading interface and `/watching trigger` from another operator; trading closes for affected villagers.
8. Test on a dedicated server with two players, leaving chunks, and after reconnect.

## Build

- JDK 21.
- NeoForge: `gradle clean build` (Gradle 9.2.1).
- Fabric: `gradle clean build` (Gradle 8.14.3).
- Output: `build/libs/everybodyiswatching-<loader>-1.21.1-0.1.1.jar`.

## Known limitations

- Vanilla model limits head turning to 85 degrees relative to the body.
- Detects real villages via occupied POIs; manually spawned groups outside villages work via command.
- One event per dimension at a time.
- Crash-recovery through temporary entity tags.
- Commands require a player execution context, not server console.
