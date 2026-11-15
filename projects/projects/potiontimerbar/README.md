# Potion Timer Bar 0.2.0

A minimal, **client-only** Alchemy Strip for Minecraft 1.21.1 (NeoForge).

## What it does

- Renders **one unsegmented, 2px-high line**, exactly **81 GUI pixels wide**, aligned with the complete vanilla hunger row.
- Appears **only when at least one status effect is active**; disappears immediately when none remain.
- If there are multiple active effects, it uses **the one that will expire last** (greatest remaining duration). No positive/negative priority, rarity filter, or duration limit.
- Infinite-duration effects also count: if selected they keep the bar filled until removed.
- Fill color follows the selected effect, including effects from other mods.
- No bottle icons, text, numeric countdown, section marks, or other HUD clutter.
- The fraction decreases based on the selected effect's locally observed starting duration and refills if the effect is reapplied/extended.
- Renders just above hunger by default, using GUI-scaled coordinates; shifts **vertically only** when air bubbles appear. X and width never shift.
- No server installation, networking, recipes, items or other dependencies.

## Setup

Copy `potiontimerbar-0.2.0.jar` into the **client** `mods/` folder. Requires Minecraft 1.21.1, NeoForge 21.1.1+ in the 21.1 line and Java 21. Use with any vanilla or modded multiplayer server.

Config at `config/potiontimerbar-client.toml`:
- `hud.enabled=true`
- `hud.autoAvoidAir=true`
- `hud.verticalOffset=0` (GUI pixels; negative moves up)

Old `showCountdown` and `preferHarmful` options are intentionally removed: the style and latest-expiring-effect rule are fixed.

## Build

Java 21, Gradle 9.2.1:
```sh
cd projects/potiontimerbar
gradle --no-daemon clean build
```
Output: `build/libs/potiontimerbar-0.2.0.jar`. Dedicated GitHub Actions workflow publishes a prerelease build only after compilation and JAR validation succeed.

## In-game QA

1. No status effects: no bar at all.
2. One Speed effect (60s): a single line above food, starting full and shrinking.
3. Speed 60s + Poison 10s: choose Speed, even though Poison is harmful.
4. Four effects with 10s, 30s, 90s and 180s remaining: select the **180s effect**. On expiry, fall back to the longest still active.
5. Reapply/extend the selected effect: the fraction refills.
6. Infinite effect: single full-width colored line until the effect ends.
7. Underwater: no overlap with bubbles and no horizontal movement.
8. GUI scales 1-4, different screen ratios, survival/creative, singleplayer and server connection.
9. Hide GUI (F1) or spectator mode: no custom bar.

**Known limitation:** Minecraft only provides remaining ticks to the client; when joining a server with an effect already running, the bar uses the first observed remaining duration as 100%. Real client gameplay testing is still required after CI compilation.

License: MIT.
