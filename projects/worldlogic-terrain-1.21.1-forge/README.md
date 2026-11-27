# WorldLogic Terrain 0.1.0
Minecraft 1.21.1 / Forge 52.1.0 / Java 21 / Gradle 8.10.2 / official mappings. Mandatory WorldLogic >= rc17. Exact rc17 dependency hash matches owner's supplied JAR.
18 registered plants and block items; 20 textures and models; natural-blocks creative tab; English/Russian; non-solid plants, two-block reed/cattail, ground cover, soil support, loot. Ground-cover model rotations added. No automatic world generation, movement modifiers or tree replacement in this content prototype. Use creative inventory or /give to obtain plants.
Build: gradle --no-daemon clean build. Output: build/libs/worldlogicterrain-forge-mc1.21.1-0.1.0.jar.
QA: server scenario checks support and drops of all plants; commands do not establish network player placement; Forge bot unsupported; graphical client not tested. Forge 1.21.1 is absent from compatibility.json; normal QA gate will reject until separately supported, no legacy exception or alternate release route.
Next: more grasses/flowers, foliage variants, then tree shapes and vegetation distribution in the same addon; protect WorldLogic generator and existing worlds.
