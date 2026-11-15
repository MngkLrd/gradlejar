# WallSlam 1.0.2 — Minecraft 1.20.1 / Forge

An accurate port of the NeoForge 1.0.2 wall-impact mechanic to Forge 1.20.1.
No hard dependency on NeoForge, APIs, libraries, client rendering mods or the latest Forge.

## Mechanics
- A melee attack against a living mob arms a knockback event.
- During the next 11 server ticks, a forward 0.65-block sweep detects real block collision shapes.
- On collision, plays vanilla impact audio and explosion/cloud/spark/critical particles,
  plus material-colored block particles.
- Delivers a separate capped 0.7–3.3 HP collision hit using
  `wallslam:collision` damage type tagged `minecraft:bypasses_cooldown`.
- Applies 20 ticks (1 second) of stun: stops mob navigation and horizontal velocity,
  keeps its target, and cancels new direct melee attacks while stunned.
- Never sets NoAI; old WallSlam 0.3.0 NoAI markers are repaired safely.
- Excludes player victims, but supports any vanilla or modded Mob.
- All gameplay checks are server-side. No global entity searches.

## Configuration
Forge SERVER config per world: `serverconfig/wallslam-server.toml`.
- `damageMultiplier = 1.0` (0.0–3.0)
- `stunTicks = 20` (0–40; existing saves may retain older values)
- `particleMultiplier = 1.0` (0.0–2.0)
- `debug = false` (true writes impact stages to latest.log)

## Version and compatibility
- Minecraft: **1.20.1**
- Loader: **Forge**, metadata minimum **47.1.0+**, NOT pinned to newest 47.3+/47.4+.
- Build dependency: **Forge 47.1.106** because the existing legacy ModDevGradle
  coordinates could not resolve 47.1.0. This affects compilation only; metadata
  accepts Forge 47.1.0 and later. Runtime compatibility below 47.1.106
  requires an actual game launch to confirm.
- Java **17**; Gradle **8.14.5**.
- Build: `gradle clean build`, expected output:
  `build/libs/wallslam-1.20.1-1.0.2.jar`.

## Verification after installing
Replace old WallSlam jars; launch with Forge 47.1.0, 47.1.106 or another
compatible 47.x loader. Slam a zombie against a stone wall with an iron sword:
one explosion effect, custom wall particles, extra health loss, and a 1-second
stun; the zombie must resume chasing after stun. Also test an open area (no
wall impact), slabs/fences, mobs with NoAI, different weapons, dedicated server
and other combat mods.

CI verifies Gradle compile and assembled JAR; it does not simulate gameplay.
