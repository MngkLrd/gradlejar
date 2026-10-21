# Bearback 0.1.2 — Minecraft 1.20.1 Forge

Gameplay-parity port of the 1.21.1 NeoForge Bearback mod: black bear mob in forest/taiga, charcoal fur with beige chest, brown 3D leather saddle on black and polar bears, tame with cod/salmon (25% chance), owner defense/follow, ride with WASD and Space, swipe with R (6 damage, 30 ticks cooldown), heal with fish, remove saddle with sneak+shears.

- Java: 17; Gradle: 8.14.5; Forge build target and tested minimum: 47.1.0.
- Forge lower bound in mods.toml: [47.1.0,)
- Build: `gradle clean build`
- JAR: `build/libs/bearback-1.20.1-0.1.2.jar`
- No Rideable Polar Bears dependency.

For checks: /summon bearback:black_bear, tame both bears, saddle, mount, ride, jump, swipe, save/reload, test dedicated multiplayer including client saddle synchronization.

The gameplay and saddle cube measurements match the original 1.21.1 mod. This version uses native Forge SimpleChannel and standard entity NBT with a client saddle sync packet instead of NeoForge 1.21.1 attachments.
