# WorldLogic Terrain 0.1.1
Minecraft 1.21.1 / NeoForge 21.1.80+ (metadata; not yet tested at minimum) / Java 21 / Gradle 9.2.1 / official mappings. Compile and QA pin 21.1.262. Requires WorldLogic NeoForge >= 1.0.0-rc20.
18 plants and block items, 20 textures and models, soil support, loot, RU/EN, creative tab. Same registry IDs as Forge prototype. No new generation, terrain-speed logic or content expansion in this migration.
Build: gradle --no-daemon clean build. Output: build/libs/worldlogicterrain-neoforge-mc1.21.1-0.1.1.jar.
Server QA: shared scenario covers soil support and loot. Sand fixture now has a solid base. Client render and player interaction not verified.
Dependency blocker: core rc20 build run 45967712297 failed compile; no published NeoForge core binary/digest as of this commit. Mandatory runtime dependency stays intact; QA dependencies must be populated with the actual published SHA-256 before server validation can pass. Do not release or claim verified in its absence. Forge prototype is superseded, not enrolled.
