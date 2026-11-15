# WallSlam 1.0.2 — Minecraft 1.21.1 / NeoForge

A lightweight server-authoritative melee wall-impact mod. A successful melee hit that knocks a mob into a solid wall triggers a small bonus hit, 20 ticks (1 second) of movement and melee stun, vanilla sounds and particle effects (including wall-material dust). No items, blocks, keybinds, shaders or hard dependencies.

## What changed since 0.3.3

- Confirm melee hits before any wall collision effects.
- Restore the verified 0.3.3 forward sweep (0.65 blocks) using solid block collision shapes.
- Use the dedicated `wallslam:collision` datapack damage type tagged `minecraft:bypasses_cooldown`. This no longer edits the mob's invulnerable-time field, and is subject to regular armor and effect reductions.
- Preserve the mob's attack target during the movement stun; never write new NoAI state.
- Clean up combat state when mobs unload, leave a dimension or die.
- Material-specific vanilla block particles, along with existing explosion/cloud/spark visuals and impact audio.
- World/server config `wallslam-server.toml`: damageMultiplier (default 1.0), stunTicks (10), particleMultiplier (1.0), debug (false).

## Build

Java 21, Gradle 9.2.1, NeoForge 21.1.100 or newer within Minecraft 1.21.1. Run `gradle clean build` in this project folder. JAR: `build/libs/wallslam-1.21.1-1.0.2.jar`.

## User acceptance check

Place a zombie in front of a full-block wall; attack with an iron sword and also try bare hands. Verify exactly one impact visual/audio burst, bonus health loss and temporary pause followed by normal pursuit. Repeat with no wall, fences/slabs, varied armor, strong knockback, multiple simultaneous targets, peaceful mobs, two players, world save/reload and dedicated server. Do not ship as stable if gameplay fails.

CI compilation and JAR validation run on GitHub; a successful build is not a substitute for in-game testing. Gameplay reference is WallSlam 0.3.3 on 1.21.1.

## 1.0.1 — visible and functional stun

- The new default `stunTicks` is **20** (1 second; previous default 10 ticks).
- During stun, the mob retains its original AI target, but cannot deal new direct melee damage. Projectiles are unaffected.
- Navigation and horizontal velocity are stopped each server tick as before; `NoAI` is never enabled.
- Existing world/server configuration files can retain the old `stunTicks = 10` value after updating. Set it to `20` manually in the generated WallSlam config and restart/reopen the world to test the intended balance.
- Damage, wall detection and impact visual effects are unchanged.

A successful Gradle build does not replace an actual client/server combat check.

## 1.0.2 — restore successful wall impacts

Fixes a regression from 1.0.0/1.0.1: the very tight 0.095-block contact check made most ordinary knockback wall impacts fail to register. Uses the gameplay-tested 0.3.3 solid-block sweep of 0.65 blocks instead. Existing 1.0.1 additions are kept: 20-tick default stun, suppression of direct melee attacks while stunned, configurable intensity and collision damage type. NoAI remains untouched. Both NeoForge ports are compiled in CI; actual gameplay confirmation is pending.
