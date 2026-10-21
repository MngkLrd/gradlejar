# TraumaCraft 0.1.0
First prototype for Minecraft 1.21.1, NeoForge, Java 21.

Provides 7 local body HP pools (440 total), approximate hit locations for projectile/melee, fall damage to both legs, fracture slowdown, bleeding, bandages, splints and medkits.

Use /trauma to inspect health. Operator tests:
- /trauma hit left_leg 50
- /trauma fracture right_leg
- /trauma bleed left_arm 2
- /trauma reset

Requires Gradle 8.14.3 and JDK 21.
Build: gradle clean build
JAR: build/libs/traumacraft-0.1.0.jar

Caveats: vanilla hearts still represent death; local zones supplement them. Minecraft melee hit locations are approximated; medical assets are vanilla placeholders. Not gameplay-tested.
