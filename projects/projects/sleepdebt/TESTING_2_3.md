# Sleep Debt 2.3 QA (Minecraft 1.21.1 NeoForge / Java 21)
Run with mandatory JEI + EMI + Jade + Patchouli and optionally Comforts.
## Coffee machine
1. Place the machine. Right click: vanilla 3-row container opens.
2. Put water bucket in slot 1, sleepdebt:coffee_grounds in slot 2, coal in slot 3. Output is slot 4.
3. Verify smoke, sound, 200-tick cooking cycle and one produced coffee; water bucket is consumed into empty bucket in slot 1.
4. Reopen, unload/reload chunk, verify inventory and progress persist.
5. Break coffee machine with ingredients; verify behavior and prevent dupes.
## Camp
6. Place awning above the bedroll and build a vanilla campfire within 3 blocks.
7. Right-click bedroll, verify fatigue/stress reduction and cooldown, no time-skip abuse.
8. Destroy/rebuild blocks. Test in rain, dark and multiplayer.
## Night terrors
9. Accelerate debt accumulation in sleepdebt-server.toml, verify dusk/very tired stages.
10. Hallucinations must be client-only and never cause server damage, collision or world entities.
11. In darkness at high debt, Sleepless naturally approaches only exhausted players.
12. Daylight/strong light causes Sleepless to disappear; dedicated server does not load client renderer.
## Existing mechanics
13. Coffee, Warm Milk, Patchouli pages, Jade hints, JEI/EMI, config, death respawn and saved NBT.
## Commands
gradle clean build
Output: build/libs/sleepdebt-1.21.1-2.3.0-alpha.1.jar
## Known limitations
- Coffee maker reuses native chest 3-row UI, not a specialized 4-slot screen.
- No natural campsite worldgen in alpha; camps are player-built.
- Hallucinations are HUD peripheral shadows, not fake rendered 3D mobs.
- Comforts follows sleeping state; not verified in-game with modded bedrolls.
- Only compilation and JAR structure are checked in CI, not gameplay.
