# WorldLogic Terrain 0.2.0
Minecraft 1.21.1, NeoForge, Java 21, Gradle 9.2.1, official mappings. Requires WorldLogic >= RC20, API v1. Build downloads and verifies the exact RC20 release JAR; no shaded core. Runtime QA installs the same pinned dependency. Metadata minimum 21.1.80, QA pin 21.1.262.
18 plants, 20 textures/models, block items, loot, RU/EN and natural tab.
Server-side movement modifier uses supporting floor and intersecting vegetation, does not remove core modifiers. Core API supplies movement strength, weather sample and material family; no duplicated stamina, injuries or ice friction. Rain further slows farmland; plain grass blocks neutral. Unknown materials neutral except core soft landing family. No addon HUD.
QA /worldlogicterrain inspect evaluates the same factor with actual server block states and API. RCON checks cover floor, plant, core strength and clearing; these do not test real player movement or graphical rendering. No tree/worldgen expansion yet.
Build: gradle --no-daemon clean build. Output build/libs/worldlogicterrain-neoforge-mc1.21.1-0.2.0.jar.
