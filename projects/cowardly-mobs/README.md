# Cowardly Mobs 0.3.0

Minecraft **1.21.1**, **NeoForge 21.1.x**, **Java 21**. Lightweight **server-side** zombie and skeleton AI. No custom items, blocks, client UI or networking. No movement-speed buffs or bow fire-rate changes.

This is one JAR containing the original Cowardly Mobs AI and the full integrated Zombie Hunger 1.0.0 behavior. **Do not install a second Zombie Hunger JAR.**

## New in 0.3.0

### 1. Morale System

A server-side numeric courage score from 0 to 1 is calculated *only when a wounded mob considers fleeing* (every ~80 ticks with cooldown).

```text
courage = clamp(0.22 + 0.55 * health_fraction
                - 0.42 * player_armor_fraction
                - 0.40 * player_attack_damage_fraction
                + ally_bonus, 0, 1)
```

Armor is normalized to 20 points; damage is normalized against 1–10 attack damage. Nearby allied zombies add **+0.17 courage** each, bow/melee skeletons add **+0.09** each (maximum four). A survival player must still have either 12+ armor points or 6+ attack damage to trigger a fear reaction. Only mobs at or below **65% HP** are eligible for fear; when courage is below **0.36**, their chance to retreat is 25–70% depending on danger. The normal cooldown prevents rapid fear toggling.

### 2. Zombie Pack Courage

Nearby zombies of the same kind provide morale support within a 9-block radius. A wounded lone zombie is more likely to retreat from an armed player than one in a pack. The pack bonus affects only fleeing decisions: **no damage, speed or armor bonuses**.

### 3. Skeleton Firing Line

Bow-armed skeletons pursuing the **same player** form an opportunistic line of up to five near allies. They:
- keep about **11.5 blocks** from the target when safe to reposition;
- spread roughly **3.4 blocks** laterally, using stable UUID-based positions;
- sidestep an allied skeleton directly blocking their line of fire;
- back off if the target is within 8 blocks;
- surrender movement back to vanilla bow AI after at most 50 ticks, with a pause before repositioning again;
- skip squad positioning if the target is out of sight, to preserve normal vanilla pursuit.

This is a loose, terrain-dependent firing line, **not simultaneous volleys** and **not guaranteed friendly-fire immunity**. Wither skeletons carrying swords retain vanilla melee positioning.

### 4. Group Rally

An injured zombie or skeleton that chooses to retreat may look for a **nearby safe group of its own mob type**. If it reaches a group containing at least two allies within 5.5 blocks and is more than 8 blocks from the player, it regains its nerve:
- immediately stops retreating;
- resumes vanilla attack/target goals;
- suppresses a new fear decision for **220 ticks** (11 seconds).

If there is no suitable group, it simply retreats away from the player, as before. Pathfinding never teleports mobs or increases their speed.

## Zombie Hunger remains included

- Rotten flesh distracts ordinary zombies even when in combat.
- Raw/cooked meat feeds idle zombies; injured zombies may seek raw meat when engaged.
- One meat item eaten per meal, **+2 HP**, 30-tick chewing, 100-tick meal cooldown.
- A player hit interrupts eating and blocks further food seeking for 100 ticks.
- Nearby hunger alerts, competition over dropped food, vanilla eating sound and six item particles.
- Husks avoid cooked meat; drowned prioritize meat in water.
- Tags and datapack compatibility preserved:
  - `#zombiehunger:rotten_meat`
  - `#zombiehunger:raw_meat`
  - `#zombiehunger:cooked_meat`
  - optional common `#c:foods/raw_meat` and `#c:foods/cooked_meat`.

Eating has AI goal priority 2, below retreat (priority 1), so fear takes precedence over food. The Zombie Hunger config remains `config/cowardlymobs-common.toml` with the original `zombie_hunger` section and parameters. Balance constants for the new tactical AI live in `ai/AiBalance.java`; a dedicated configurable morale file is not included in 0.3.0.

## Building

JDK 21, Gradle 9.2.1:

```bash
cd projects/cowardly-mobs
gradle --no-daemon clean build
```

Output: `build/libs/cowardlymobs-1.21.1-0.3.0.jar`.

The repository build hub also builds and verifies this project on pull requests and publishes a prerelease JAR on main updates.

## Gameplay test checklist

1. Load Minecraft 1.21.1 / NeoForge on a client and a dedicated server.
2. Compare one injured zombie versus four injured zombies when a player wears iron armor. The group should retreat less often.
3. Confirm bare-handed, unarmored player does not trigger morale retreat.
4. Change player armor or held weapon and repeat; fear chance should change.
5. Place a separated squad of bow skeletons in sight of the same player. Observe loose lateral spacing and continued arrow shots.
6. Let one skeleton block another's shot; observe limited sidestepping (no forced terrain clipping).
7. Approach within 8 blocks: bow skeletons retreat at **normal speed**.
8. Retreating wounded zombie approaches two allies more than 8 blocks from player; it should rally and resume aggression without immediate re-fleeing.
9. Repeat group rally with skeletons and confirm sword-wielding wither skeletons still attack in melee.
10. Toss rotten flesh during combat and attack a chewing zombie: older Zombie Hunger behavior should still work, as should husk/drowned rules and datapack tags.
11. Test many mobs in a farm, chunk unload/reload and dimension transfer for duplicate AI or tick performance issues.

**Limitations:** The line is opportunistic, not rigid formation AI. Group selection is local. Compile/JAR verification is not a substitute for in-game behavior testing.
