# FieldKit 1.2.2 — Compact Coordinate HUD

Minecraft **1.21.1**, NeoForge **21.1.262**, Java **21**. Built with **ModDevGradle 2.0.148**.

This is a client-only mod with an unobtrusive upper-left HUD at **50%** text size. Shows **XYZ**, and optionally direction, world time, and looked-at block/entity. Displays a temporary waypoint distance/direction.

- **H** — HUD on/off.
- **V** — coordinates-only / detailed HUD.
- **B** — mark waypoint at crosshair target (or player position).
- **N** — remove waypoint.

There is **no bottom/right equipment panel**, no prominent colored bar, and no gameplay items or recipes.

## Real build

GitHub Actions: open the **Actions** tab, select **FieldKit NeoForge JAR**, then download the compiled JAR artifact after a successful run.

Locally with Java 21 and Gradle 9.2.1: `gradle --no-daemon clean build`.

The production JAR is `build/libs/fieldkit-1.2.2.jar`.

GitHub CI compiles against **real NeoForge + Minecraft libraries**, never handwritten stubs. It also checks the package and JVM constant-pool reference types. A successful compile/static validation is **not equivalent** to a fully tested in-game release: run the JAR in an actual Minecraft/NeoForge client before final release.

The release is marked **prerelease** until it has also been tested inside an actual Minecraft 1.21.1 client.
