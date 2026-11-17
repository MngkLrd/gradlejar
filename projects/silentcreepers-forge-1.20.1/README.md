# Silent Creepers v1.0.0

Vanilla creepers actively stalk the player from behind, seek solid cover, retreat when directly stared at, and no longer hiss before explosion. Only creepers are changed; no items, block entities, recipes, renderers or textures.

## Server-side behavior
- Replaces the default `SwellGoal`, avoiding competition with its fuse/attack movement.
- Eyesight: a ~46-degree gaze cone and unobstructed line of sight triggers a tactical retreat.
- Retreat keeps a distance and prefers line-of-sight-blocking objects; 30 tick fear memory.
- When unobserved, pathfind to a point behind the player's look direction, using cover where possible.
- Explodes only after approaching within 2.85 blocks unseen, with vanilla fuse/damage. Flint-and-steel ignition still works.
- Suppresses only `CREEPER_PRIMED` at the entity playSound call. Actual blast, hurt and death effects remain.
- Uses capped pathfinding candidates, 14-tick repathing and server-only AI; clients need the mod only for regular loader compatibility.

## Tests
1. Spawn creeper 12-15 blocks from survival player; look away and observe approach from behind.
2. Look directly at creeper with a clear view: retreats instead of initiating fuse.
3. Put wall or tree between player and creeper: prefers hidden approaches.
4. Turn away at close range: creeper closes in and explodes without hiss, explosion remains audible.
5. Walk into wall/cave: creeper must not get stuck in a CPU-intensive pathfinding loop.
6. Flint-and-steel ignition still forces regular fuse.
7. Test multiplayer and dedicated server; test loaded chunk, despawn, and multiple creepers.

## Build
Use the existing `MngkLrd/gradlejar` Actions build hub and `projects/silentcreepers-*` projects. Each project has a separate build profile.

Run `gradle clean build` from each project folder. The production artifact is under `build/libs/`.
Runtime Java: 17 for 1.20.1, 21 for 1.21.1, 25 for 26.x.
