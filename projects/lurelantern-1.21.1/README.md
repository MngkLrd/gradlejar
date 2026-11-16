# Lure Lantern 1.1.0 (NeoForge 1.21.1)

Build source is archived as `source.tar.xz` to retain its PNG textures. GitHub Actions restores it before Gradle builds, then uploads the verified JAR. Java 21 / Gradle 9.2.1.

The 1.21.1 Java compatibility fixes are applied during CI source restore: correct TamableAnimal package and public codec() override.
