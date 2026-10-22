# Hungry Mining

Small survival gameplay mod for **Minecraft 1.21.1 / NeoForge / Java 21**.

Digging speed is modified by the vanilla food meter. There are no new items,
textures, keybinds, HUD elements, network packets or external mod dependencies.

| Food points (0–20) | Mining speed | Effect |
| --- | --- | --- |
| 0–4 | 60% | -40% |
| 5–9 | 80% | -20% |
| 10–15 | 100% | Normal |
| 16–19 | 105% | +5% |
| 20 | 108% | +8% |

One food point is half a hunger drumstick. Saturation does not affect the multiplier.

## Build
Use JDK 21 and Gradle 9.2.1:

```sh
gradle clean build
```

Output: `build/libs/hungrymining-1.21.1-1.0.0.jar`

This standalone Gradle project also builds through the repository's
Minecraft Mod Build Hub (project directory: `projects/hungrymining`).

## How it works
The common NeoForge `PlayerEvent.BreakSpeed` event adjusts the existing mining
speed, preserving other mods' earlier adjustments. Creative and Spectator
are unaffected. The handler is safe to run on both logical sides so that
client mining animation and server block-breaking calculations agree.

Install on both the client and dedicated server for multiplayer.

## Quick test
1. Start a survival world; mine the same stone block type with the same pickaxe.
2. Compare food=20 with food=12 (8% bonus vs baseline).
3. Compare food=8 (-20%) and food=2 (-40%).
4. Repeat with an Efficiency-enchanted pickaxe and under Haste/Fatigue.
5. Verify Creative instant mining is not slowed.
6. Confirm single-player and a dedicated server with the mod on both ends.

Known limitation: the small positive bonus may be difficult to perceive on
blocks which already break in one tick. Very low food levels can also
be influenced by Minecraft's built-in conditions and other mods.
