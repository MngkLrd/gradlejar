# Lure Lantern 1.1.1 (NeoForge 1.21.1)

Build source is archived as `source.tar.xz` to retain its PNG textures. GitHub Actions restores it before Gradle builds, then uploads the verified JAR. Java 21 / Gradle 9.2.1.

The 1.21.1 Java compatibility fixes are applied during CI source restore: correct TamableAnimal package and public codec() override.

CI checks the exact MapCodec<LanternBlock> return type required by Minecraft 1.21.1.

### Texture model fix
Version 1.1.1 replaces the erroneous 16x16 centered item-silhouette texture with a precise vanilla UV atlas for the lantern body [0,2,6,9], base, cap, and handle. Four models set `minecraft:cutout` explicitly. The reproducible atlas generator is `tools/rebuild_textures.py` and runs in CI after unpacking the original source archive.
