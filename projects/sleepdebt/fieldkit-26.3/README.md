# FieldKit Compact HUD — Minecraft 26.3

**Port of FieldKit 1.2.2.** NeoForge **26.3.0.90**, Minecraft **26.3**, Java **25**, ModDevGradle **2.0.148**.

Keybindings: **H** toggle HUD, **V** compact mode, **B** waypoint under crosshair, **N** clear waypoint.

Changes from 1.21.1: SDL scancodes instead of GLFW keys; `Identifier` instead of `ResourceLocation`; modern GuiGraphicsExtractor render submission and Matrix3x2fStack transformations. 

This is an independent Gradle project. From this folder: `gradle clean build`. Expected output: `build/libs/fieldkit-1.2.2.jar`.

Automated GitHub Actions builds use official Minecraft and NeoForge dependencies. A passing build does not prove client runtime/UI behavior, so test the JAR in Minecraft 26.3 before public release.
