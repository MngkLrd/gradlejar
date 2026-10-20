# Wallslam 0.1.1 — Minecraft 1.21.1

A small server-authoritative NeoForge combat mod. Punch or strike a mob with enough knockback and slam it against solid terrain: the mob takes an additional **1–4 HP** based on knockback strength, a stone impact sound plays, and a few critical-hit particles appear.

- **No new weapons**, enchantments, blocks, GUIs, recipes, or keybindings.
- Triggers only from actual `LivingKnockBackEvent` knockback and real horizontal wall collisions within 10 entity ticks.
- Mobs only. Players are excluded from secondary impact damage in this first release.
- A 12-tick re-trigger cooldown prevents chained recursive impacts.
- Requires no client installation when installed on a dedicated server for actual damage. Particle/sound effects use vanilla network broadcasts.
- First release is mechanically consistent but does not distinguish which specific block was struck (any collidable wall qualifies).
- All impacts are managed on the logical server, no scan over chunks or loaded entities.

**Balance constants:** `MIN_KNOCKBACK=0.29`, `MAX_TICKS_AFTER_HIT=10`, `RETRIGGER_DELAY_TICKS=12`, `damage=min(4,1 + 2*strength)`. They are in `dev.wallslam.mechanics.ImpactTracker` for easy rebalance in the next build.

## Build

- Minecraft: `1.21.1`
- NeoForge 21.1.262
- Java: 21
- Gradle: 9.2.1
- Command: `gradle clean build`
- Result: `build/libs/wallslam-1.21.1-0.1.1.jar`

## Testing checklist

1. Start a clean world with the matching Minecraft and NeoForge version.
2. Knock a zombie into a solid wall at close range and confirm secondary damage plus impact sound/particles.
3. Attack a zombie in an open field — normal melee damage only.
4. Walk a mob into a wall — no Wallslam.
5. Knock a mob into the ground (without horizontal collision) — no Wallslam.
6. Attack a player against a wall — no secondary impact damage.
7. Repeat on a dedicated server. Client without the mod can connect when server runs the mod (no custom packets).
8. Test with high knockback, armored mobs and large entities.

**In-game tests are required before calling the mod fully validated.** CI verifies actual Gradle compilation and static packaging, not gameplay.



## Loading metadata

NeoForge 21.1 requires `modLoader="javafml"` and `loaderVersion="[4,)"` in `META-INF/neoforge.mods.toml`; missing these caused a real game startup failure in v0.1.0. The shared CI verifier checks the packaged TOML and the expected loader schema before publishing a JAR.
