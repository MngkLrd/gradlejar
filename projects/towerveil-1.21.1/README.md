# Tower Veil 0.2.0 Beta — seamless edition

Minecraft 1.21.1 / NeoForge 21.1.262 / Java 21

## What it does

Stand near the middle of a 4x4+ patch of ordinary tilled farmland (you must actually stand ON it, not look from far away). Look almost straight down until the surface fully covers the camera for at least 8 ticks. Tower Veil transfers you into a separate, persistently saved farmland dimension. Raise your view: the entire visible world is farmland and a 39-block tower stands 24 blocks away.

**No vanilla dimension loading screen**: the v0.2 implementation uses Immersive Portals' `/portal tpme` seamless teleportation, invoked server-side by Tower Veil. If Immersive Portals is absent, Tower Veil intentionally refuses to teleport instead of falling back to a visible vanilla loading screen.

## Dependencies

Required for seamless operation, in addition to Tower Veil:
- Minecraft 1.21.1 (Java Edition)
- NeoForge for Minecraft 1.21.1
- Immersive Portals for NeoForge 1.21.1 (for example Immersive Portals CE 6.0.9). Install a build exposing `/portal tpme`.
- Cloth Config API for NeoForge if required by the chosen Immersive Portals build.

Both Tower Veil and Immersive Portals need to be installed on the client and server. Do not install competing builds of Immersive Portals together.

## Build

- Java 21
- Gradle 9.2.1, using the MngkLrd/gradlejar GitHub Actions build hub
- `gradle clean build`
- Output: `build/libs/towerveil-neoforge-mc1.21.1-0.2.0-beta.jar`

## Test

1. Make a new TEST world. Install both mods.
2. Confirm the `/portal tpme` command exists using tab completion (requires permission in some mod versions).
3. Hoe 5x5 or 6x6 farmland in the open (4x4 may not fully fill a wide FOV).
4. Stand in the middle, look down vertically for 0.4 seconds, then raise your head.
5. You should be standing on farmland at Y=64.9375, not buried underground.
6. The tower should be 24 blocks away, built on the surface.
7. Type `/towerveil return` and confirm the exact entry location returns with no loading screen.
8. Re-test on subsequent visits and on a dedicated server.

## Limitations

- This build integrates the Immersive Portals runtime; it does NOT independently implement simultaneous multidimensional client loading.
- Chunk creation for the first visit may cause a short stall, even though no full-screen terrain loading UI is shown.
- This integration has been verified only by Gradle compilation. It is not yet confirmed by in-game testing against every NeoForge Immersive Portals fork.
- Farmland screen coverage currently approximates a 70-degree vertical FOV at 16:9, using a 3x3 sample ray grid, rather than the player's actual client FOV/aspect.
- The field is a flat farm world, not void-only outside the tower. There is one fixed tower in the beta.
- The fallback while the teleport runtime is absent is an action-bar warning, not a visible teleport.
