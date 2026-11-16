# Hungry Mining 1.1.0 — Fabric 1.20.1

Fabric port of Hungry Mining 1.1.0 using one common Player Mixin.
No new items, blocks, menus or recipes.

## Gameplay
- Hunger 18–20: +8% mining speed.
- Hunger 17: 100% speed, the first slowdown after losing three food points.
- Hunger 0–17: linear decrease to 30% speed at zero hunger.
- Creative/Spectator are unaffected.
- Other vanilla tool, enchantment and effect speed modifiers are preserved.
- Requires Fabric Loader; no Fabric API needed.

## Build
Gradle runs on **Java 21** as required by Fabric Loom 1.11.8. The compiler
uses `--release 17`, so the output works with **Java 17** at runtime in
Minecraft 1.20.1. Use Gradle 8.14.3.

```sh
gradle clean build
```

Expected: `build/libs/hungrymining-fabric-1.20.1-1.1.0.jar`.

Install the JAR on both client and dedicated server for multiplayer.

## Testing
- Mine matching blocks at food 20, 18, 17, 9 and 0.
- Test Efficiency, Haste, Fatigue, underwater, airborne and Creative.
- Test dedicated server startup with Fabric Loader.
