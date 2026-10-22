# WallSlam 1.0.0 — Minecraft 26.3 / NeoForge

A lightweight server-authoritative melee wall-impact mod. A successful melee hit that knocks a mob into a solid wall triggers a small bonus hit, 10 ticks of movement stun, vanilla sounds and particle effects (including wall-material dust). No items, blocks, keybinds, shaders or hard dependencies.

## What changed since 0.3.3

- Confirm melee hits before any wall collision effects.
- Detect actual near-contact collision geometry (0.095 block tolerance), with horizontalCollision fallback for thin shapes.
- Use the dedicated `wallslam:collision` datapack damage type tagged `minecraft:bypasses_cooldown`. This no longer edits the mob's invulnerable-time field, and is subject to regular armor and effect reductions.
- Preserve the mob's attack target during the movement stun; never write new NoAI state.
- Clean up combat state when mobs unload, leave a dimension or die.
- Material-specific vanilla block particles, along with existing explosion/cloud/spark visuals and impact audio.
- World/server config `wallslam-server.toml`: damageMultiplier (default 1.0), stunTicks (10), particleMultiplier (1.0), debug (false).

## Build

Java 25, Gradle 9.2.1, NeoForge 26.3.0.76 or newer within Minecraft 26.3. Run `gradle clean build` in this project folder. JAR: `build/libs/wallslam-26.3-1.0.0.jar`.

## User acceptance check

Place a zombie in front of a full-block wall; attack with an iron sword and also try bare hands. Verify exactly one impact visual/audio burst, bonus health loss and temporary pause followed by normal pursuit. Repeat with no wall, fences/slabs, varied armor, strong knockback, multiple simultaneous targets, peaceful mobs, two players, world save/reload and dedicated server. Do not ship as stable if gameplay fails.

CI compilation and JAR validation run on GitHub; a successful build is not a substitute for in-game testing. Gameplay reference is WallSlam 0.3.3 on 1.21.1.
