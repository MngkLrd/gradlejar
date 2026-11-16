# Shield Bash Lite 1.0.4 — Minecraft 1.20.1 Fabric

This project is an independent port, reusing the 1.21.1 Fabric logic.

- Game runtime **Java 17**; build Gradle runs on **Java 21** because Loom 1.11.8 requires it.
- A Java 17 JDK toolchain is used to compile the mod and `options.release = 17`.
- Fabric Loader 0.17.3; Fabric API 0.92.12+1.20.1.
- A successful vanilla shield block during the first **20 ticks** (1 second) stuns the attacker with Slowness and Weakness for **40 ticks** (2 seconds).
- Knockback: **0.175**; cooldown: **30 ticks**; success particles: white CLOUD.
- No custom sounds, items, recipes, or client-only code.
- Does not trigger from ranged mobs or projectile attacks, bosses excluded.
- Note: the stun uses effects; it is not a complete AI/action freeze.

Compile: `gradle clean build`
Expected output: `build/libs/shieldbashlite-fabric-1.20.1-1.0.4.jar`

CI uses build-profile Java 21 to run Gradle, while the compiled jar remains Java 17 compatible.
Tests: block zombie melee within the first second; check particles, reduced movement, light knockback; verify arrows don't activate it. For dedicated servers install Fabric Loader and Fabric API.
