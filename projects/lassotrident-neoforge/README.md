# Lasso Trident 0.2.0 - NeoForge 1.21.1

This port updates the rope's visuals and movement physics. Attach a normal Minecraft lead to an ordinary trident in an anvil; item components and enchantments are preserved.

## Changes in 0.2.0
- Replaces two glowing debug lines with a real vanilla-shaded `RenderType.leash()` mesh made of crossed strips.
- Adds a slight natural sag and reactive side-to-side rope movement, with smoothly interpolated ambient lighting.
- Adds gentle player acceleration while reeling toward the trident.
- Rope goes taut at its current length. Once taut, it blocks most outward movement but retains tangential velocity so players can swing while not pulling. Gravity and normal WASD remain active.
- The rope shortens progressively while RMB is held, without changing the 30-second auto-release.
- No extra PNG assets, dependencies or texture overrides.

## Gameplay
1. Combine trident + lead in an anvil (2 levels).
2. Throw at a solid wall or ceiling.
3. Hold RMB to reel yourself upward; release to dangle/swing.
4. Press RMB again to detach. The trident drops.
5. After 30 seconds it drops automatically; maximum reach 48 blocks.

## Build
Java 21, Gradle 9.2.1, NeoForge 21.1.197, Minecraft 1.21.1:
`gradle --no-daemon clean build`
JAR: `build/libs/lassotrident-neoforge-1.21.1-0.2.0.jar`

## Manual test
- Inspect the rope in daylight and darkness, straight down and vertical up.
- Swing while letting go of RMB; then resume winching by holding RMB.
- Verify pickup and timeout preserve existing item data.
- Test in multiplayer and on a dedicated server. CI compiles but does not run client gameplay.
