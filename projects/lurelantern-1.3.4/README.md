# Lure Lantern 1.3.4 - Minecraft 1.21.1 NeoForge

Original 1.3.2 mechanics are preserved by rebundling the SHA-256 verified public release.
Only the lantern block class is recompiled, alongside a NeoForge drops event handler and a
client-only spawner-style preview. The placed advanced lantern has a translucent body.

Java 21, Gradle 9.2.1. Run `gradle clean build`.
Output: `build/libs/lurelantern-1.3.4.jar`.

Tests: in hand lure/active toggles, advanced capture, placement with NBT,
restart and inspect mob, survival and explosion breaks, creative break,
one preserved mob per lantern, sneak-click release, vanilla and modded mob render.
A Gradle build verifies compilation, NOT gameplay; manually smoke-test in Minecraft.

## Art: 1.3.4
All four texture atlases were redrawn as true 16x16 PNGs in the original vanilla layout using a compact four-material palette. The Advanced lanterns have translucent central panes; original Java capture logic is untouched.

Follow Blockbench Minecraft Style Guide: https://blockbench.net/wiki/guides/minecraft-style-guide/

This release is CI build-verified, not yet tested in a real Minecraft client.
