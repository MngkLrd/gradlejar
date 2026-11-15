# Traumatic 0.3.2 - Minecraft 26.3

Fix: port all 23 shaped and shapeless recipes to the Minecraft 26.3 string-ingredient schema. Previously invalid recipe JSON prevented registry initialization and world creation. A Gradle validation task now rejects legacy recipe format before JAR creation. Damage logic, hit locations, bleeding, fractures, healing, items, GUI and HUD are unchanged.

Build: Java 25, NeoForge 26.3.0.90+, Gradle 9.2.1.
