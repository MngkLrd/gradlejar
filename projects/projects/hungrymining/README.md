# Hungry Mining 1.1.0

Small survival gameplay mod for **Minecraft 1.21.1 / NeoForge / Java 21**.

Digging speed changes smoothly based on the vanilla food meter. No new items,
textures, keybinds, HUD elements, network packets, or external mod dependencies.

**Balance:** The first two lost food points cause no change. Once three
food points have been lost (20 to 17), the mining bonus goes away. Every
further lost food point slows digging progressively.

| Food points (0–20) | Mining speed | Compared to normal |
| --- | --- | --- |
| 18–20 | 108% | +8% |
| 17 | 100% | normal |
| 15 | 91.8% | -8.2% |
| 12 | 79.4% | -20.6% |
| 9 | 67.1% | -32.9% |
| 6 | 54.7% | -45.3% |
| 3 | 42.4% | -57.6% |
| 0 | 30% | -70% |

A food point is half a hunger drumstick. Hunger/saturation are not the
same: the speed modifier depends on food level only, not hidden saturation.

At 0 food, mining is **half as fast as in Hungry Mining 1.0.0** (30% vs 60%).
The transition from 17 to 0 is linear; every food point has its own speed.

## Build
Use JDK 21 and Gradle 9.2.1:

```sh
gradle clean build
```

Output: `build/libs/hungrymining-1.21.1-1.1.0.jar`

Repository build hub project: `projects/hungrymining`.

## How it works
Common NeoForge `PlayerEvent.BreakSpeed` scales the current mining speed,
preserving previously applied multipliers from other mods. Creative and
Spectator are unaffected. The handler runs on both logical sides, so
client prediction and server calculations can agree.

Install on **both client and dedicated server** for multiplayer.

## Quick test
1. Mine the same stone block with the same pickaxe at food=20, 19, 18.
   Speed should remain identical.
2. At food=17 verify the first reduction (108% to 100%).
3. At food=12 verify about 79.4% of unmodified speed.
4. At food=0 verify 30% of unmodified speed (half of v1.0 at zero food).
5. Repeat with Efficiency, Haste and Mining Fatigue.
6. Verify Creative instant mining is unchanged.
7. Verify dedicated server and client with the same mod JAR.

Known limitation: small differences may be imperceptible on blocks already
mined within one game tick. Other mods may also modify dig speed.
