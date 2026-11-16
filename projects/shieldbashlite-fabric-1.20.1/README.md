# Shield Bash Lite 1.0.4 — Minecraft 1.20.1 Fabric

- Runtime **Java 17**; Gradle build JVM **Java 21** because Loom 1.11 needs it.
- Bytecode compiled using Java 17 toolchain and `options.release = 17`.
- Fabric Loader 0.17.3, Fabric API 0.92.12+1.20.1.
- Fabric API 1.20.1 has no `ServerLivingEntityEvents.AFTER_DAMAGE` event. This port instead injects at the RETURN of vanilla `LivingEntity.isDamageSourceBlocked` and triggers only when it returns true, on the server, with vanilla shield raised.
- Perfect window: 20 ticks; stun (Slowness+Weakness): 40 ticks; knockback 0.175; cooldown 30 ticks.
- White CLOUD particles, no custom sounds; excludes ranged mobs/projectiles.
- No added items, recipes, textures or client-only code.

Build: `gradle clean build`
JAR: `build/libs/shieldbashlite-fabric-1.20.1-1.0.4.jar`

**Testing:** On survival, hold the vanilla shield and let a zombie hit it within one second; particles must appear and zombie should be slowed. Hold shield for longer than 1 second: no bash. Skeleton arrows must not activate it. A compiled JAR does not guarantee gameplay results, test in client/server.
