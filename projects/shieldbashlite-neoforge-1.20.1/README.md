# Shield Bash Lite v1.0.4 — Minecraft 1.20.1 NeoForge

- **Java:** 17. **Gradle:** 8.14.5.
- Loader used to compile: NeoForge 1.20.1-47.1.106 (legacy net.minecraftforge.* namespaces).
- 20-tick perfect-block window, 40-tick Slowness/Weakness stagger, 0.175 knockback.
- 30-tick cooldown. White CLOUD particles, no added sounds.
- Ranged mobs/projectiles and bosses excluded; PvP not supported.
- Requires the normal vanilla shield. Event triggers ONLY on a successful shield block.
- Does not override mob AI entirely. Stagger is implemented using status effects.
- No custom items, recipes, textures, sounds or client-only classes.

Build: `gradle clean build`
Output: `build/libs/shieldbashlite-neoforge-1.20.1-1.0.4.jar`

Compatibility: Minecraft 1.20.1. The "forge" mod dependency name is intentional in NeoForge 1.20.1: that historical loader still declares modId "forge". Do not port this metadata unchanged to NeoForge 1.21.x.

Testing after the JAR compiles: create survival world, let a zombie attack a raised vanilla shield within 1 s; check particles, slowdown and knockback. A skeleton's arrow must NOT trigger. Test multiplayer if intended for servers.
