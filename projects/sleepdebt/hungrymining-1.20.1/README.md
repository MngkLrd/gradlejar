# Hungry Mining 1.1.0 — Minecraft 1.20.1

Loader: Forge 47.1.0 or newer. Java 17.

Same Hunger Mining 1.1.0 balance across all ports:
- Food 18–20: 108% mining speed, no speed penalty.
- Food 17: 100% mining speed, first speed change after three food points lost.
- Food 0–17: linear interpolation between 30% (food 0) and 100% (food 17).
- No new items, blocks, recipes, or keybinds.
- Survival/Adventure gameplay only; Creative and Spectator are not modified.
- No cross-loader compatibility; install the matching JAR on both client and server.

Build with Java 17 and Gradle 8.14.5:
```sh
gradle clean build
```
Expected JAR: `build/libs/hungrymining-1.20.1-1.1.0.jar`

Check: compare mining identical blocks at 20, 18, 17, 9 and 0 food.
Also verify startup on a dedicated server and in Creative mode.

No recipes: this mod changes a mining event; datapack recipe schema changes in 26.x do not apply.
