# Minecraft Mod Build Hub

Reusable CI workspace for Minecraft mods. The repository contains a first project (**FieldKit 1.2.2**) in its root directory as the bootstrap project. Future independent projects live in `projects/<mod-id>/`, each with its own real Gradle project and `build-profile.json`.

## Build a mod

Open [Actions](../../actions/workflows/build.yml) → **Minecraft Mod Build Hub** → **Run workflow**.

Enter `.` for FieldKit, or `projects/<mod-id>` for another mod. The workflow reads the project's declared versions of Java and Gradle, downloads its actual mod loader/Minecraft dependencies, performs `gradle clean build`, checks the real JAR, and publishes a **pre-release** on [Releases](../../releases).

Builds triggered by a push to main use the default project (`.`). Pull requests compile and verify but **do not publish** a release.

No fake Minecraft APIs and no manual bytecode name remapping.

## Project contract

Each mod directory requires:
- `settings.gradle` (or `.kts`) and `build.gradle` (or `.kts`).
- `build-profile.json` declaring `slug`, `version`, `minecraft`, `loader` (`neoforge`, `forge`, `fabric`), `java` (17 or 21), `gradle` and `jar_glob`.
- `src/main/resources/pack.mcmeta` plus the loader's metadata.
- Optional `tools/verify_release.py` to perform project-specific bytecode checks.

Example profile:
```json
{
  "slug": "fieldkit",
  "version": "1.2.2",
  "minecraft": "1.21.1",
  "loader": "neoforge",
  "java": "21",
  "gradle": "9.2.1",
  "jar_glob": "fieldkit-1.2.2.jar"
}
```

The profile's Java and Gradle versions are used by CI. Actual dependencies and mappings remain defined in the mod's Gradle files. Therefore individual Forge/NeoForge/Fabric projects are **not** treated as interchangeable.

## Verification scope

Success means: genuine Gradle compilation, valid ZIP/JAR, required metadata, no bundled fake Minecraft classes, a single selected binary and optional per-project static validation. Success does **not** imply that the mod passed an in-game Minecraft client test. Always test runtime UI, keybinds, startup, and server/client separation before marking a build production-ready.

## Why Releases?

The linked account reached its GitHub Actions artifact storage quota. This workflow publishes JARs as pre-releases instead of using `actions/upload-artifact`.
