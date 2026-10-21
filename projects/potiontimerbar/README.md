# Potion Timer Bar 0.1.0

Tiny, fully client-side status effect timer for Minecraft **1.21.1 / NeoForge**.

## Gameplay

- Draws a handcrafted 11-pixel potion vial and a thin matching-color timer above the food meter.
- Prioritizes harmful effects, then whichever effect expires soonest.
- Supports modded MobEffect instances automatically; does not hardcode potion IDs.
- Refreshes the displayed duration when an active effect is renewed.
- Does not show infinite or hidden-icon effects.
- Avoids air bubbles when underwater and mounts when riding.
- Soft flash during the final three seconds.
- Optional numeric countdown.

## Installation

Put the built JAR in the client `mods/` folder. **Do not install it on the server.**
Only NeoForge **21.1.1 or newer** in the Minecraft 1.21.1 line is required.
No JEI, server plugin, or other mod dependency.

## Config

Generated at `config/potiontimerbar-client.toml`:

- `hud.enabled=true`
- `hud.showCountdown=false`
- `hud.preferHarmful=true`
- `hud.autoAvoidAir=true`
- `hud.verticalOffset=0` (GUI pixels)

## Build

Requires Java 21 and Gradle 9.2.1:

```sh
cd projects/potiontimerbar
gradle --no-daemon clean build
```

Output: `build/libs/potiontimerbar-0.1.0.jar`.

The CI pipeline also publishes prerelease JARs to the GitHub Releases section.

## QA checklist

1. Give yourself 60 seconds of Speed and check color, smooth countdown, and disappearance.
2. Add Poison or Wither: harmful effect should take priority.
3. Renew an existing effect: bar should refill.
4. Enter water: air bubbles must not overlap vial.
5. Mount a horse and test GUI scale 1, 2, 3, 4.
6. Connect to a vanilla or NeoForge server without this mod installed on the server.
7. Run with F1 hidden HUD, modded effects, and no effects.

No gameplay world changes, new items, blocks, recipes, textures, or network messages.

License: MIT.
