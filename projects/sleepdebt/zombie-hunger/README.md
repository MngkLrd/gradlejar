# Zombie Hunger (NeoForge 1.21.1)

Server-side zombie food instincts without new items or client-only classes.

## Mechanics

- Dropped **rotten flesh** distracts ordinary zombies even if they have a player target.
- Dropped **raw/cooked meat** is eaten when idle; injured zombies sometimes seek raw meat in combat.
- One item per meal from a stack. Every meal heals **2 HP**, never above max.
- Eating takes 30 ticks and has a 100-tick per-zombie cooldown.
- Hitting a zombie interrupts eating and prevents new food targeting for 100 ticks.
- Several zombies race to a single item. Only the first to finish consumes an item.
- A zombie detecting food sends a local 6-block hunger alert to nearby zombies.
- **Husks** ignore cooked meat, search at close range and rarely abandon combat.
- **Drowned** favor items floating underwater; on land, only nearby rotten flesh distracts them.
- Server plays the vanilla eating sound and sends 6 small item particles on consumption.
- Existing vanilla loot/equipment pickup is untouched.
- Search interval randomized (15-19 ticks), path follow capped at 240 ticks; no chunk-loading scans.
- Config: `config/zombiehunger-common.toml`. Server controls gameplay values.

## Compatibility

Data tags (reloadable via datapack):
- `#zombiehunger:rotten_meat`
- `#zombiehunger:raw_meat`
- `#zombiehunger:cooked_meat`

Raw/cooked tags include vanilla meat and optional `#c:foods/raw_meat` / `#c:foods/cooked_meat`. Add modded food item IDs via normal datapacks. Fish and vegetable foods do not count by default.

## Build

Requires JDK 21 and Gradle 9.2.1. From this directory:
```bash
gradle --no-daemon clean build
```
Expected output: `build/libs/zombiehunger-1.21.1-1.0.0.jar`.
In the build hub, run `Minecraft Mod Build Hub` with `project_dir=projects/zombie-hunger`.

## Test checklist

1. On a local NeoForge 1.21.1 world, summon one ordinary zombie and toss one rotten flesh near it.
2. Let the zombie target you and toss rotten flesh outside melee reach; verify pursuit is interrupted.
3. Attack during the 30-tick chewing period. Verify immediate interruption and attack behavior.
4. Throw a stack of 8 raw beef to a wounded zombie; verify exactly one disappears per meal, health +2 HP.
5. Spawn three zombies competing for one item. Verify no duplication, and loser returns to other tasks.
6. Throw rotten flesh by a group. Nearby zombies should investigate. Run this test at night.
7. Spawn a husk and toss cooked meat (ignored), then raw meat (only if close/eligible).
8. Spawn a drowned in water and toss meat into water, then toss cooked meat on land.
9. Add a modded meat item to a tag via datapack and use `/reload`.
10. Run `gradle runServer`; verify no client-class loading errors.

## Limitations

No custom chewing animation: the vanilla mob stands still and looks at the item. In-world smoke/eat effects only on successful consumption. GitHub Actions compile and inspect JAR but cannot confirm in-game AI behavior without a Minecraft test.
