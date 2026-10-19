# FieldKit — NeoForge Minecraft 1.21.1

Compact coordinate HUD. This repository builds the source with **Java 21**, **NeoForge 21.1.262**, and **ModDevGradle**, not offline Minecraft stubs.

Go to [Actions](../../actions) for builds; after a successful workflow, download the `FieldKit-JAR` artifact.

Keybinds: **H** toggle HUD, **V** extra details, **B** mark waypoint, **N** clear waypoint.

Project release build: `gradle --no-daemon clean build`. JAR: `build/libs/fieldkit-1.2.2.jar`.

A successful Gradle build verifies compilation but **not** full in-game rendering; test the resulting JAR in Minecraft 1.21.1 with NeoForge 21.1.262 before release.
