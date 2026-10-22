# PearlPath

Minecraft 1.21.1 / NeoForge / Java 21. Client-only.

When an Ender Pearl is held in either hand, a cyan-to-amethyst dashed, PUBG-like world-space arc previews the expected flight. In first person the visible arc originates on the crosshair (without drawing the near-camera segment). The line stops at the first block or pickable-entity collision and marks the landing point with a tiny cross. No server installation is needed.

## Build
```bash
gradle --no-daemon clean build
```
JAR: `build/libs/pearlpath-1.0.1.jar`.

## Notes
Real pearls have launch randomness (vanilla inaccuracy=1), so the prediction is the nominal path rather than a guarantee of the exact randomized landing position. Nether portals and unusual modded collision handling may affect final teleport destination.
