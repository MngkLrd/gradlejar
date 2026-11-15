# Sleep Debt 3.0.0 Alpha 11 — Night horror QA

1. Minecraft 1.21.1, NeoForge 21.1.x, Java 21, Survival. Use /time set midnight and /sleepdebt debug preset critical.
2. Outdoors away from camp in loaded chunks, an apparition should appear around 19–30 blocks away. Look at it after 1.8 s: it fades after approximately 11 ticks of direct gaze. No hitbox or collision.
3. Ignore the figure; after around 3.6 seconds it should stalk slowly and emit spaced footsteps. Direct gaze stops movement.
4. /sleepdebt debug spawn_sleepless spawns the real hostile entity; attacks should have varied recovery, darkness and smoke. Vignette becomes intense up close.
5. Die near the hunter. It is dismissed at the death location, even if you respawn elsewhere. On fresh configs debt resets. Existing TOML files with fatigue.persistAfterDeath=true require setting it to false to reset.
6. With persistAfterDeath=true, debt remains, but the 3600-tick grace prevents immediate repeated hunting.
7. Enter a bed successfully with hunter far enough away that vanilla permits sleep: hunter dismissed, 1200-tick protection. Bed eligibility remains vanilla.
8. On daylight change, real hunters and client visions vanish. Verify existing registries, NBT, opt-in integrations and dedicated server startup.
9. CI proves compilation/packaging only; run actual game tests before a stable release.
