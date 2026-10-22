# Cowardly Mobs 0.2.0

Server-side Minecraft 1.21.1 / NeoForge mod, built using Java 21. **Zombie Hunger 1.0.0 is now integrated directly into Cowardly Mobs.** Install only the new Cowardly Mobs JAR, not both separate mods. The existing Zombie Hunger project and JAR remain independent for users who still need them.

## Original Cowardly Mobs AI
- Wounded zombies and skeletons (under 40% HP) sometimes retreat from survival players with strong armor or weapons.
- Bow skeletons move at normal speed, try to stay about 11 blocks away, spread out and loosely regroup around a common player target.
- Vanilla ranged and melee attacks, including wither skeleton melee behavior, remain.

## Integrated Zombie Hunger AI (source mechanics preserved)
- Ordinary zombies prioritize dropped rotten flesh, which distracts them even during pursuit.
- Raw/cooked meat can be eaten while idle; wounded zombies can seek raw meat while fighting if conditions allow.
- Each meal takes 30 ticks, consumes **one** item and restores **2 HP** (one heart), capped at max HP.
- 100-tick cooldown between meals. Being hit by a player interrupts eating and suppresses food-seeking for 100 ticks.
- Several zombies can compete for the same item. Only the winner consumes it; no duplication.
- Hungry zombies send a nearby 6-block food alert.
- Husks ignore cooked meat, detect food only nearby and are less likely to abandon combat.
- Drowned prefer floating/underwater meat and only chase nearby rotten flesh on land.
- Vanilla eating sound and 6 small item particles play after a completed meal.
- Existing vanilla equipment pickup remains untouched. Scanning is periodic, localized and bounded.

## AI interaction rules
- One server-side registration handler attaches goals only once per entity, including when chunks reload.
- Priority 1: wounded retreat; priority 2: food seeking for zombies / formation for skeletons.
- Fear can interrupt an ongoing eating attempt. Following retreat, a zombie can look for food again.
- There is no artificial movement speed multiplier.

## Config
NeoForge COMMON config: `config/cowardlymobs-common.toml` in the `zombie_hunger` section. Original Zombie Hunger config parameters and defaults are preserved.

## Datapack compatibility
Legacy tags remain available with exactly the same identifiers:
- `#zombiehunger:rotten_meat`
- `#zombiehunger:raw_meat`
- `#zombiehunger:cooked_meat`

The raw/cooked tags include vanilla items and optional `#c:foods/raw_meat` / `#c:foods/cooked_meat`. Existing datapacks can add modded meat using these legacy tags without changes. Fish and vegetables are ignored by default.

## Build
From `projects/cowardly-mobs` with JDK 21 and Gradle 9.2.1:

```bash
gradle --no-daemon clean build
```

Expected JAR: `build/libs/cowardlymobs-1.21.1-0.2.0.jar`.
GitHub Actions: `Minecraft Mod Build Hub` with project_dir `projects/cowardly-mobs`. Production releases are published by push-to-main CI.

## Gameplay checks
1. Start NeoForge 1.21.1 single-player and a dedicated server, without the separate Zombie Hunger JAR.
2. Drop rotten flesh near a pursuing regular zombie: it should divert to the dropped item.
3. Drop raw/cooked meat when the zombie is idle; verify it walks over and eats.
4. Wound zombie below 40% HP and equip strong armor. Confirm retreat occasionally interrupts eating and does not increase movement speed.
5. Count items in a stack; precisely one should disappear after chewing; healing should be 2 HP.
6. Hit zombie while chewing: eating must be interrupted and the food ignored for 100 ticks.
7. Spawn three zombies competing for one piece of meat and ensure no item duplication.
8. Verify nearby-zombie food alerts, husk preference, drowned underwater behavior.
9. Verify eating sound, particles, skeleton ranged spacing and shooting.
10. Test an external datapack adding meat to legacy tags; test chunk unload/reload and dedicated server.

Note: GitHub CI verifies compilation and JAR structure, not in-game behavior.