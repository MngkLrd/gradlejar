# Redstone Vision 0.2.0-alpha

Minecraft 1.21.1 | NeoForge 21.1.262 | Java 21 | Gradle 9.2.1

Engineer's Goggles provide redstone component discovery and diagnostics while the
player wears the goggles. Shift + right-click with the goggles in the hand opens
the charging menu. Each redstone dust fuels 60 server ticks (3 seconds), maximum
64 dust (3m 12s). Press V to toggle, spending one fuel tick per active tick.

## Build
Run `gradle clean build` from this directory; expected output:
`build/libs/redstonevision-0.2.0-alpha.jar`.

## Rendering limitations
The transparent terrain hook is EXPERIMENTAL. It targets chunk render layers and
does not provide reliable block-selective 70% opacity. Shader and renderer mod
compatibility remains untested. No gameplay run has been completed. Java classes
are separated by common, server, client, rendering, network and menu concerns.

## Tests after building
- Start a clean client with NeoForge and no other mods.
- Check goggles and charging menu, including Shift+click and closing with dust inside.
- Load exactly one redstone, enable vision and confirm 60 server ticks usage.
- Remove helmet, change dimension, disconnect/reconnect, charge and re-equip.
- Build hidden circuits and compare their highlights and terrain with and without shaders.
- Dedicated server should load without client classes being referenced.
