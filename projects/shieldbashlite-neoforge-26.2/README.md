# Shield Bash Lite v1.0.4 — neoforge / Minecraft 26.2

- Java 25; Gradle 9.2.1
- Vanilla shield perfect block: 20 ticks (1s)
- Stagger: 40 ticks (2s) through Slowness and Weakness
- Knockback: 0.175; cooldown: 30 ticks
- Server-side block detection; white CLOUD particles; no custom sounds
- Ranged attackers and projectiles excluded
- Vanilla Wither is excluded via RangedAttackMob, Dragon is not a Mob
- No additional items, recipes, networking or client-only classes

Compile: `gradle clean build`
Output: `build/libs/shieldbashlite-neoforge-26.2-1.0.4.jar`

This is a port of the existing v1.0.3 gameplay, not a new hard AI-freeze mechanic.
Test in-game with a zombie and skeleton; a compiled JAR alone does not
prove the stun feel, which requires gameplay validation.
