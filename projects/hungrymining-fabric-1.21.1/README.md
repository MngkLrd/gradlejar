# Hungry Mining 1.1.0 — Fabric 1.21.1

This is the Fabric port of Hungry Mining 1.1.0. It uses a single common Player Mixin
to modify vanilla dig speed on both logical sides. No new items or recipes.

## Gameplay
- Hunger 20, 19, 18: +8% mining speed (no penalty for the first two lost food points).
- Hunger 17: baseline 100% mining speed (first reduction after three points lost).
- Hunger 16 down to 0: smooth linear decrease from baseline to 30%.
- Hunger 0: 30% normal speed (70% slower than vanilla).
- Creative/Spectator are unaffected, and enchanted/modded tool multipliers are preserved.
- Depends on the Fabric Loader only; no Fabric API required.

## Build
Java 21; Gradle 8.14.3; Loom 1.11.8.

```sh
gradle clean build
```

Expected JAR: `build/libs/hungrymining-fabric-1.21.1-1.1.0.jar`

Install the same matching Fabric mod on both client and dedicated server.

## Test checklist
- Compare identical blocks and tools at hunger 20, 18, 17, 9, and 0.
- Compare mining under Efficiency, Haste, Fatigue, swimming and airborne.
- Verify creative mode remains instant and the dedicated server launches.
- Do not install this Fabric build on Forge or NeoForge.
