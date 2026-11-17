# Lure Lantern 1.3.6 | NeoForge 1.21.1

**The original 1.3.2 item capture, recipes, lure logic and mob NBT format are preserved.** This source rebundles all original 1.3.2 game classes except `LureLanternBlock.class` (replaced in 1.3.3) and adds the 1.3.3 drop handler and client-side entity preview. Version 1.3.6 completely redraws the four block textures to the original Blockbench Minecraft Style Guide, in native 16x16 vanilla texture resolution.

## Textures

- `lure_lantern.png`: muted violet base form.
- `lure_lantern_active.png`: blue-green lure mode.
- `advanced_lure_lantern.png`: translucent forest-green glass to reveal the saved mob.
- `advanced_lure_lantern_active.png`: translucent warm amber active variant.

Hand-designed pixel clusters in a limited iron + colored glass palette. No AI rescale, anti-aliasing, pillow shading, dithering or pixel noise. Reuses vanilla lantern/hanging lantern geometry and the existing item transforms.

## Compile

Java **21**, Gradle **9.2.1**, NeoForge **21.1.262**. Run `gradle clean build`. The generated mod JAR is `build/libs/lurelantern-1.3.6.jar`.

`libs/lurelantern-1.3.2.jar` is the exact original June binary; SHA-256 is verified before compilation. Without the bundled jar, Gradle attempts to retrieve the same original release via CurseMaven.

## In-game checklist

1. Check item in hand/inventory and both placed/hanging lanterns, active and inactive.
2. Lure neutral mobs in hand and on ground.
3. Capture cow in advanced lantern with Shift + right-click, inspect its tooltip.
4. Place and leave/rejoin; verify the captured mob still exists inside the lantern.
5. Break in Survival: exactly one advanced lantern should drop with the mob saved.
6. Place the dropped lantern, release its mob, verify NBT.
7. Repeat for Creative and explosion breaks.
8. Confirm entity preview, visibility through tinted glass and no render crash with modded mobs.
9. Verify recipes, Minecraft/NeoForge startup and optional JEI integration.

**Limitations:** This package is build-verified when CI succeeds, but not gameplay-tested in Minecraft by this environment. Mob previews are static snapshots, and some unusual modded entities may not render. Per-mob body tint is not added in 1.3.6.

## NeoForge GameTest
This source includes a dedicated-server integration test. Run `gradle --no-daemon runGameTestServer` (Java 21). Test exercises dropping and re-placing an advanced lantern with cow NBT. The test class and structure are excluded from distribution JAR. CI also compiles the mod; client rendering still requires a manual visual test.

## 1.3.6: Advanced Lantern modes and preview

**Advanced Lantern**: right-click an empty placed lantern (empty hand or with an item) to cycle:

1. **OFF**: no lure, no capture, light/textures of inactive lantern.
2. **LURE**: attract friendly mobs, but NEVER capture them automatically.
3. **LURE + AUTO-CAPTURE**: lure mobs and capture the nearest eligible one; the lantern stops after capture.

Shift+right-click on an occupied placed advanced lantern releases the creature, exactly as before.
Right-click while occupied shows the animal's name. Basic Lantern still has its two states.
Held lantern's original capture and lure mechanics remain unchanged.

The selected mode persists as `LureMode` (0/1/2) in BlockEntity NBT. Old saves that
contain no `LureMode` default to auto-capture if their advanced lantern was active.
Original `CapturedMob` data is not migrated or rewritten. Removing/placing the block
uses the existing item NBT transport.

**Client preview**: rotates smoothly at ~2 degrees per tick, fits a max 0.27×0.30 block-space
box, centered differently for standing/hanging lanterns, with a tiny emissive pixel aura:
green for farm animals, blue for villagers, lilac for tame animals and gold for other mobs.
Preview entities are client-only visual objects. Each captured mob is deserialized only when
its preview data revision changes; no extra server entities are spawned.

### Automated integration tests

`gradle runGameTestServer` validates captured-cow drop/replace/release, the new
advanced modes and NBT round-tripping. No GUI/shader validation is possible on
headless GitHub Actions; manually check centered 3D preview through glass.
