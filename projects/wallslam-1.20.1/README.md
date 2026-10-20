# Wallslam 0.2.0 — Minecraft 1.20.1

Small server-side combat mod for Minecraft 1.20.1 / NeoForge.

Ordinary bare-handed attacks can slam a zombie, pig, villager or any living mob into a **solid wall**, without Knockback enchantments. Weapon attack power changes the collision damage and adds a small physical knockback bonus.

## Behavior

- On an actual melee hit, record the direction from the attacker to the victim.
- For up to 9 game ticks, check *solid block collision shapes* in that direction (31 cm ahead of the bounding box) after each mob's tick.
- On a wall contact: deal 0.65–3.25 additional health points (scales with actual attack damage attributes), play a clear stone crash sound, and emit POOF and critical particles.
- Additional weapon push: up to +0.25 knockback strength for high-power weapons; fists also work.
- Ten-tick individual cooldown prevents spam.
- No client-only classes. Only living Mob targets get collision damage (does not damage other players).
- Does not rely on the deprecated single-frame horizontalCollision flag or on a minimum LivingKnockBackEvent strength.

## Balance examples (approx.)

- Unarmed base attack (~1): 0.68 health impact
- Stone sword (~5): ~1.60 health impact
- Iron sword (~6): ~1.83 health impact
- Netherite sword (~8): ~2.29 health impact

In-game damage can vary with enchantments, attack modifiers and target armor.

## Version and build

- Minecraft 1.20.1, NeoForge, Java 17, Gradle 8.14.5.
- `gradle clean build`
- Expected JAR `build/libs/wallslam-1.20.1-0.2.0.jar`

## Manual smoke testing

1. Spawn a zombie, pig and villager adjacent to a full stone/dirt wall; hit each toward that wall with an empty hand and then a sword. An audible crash and burst of particles should occur.
2. Repeat with wood, stone, iron, diamond and netherite weapons to compare force and impact damage.
3. Repeat with a mob in open air: no Wallslam particle/sound or extra collision damage.
4. Strike a mob while next to a wall but *away* from the wall: no impact.
5. Test on a dedicated NeoForge server and client; test high knockback resistance and modded mob hitboxes.

GitHub CI builds against real official Minecraft/NeoForge dependencies and validates the JAR. It does not actually launch the client; gameplay must still be verified.
