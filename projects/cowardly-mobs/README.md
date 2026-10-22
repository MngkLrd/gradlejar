# Cowardly Mobs 0.4.0: Dynamic Spider Jockeys

Minecraft **1.21.1** · **NeoForge 21.1.x** · **Java 21**

A lightweight AI overhaul: morale, Zombie Hunger, tactical skeleton firing lines and dynamic spider jockeys. No new models, textures, blocks, item recipes or GeckoLib dependency.

## What's new: skeletons mount spiders

- An **ordinary vanilla skeleton** can detect an **unoccupied ordinary spider** within 6 blocks.
- 15% roll per encounter by default, with a 30-second cooldown before another roll. This is not a chance rolled every game tick.
- A wounded, frightened skeleton facing a dangerous player has a 35% chance instead. This gives morale a purpose during mount decisions.
- The skeleton approaches using vanilla pathfinding and **normal movement speed**.
- Once near the spider, it performs a real physical jump with approximately 0.39 vertical velocity and modest horizontal velocity. It visibly spends multiple ticks airborne **before** a vanilla `startRiding()` call.
- Short vanilla cloud particles emphasize takeoff and landing; no custom sound files.
- The client-only animation smoothly leans and bobs a previously observed walking skeleton into riding pose after landing. The real jump is independently replicated by vanilla Minecraft movement.
- Dynamic mounting is limited to **normal skeletons and normal spiders** in this version. Cave spiders, wither skeletons and strays aren't included.
- Only free spiders with headroom and out of water are eligible. Pathfinding fails safely when unreachable, the target disappears or the rider/target dies.
- Once mounted, foot-specific retreat/formation AI is suspended so vanilla spider-jockey navigation and bow attacks can resume. When dismounted, the original goals become available again.
- The new mount goal takes precedence over retreat if a chance roll succeeds; otherwise morale and formation work as before.

### Client and server installation

The **gameplay and jump physics run entirely on the server**, so a dedicated server with the JAR also works with vanilla clients (the leap and passenger mounting are visible via vanilla entity updates).

For the **extra lean/bob landing animation**, install the same JAR on **both client and server**. Only the client uses `client/SpiderMountRenderEvents`, subscribed on `Dist.CLIENT`. No client classes are loaded by common/server code.

### Settings

`config/cowardlymobs-jockey-common.toml`:
- `spider_jockey.enabled` = true
- `spider_jockey.mountChance` = 0.15
- `spider_jockey.frightenedMountChance` = 0.35
- `spider_jockey.searchRadius` = 6
- `spider_jockey.retryCooldownTicks` = 600

The existing integrated Zombie Hunger configuration remains in `config/cowardlymobs-common.toml`. Existing datapacks using `#zombiehunger:raw_meat`, `#zombiehunger:cooked_meat`, and `#zombiehunger:rotten_meat` still work.

## Preserved systems

- **Morale System:** dynamic health, player damage/armor and allied mob courage, with fear cooldowns.
- **Zombie Pack Courage:** extra nearby zombies make an injured zombie less likely to retreat.
- **Skeleton Firing Line:** non-riding bow skeletons move laterally into a loose firing line, avoid shooting through teammates, retreat at normal speed.
- **Group Rally:** fleeing zombies/skeletons can regroup with nearby friendly mobs and return to battle.
- **Zombie Hunger:** dropped meat and rotten flesh, 2 HP heal per item, chewing/interruption, contest over drops, neighbor alerts, husks/drowned preferences, sounds and particles.

Do **not** install the standalone Zombie Hunger JAR alongside Cowardly Mobs.

## Build

JDK 21, Gradle 9.2.1:

```bash
cd projects/cowardly-mobs
gradle --no-daemon clean build
```

Expected: `build/libs/cowardlymobs-1.21.1-0.4.0.jar`.

## In-game testing

1. Start a NeoForge 1.21.1 client **with the mod** and a dedicated server **with the mod**.
2. Summon a regular skeleton and regular spider on flat ground during nighttime. To reliably observe, set mountChance to 1.0 and retryCooldownTicks to 60 in the server config; reboot/reload as needed.
3. Watch the skeleton approach, make a real physical jump, then ride the spider. Watch the extra pose lean on the client.
4. Repeat with a client without the mod: vanilla leap and passenger mounting should remain visible, without the extra client pose animation.
5. Verify mounted skeleton continues firing arrows at a player while spider moves. After spider death/dismount, ordinary skeleton morale and formation should resume.
6. Test a spider with an existing passenger: it must be ignored.
7. Test a cave spider, stray and wither skeleton: no dynamic mounting should occur.
8. Test low ceilings and water: the AI must avoid unsuitable mounts.
9. Kill spider while the skeleton is approaching or airborne: no crash, no stuck goal.
10. Test simultaneous skeletons competing for one spider: no passenger duplication.
11. Test chunk unloading/dimension re-entry, multiple nearby mobs and dedicated server headless startup.
12. Retest morale, Group Rally, Spider Jockey prevention of foot formation, and **all Zombie Hunger features**.

**Limitation:** The client landing effect animates whole-body lean/bob, not separately rigged arms/legs. The airborne trajectory comes from real entity physics and the vanilla model animates normally. A bespoke per-bone animation would require a deeper rendering hook and separate further runtime tests. CI checks compilation/JAR integrity, not Minecraft in-game behavior.
