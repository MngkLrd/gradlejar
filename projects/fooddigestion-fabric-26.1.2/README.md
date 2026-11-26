# Food Digestion 1.0.0
Minecraft 26.1.2 / fabric / Java 25 / Gradle 9.5.1.
Build: gradle --no-daemon clean build. The digestion queue restores 40% of hunger immediately (minimum one point) and releases the rest at 2, 3.5 or 5-second intervals depending on nutrition. Vanilla effects are not changed. Pending nutrition is capped by the missing hunger capacity and stored in player food data. No Fabric API required. Gameplay and compatibility require testing.
