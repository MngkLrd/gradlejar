# Potion Timer Bar 0.2.1 - Minecraft 1.20.1

Loader: **forge**; Java **17**. Client-only; install the JAR in the client mods folder, not on the dedicated server.

Exactly one 81 GUI-pixel / 2-pixel-high continuous colored line above the food row. It displays **only while any status effect is active**, whether beneficial or harmful, and uses whichever effect has the **greatest remaining duration**. Infinite effects are counted (full bar). Color matches the selected effect. Modded effects are supported. Hidden-icon effects are not excluded. No segmented icons, extra bottles, network or server logic.

The bar uses GUI-scaled pixels and stays horizontally aligned to vanilla food at all GUI scales, including underwater, when it moves vertically to avoid air icons. Effect duration is tracked from first observation on the client; an already-running effect is initially treated as 100%.

## Build
```sh
gradle --no-daemon clean build
```
Java 17; exact JAR is `build/libs/potiontimerbar-1.20.1-0.2.1.jar`.

## Test
- /effect clear @s -> no bar.
- /effect give @s minecraft:speed 60 -> correct color and decreasing line.
- Add poison 5s -> speed stays selected.
- Add strength 180s -> strength becomes selected.
- Remove all effects -> bar disappears instantly.
- Test infinite effects and effect refresh, underwater, F1, GUI scale and multiplayer.

These projects do not ship a texture asset; all drawing is native GUI fill calls.
