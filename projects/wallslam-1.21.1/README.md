# Wallslam 0.3.3 — Minecraft 1.21.1 / NeoForge

**Fix:** the previous implementation had effectively invisible impact feedback whenever the secondary damage event was rejected by vanilla hurt immunity or another mod. The new version resolves feedback separately from damage and applies a half-second AI stun.

On a successful melee hit against a wall, Wallslam:
- scans only a short 0.50-block probe of real block collision shapes in the knockback direction, during an 11-tick window;
- emits a large cloud, electric sparks, critical particles, and two impact sounds regardless of whether the secondary damage is accepted;
- applies 0.7–3.3 bonus HP of collision damage depending on weapon attack attributes;
- interrupts movement and targeting for **10 ticks / 0.5 seconds** and shows electric sparks above its head;
- does not modify NoAI or persist stun state; a mob resumes moving normally after the timer;
- leaves naturally NoAI mobs unchanged.

**No Knockback enchantment required.** Only actual melee strikes arm the slam. Target players are excluded, but zombies, pigs and villagers are included.

Build with **Java 21**, **Gradle 9.2.1**, official NeoForge and `gradle clean build`. Expected JAR: `build/libs/wallslam-1.21.1-0.3.3.jar`.

**Manual checks:** test regular melee against full-block stone and dirt walls, with fist and iron sword; test target away from the wall; verify audio and cloud/spark particles on impact, AI pauses about 0.5 seconds, and mob AI resumes. Also test persistent NoAI mobs, dedicated server and reload during stun.

GitHub CI verifies actual compilation and mod packaging. Gameplay is not yet smoke-tested in a real game client.

## 0.3.1 — permanent-stun hotfix

Wallslam 0.3.0 used `setNoAi(true)`, which could leave mobs permanently frozen. This version **never disables the NoAI flag**, tracks a 10-tick deadline in server time, stops navigation/targeting temporarily, and clears transient state when a mob leaves its level. It also automatically restores AI for old affected mobs that carry the persistent `WallslamManagedStun` marker on chunk/world load. Existing intentionally NoAI mobs without that marker stay untouched.

## 0.3.2 — wider loader compatibility

Minimum targeted NeoForge: **21.1.100** (previously 21.1.262). This project is compiled against that lower version instead of restricting players to the newer pinned build. CI build verifies compilation and packaging; runtime behavior on older loaders should still be checked in-game. No gameplay changes.

## 0.3.3 — event-order fix and diagnostics

Melee hits are prepared on LivingIncomingDamageEvent and only confirmed on LivingDamageEvent.Pre. Knockback strength is now available during the knockback event. The wall probe sweeps 0.65 blocks from the entity bounding box. Up to 24 status transitions are logged as `Wallslam diagnostic` so a live test can distinguish incoming, confirmed, no-wall and impact stages. Gameplay testing is still required.
