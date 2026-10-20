# Wallslam 0.3.0 — Minecraft 1.21.1 / NeoForge

**Fix:** the previous implementation had effectively invisible impact feedback whenever the secondary damage event was rejected by vanilla hurt immunity or another mod. The new version resolves feedback separately from damage and applies a half-second AI stun.

On a successful melee hit against a wall, Wallslam:
- scans only a short 0.50-block probe of real block collision shapes in the knockback direction, during an 11-tick window;
- emits a large cloud, electric sparks, critical particles, and two impact sounds regardless of whether the secondary damage is accepted;
- applies 0.7–3.3 bonus HP of collision damage depending on weapon attack attributes;
- freezes the target mob's AI for **10 ticks / 0.5 seconds** and shows electric sparks above its head;
- restores normal AI automatically after the timer, after a chunk/entity unload and when loading a saved stunned mob;
- leaves naturally NoAI mobs unchanged.

**No Knockback enchantment required.** Only actual melee strikes arm the slam. Target players are excluded, but zombies, pigs and villagers are included.

Build with **Java 21**, **Gradle 9.2.1**, official NeoForge and `gradle clean build`. Expected JAR: `build/libs/wallslam-1.21.1-0.3.0.jar`.

**Manual checks:** test regular melee against full-block stone and dirt walls, with fist and iron sword; test target away from the wall; verify audio and cloud/spark particles on impact, AI pauses about 0.5 seconds, and mob AI resumes. Also test persistent NoAI mobs, dedicated server and reload during stun.

GitHub CI verifies actual compilation and mod packaging. Gameplay is not yet smoke-tested in a real game client.
