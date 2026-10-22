# Ember Shelter in-game QA

1. Start NeoForge 1.21.1 using Java 21 with the produced JAR on client and server.
2. Run `/give @s embershelter:camp_kit` and test a flat 7x7 field with clear sky.
3. Verify item icon, recipe, staged fences, logs, wool, chest and torches.
4. Verify a blocked area refuses construction and consumes neither kit nor materials.
5. With `consumeMaterials=true`, verify full materials are required, charged once, and creative mode bypasses costs.
6. Confirm a camp lit at night causes basic zombies, skeletons and spiders to stop choosing the protected player as a target; test extinguished camp and daytime.
7. Restart server, check finished camp protection persists; break campfire and verify protection stops.
8. Repeat tests with a dedicated server (no client-only references in common code).
