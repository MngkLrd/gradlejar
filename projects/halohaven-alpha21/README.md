# Halo Haven 2.1 Alpha — Living Skies
Independent NeoForge 1.21.1 expansion for **Halo Haven 1.8.0+**.
Requires the original Halo Haven mod (mod ID `halohaven`) installed alongside this addon.
The 2.1 addon registers 20 new entities, 25 microstructures and 2.0 regional blocks.

The bundled source archive contains an exact offline-reviewed snapshot of the Java and resource source tree, not a prebuilt binary.
The Gradle settings script expands it deterministically before project configuration.
All 20 new entity UV atlases are authored at 128x128 pixels with opaque mapped faces.
All 25 microstructures are stored as real compressed Minecraft NBT files; placement checks in-game remain necessary.

Build: `gradle clean build` using JDK 21 and Gradle 9.2.1.
Output: `build/libs/halohavenalpha2-2.1.0-alpha.jar`.
Game test note: CI compile is necessary, but neither a full gameplay test nor visual acceptance by the user.
