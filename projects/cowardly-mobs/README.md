# Cowardly Mobs 0.5.0 — Spider Instinct

**Minecraft 1.21.1 | NeoForge 21.1.x | Java 21**

Cowardly Mobs 0.5.0 adds four server-driven spider behaviors without introducing new mobs, animation libraries or speed/damage buffs. The existing **Zombie Hunger, morale, zombie pack courage, skeleton firing line, group rally and dynamic skeleton-to-spider jockey mounting** remain integrated in the same JAR.

## Spider Instinct (new)

### 1. Backstab Instinct
- A hostile spider with a player target pathfinds toward a point approximately 3.8 blocks **behind** the direction the player faces and 2 blocks to the side.
- The side is stable for each spider rather than alternating every tick.
- It recalculates movement every 12 ticks and falls back to normal pursuit if a behind-player route cannot be calculated.
- Melee still uses the spider's own vanilla attack method and a normal ~20-tick cooldown, with no extra damage.

### 2. Gaze Avoidance
- A spider checks the player's **full 3D viewing direction**, a configurable **100-degree total cone**, within **12 blocks**, and an unobstructed vanilla line of sight.
- A spider reacts after approximately **10 consecutive ticks** of exposure to the player's gaze. A brief grace period prevents jitter when the player glances away.
- It tries to move 7–10 blocks out of sight, preferring safe vanilla pathfinding at the normal speed.
- A cornered spider can defend itself after about 1.5 seconds if the player is already in melee range. Spiders do not become invulnerable or passive.

### 3. Wall Ambush
- An unwatched spider occasionally examines a **small 2–5-block-radius local area** for a climbable 3-block-tall solid face with clear space on the climbing side.
- With suitable terrain, a default **50% roll** lets it approach the wall and climb using vanilla spider wall-climbing movement.
- Once at least about **2 blocks above its starting height**, it performs a **real physical airborne jump** toward the player; no teleportation or scripted render-only jump.
- The jump uses ordinary melee contact and applies **no additional damage**. A **200-tick (10-second) cooldown** keeps attacks from repeatedly chaining.
- Blocked paths, low ceilings, submerged spiders or unsuccessful climbing cancel safely. No block breaking or block placement.

### 4. Spider Jockey Synergy
- If a spider carries a skeleton who targets the player, it adopts a ranged-mount role.
- The spider attempts to stay about **9–11 blocks** from the victim and seeks an offset lateral firing position, rather than always charging into melee.
- It respects normal movement speed. Mounted spiders do **not** use the solo spider wall-ambush or gaze-escape decisions.
- The rider's vanilla bow attacks are preserved. A skeleton on foot retains the 0.4.0 dynamic spider-mount leap and landing animation.

### How spider aggression is triggered

The new behaviors activate on spiders already attacking an ordinary survival player through vanilla targeting, or when a **skeleton passenger targets the player**. They do **not** force daylight-passive spiders to attack.

### Server config

New: `config/cowardlymobs-spiders-common.toml`:

```toml
[spider_instinct]
enabled = true
flanking = true
gazeAvoidance = true
wallAmbush = true
jockeyTactics = true
gazeRange = 12
gazeConeDegrees = 100.0
wallAmbushChancePercent = 50
diveCooldownTicks = 200
```

The older config files `cowardlymobs-common.toml` (Zombie Hunger) and `cowardlymobs-jockey-common.toml` (dynamic skeleton mounting) are preserved.

## Client/server separation

All spider decisions, navigation and airborne movement run on the logical server. Vanilla clients can observe the movement. To see the optional 0.4.0 **skeleton landing pose** animation, install the mod on both client and server. No client-only code is imported by the new spider AI.

## Build

Requires **JDK 21** and **Gradle 9.2.1**.

```bash
cd projects/cowardly-mobs
gradle --no-daemon clean build
```

Output: `build/libs/cowardlymobs-1.21.1-0.5.0.jar`.

## Manual gameplay testing (CI is compilation only)

1. Start Minecraft 1.21.1 NeoForge client and a dedicated server. Test no client-only class loading on headless server.
2. At **night**, summon a spider with a survival player nearby. Face away and verify that the spider tries to move around to the rear.
3. Look directly at the spider within 12 blocks for at least 10 ticks. It should interrupt its attack approach and try to back off at normal speed.
4. Look away again. After the gaze-grace period it should resume flanking, without movement jitter.
5. Fight in a cave with solid vertical walls at least 3 blocks tall. With `wallAmbushChancePercent = 100` and `diveCooldownTicks = 40`, test that the spider physically **climbs the wall** and **jumps from height**. Flat terrain should produce no invented airborne wall pounces.
6. Fight the same spider in a corner; verify it can still bite instead of becoming harmless.
7. Spawn a skeleton riding a spider, or enable the 0.4.0 dynamic mounting chance. The mount should reposition at bow range rather than always charging at the player.
8. Remove the passenger. The spider should return to ordinary flank/gaze/wall logic when it has a valid player target.
9. Disable each new toggle in the config and verify the corresponding behavior stops.
10. Test cave spiders, walls made from logs, low ceilings, water, daylight, obstacles, and large groups for performance and pathfinding fallbacks.
11. Regression: test Zombie Hunger meat pickup/healing, husks/drowned, zombie pack morale/rally, skeleton firing line and dynamic mounting.
12. Test chunk unload/reload and dimension transfers to ensure the AI goal is installed only once per entity.

### Known limitations
- Complex terrain may prevent a spider finding a valid wall or a path up it; the vanilla pursuit fallback remains.
- The wall leap uses actual Minecraft entity movement and vanilla leg animation, not a new rigged animation asset.
- Line-of-sight awareness is geometric; there is no actual client rendering/frustum check or awareness through walls.
- Github CI verifies compilation and JAR integrity; the full in-game movement and visual quality require manual testing.
- Avoid installing the separate Zombie Hunger mod together with Cowardly Mobs, since the behavior is already integrated.
