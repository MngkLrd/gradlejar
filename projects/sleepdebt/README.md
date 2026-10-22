# Sleep Debt 2.3.0-alpha.2 (Minecraft 1.21.1 NeoForge / Java 21)
## New systems
- 3D copper coffee maker: 27-slot vanilla-style chest screen. Water bucket slot 1, coffee grounds slot 2, coal slot 3, product slot 4; brewing runs 200 ticks with real server smoke + sound. Ingredients persist, contents drop on machine break.
- Player-built field camps with bedroll + awning + vanilla campfire. Camp resting has a 2-minute cooldown. Shelter, campfire, pillow and sleep mask improve recovery.
- Physical fatigue and stress join persistent sleep debt. Sprinting, swimming and nearby monsters affect them. HUD shows all three.
- Client hallucinations: small peripheral eye shapes and dark eyelid borders under critical fatigue. Non-interactive, server never spawns illusions.
- The Sleepless: an actual hostile humanoid with custom 64x64 skin, server AI targeting sleep-deprived players, nighttime spawning under low light and daylight retreat.
- Patchouli in-game guide pages for camps, brewing and night terrors.
- Jade hints for beds/bedroll and coffee maker; standard recipes accessible in JEI and EMI.
## Dependencies
NeoForge 21.1.x + Java 21 + JEI, EMI, Jade and Patchouli. Comforts optional, no direct API override.
## Build
gradle clean build
build/libs/sleepdebt-1.21.1-2.3.0-alpha.2.jar
## Limits
- Camp structures are crafted and placed by the player, not world-generated.
- Coffee maker UI reuses a vanilla chest layout, not a bespoke four-slot machine screen.
- Hallucinations are HUD illusions, not fake 3D entities in the world.
- The Sleepless shares a vanilla humanoid animation rig.
- Tested through CI compilation and JAR verification; dedicated-server/client runtime tests pending.
