# Tower Veil 0.1.0 Beta (Minecraft 1.21.1, NeoForge)

Stand ON an ordinary 4x4+ patch of freshly tilled vanilla farmland in any dimension, near its middle. Look almost straight down and let the farmland fill the entire view for roughly 0.4 sec. You are transferred to a different dimension while still looking at similar farmland. Raise your camera to reveal an otherwise empty, endless farm field and a stone tower about 24 blocks away.

## Details
- Trigger: server-side 4x4 contiguous farmland check and server-side screen coverage approximation (nine rays, assumed 70-degree vertical FOV, 16:9).
- Only detects a player STANDING ON the farmland, not looking from afar.
- This beta uses vanilla dimension teleportation: there can be a brief loading screen.
- Real client FOV, aspect ratio and other camera effects are not detected. Plow 5x5 to 6x6 if 4x4 does not fill your view.
- Destination: flat farmland which does not dry out, and one enterable 39-block-tall stone tower with climbable ladder.
- Use /towerveil return to teleport to the saved original spot, even if it was beside your house.
- Mod is required on the client and dedicated server.

## Build
- Java 21
- Gradle 9.2.1
- Run: gradle clean build
- Output: build/libs/towerveil-neoforge-mc1.21.1-0.1.0-beta.jar
- CI: MngkLrd/gradlejar GitHub Actions

## Test
1. Start a new world with the mod.
2. Till a patch of at least 5x5 farmland in the open. Do not plant crops on it.
3. Stand near the middle and look vertically down for half a second.
4. Raise your view in the other dimension: the tower should be about 24 blocks ahead.
5. Walk into the tower, climb ladder, and issue /towerveil return.
6. Confirm exact return coordinates, including when built beside your house.

## Known beta limitations
- Vanilla dimension loading screen might briefly appear. Truly invisible transfer requires client preload/transition rendering.
- Screen coverage is approximated, not measured against your actual FOV or ultrawide aspect ratio.
- Tower is built on first entry; removing central lodestone allows it to rebuild.
- Field farmland does not dry but can still be trampled by long falls.
