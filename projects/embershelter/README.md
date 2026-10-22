# Ember Shelter 1.0.0

**NeoForge 1.21.1 / Java 21.** A survival camp builder based on one usable Camp Kit.

## How to use

1. Craft a Camp Kit (leather, string, oak plank).
2. Find a **flat, open 7x7 area**, with firm ground and at least 4 blocks of clearance.
3. Right-click the **top of a block** with the Camp Kit. The center becomes a lit campfire.
4. Fences, tent posts, white-wool shelter, chest, seat and torches appear in stages.
5. In survival, default `consumeMaterials=true` requires the blocks to be present in the main inventory, in addition to the kit. The exact amounts are counted from the blueprint, including the campfire.
6. For a test: `/give @s embershelter:camp_kit` and change the server config below if you want free building.

## Controls / balance

- No automatic trigger on vanilla campfire placement; only the Camp Kit activates.
- If any campsite voxel is occupied (apart from optional replaceable plants or snow), **nothing is built or charged**.
- Builds are server-side, with particle and sound broadcasts, every `buildDelayTicks` ticks.
- Camp Kit is consumed once construction is accepted. Creative mode never pays.
- If a new obstruction is detected while building, remaining steps are abandoned instead of overwriting it. On a still-active server, their reserved building blocks are returned to the player's inventory when online, or dropped at camp if offline.
- Nighttime anti-aggro (Zombie, Skeleton, Spider, including common variants) applies only to finished kit camps with lit campfires, not all vanilla campfires. Bosses, witches, raiders and other mobs unaffected.
- Dedicated server supported. Install on client and server (new item and assets).

## Server config

`<world>/serverconfig/embershelter-server.toml` (created after first launch):

- `consumeMaterials=true`
- `allowReplacingGrass=true`
- `allowReplacingFlowers=false`
- `buildDelayTicks=5`
- `maxActiveCampsPerPlayer=1`
- `protectCampFromWeakMobs=true`
- `protectionRadius=7`
- `allowProtectionOutsideOverworld=false`

## Build

Run `gradle --no-daemon clean build` with Java 21, Gradle 9.2.1. Result: `build/libs/embershelter-1.21.1-1.0.0.jar`.

## Known limits

- Active animation queues are not saved across server restarts. Placed blocks remain; an interrupted build will not resume automatically.
- Anti-aggro prevents eligible mobs from choosing protected players as their targets. It is not a physical force field; neutral wandering mobs can still pass the fence.
- Vanilla `mayInteract` is checked for placement; third-party protection plugins may enforce extra restrictions differently. Test on claim servers before deployment.
- No custom JEI integration is necessary for one standard shaped crafting recipe; JEI and EMI show it through vanilla recipe lookup.
- No direct in-game client test is claimed by this source package.
