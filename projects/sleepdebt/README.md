# Sleep Debt: Restless Nights — 3.0.0-alpha.8

**Minecraft:** 1.21.1 | **NeoForge:** 21.1.x | **Java:** 21 | **Gradle:** 9.2.1

This is a major redesign of the Sleep Debt project, not another pack of consumables.
It uses one coherent survival loop: effort -> fatigue, danger -> stress, waking -> sleep debt,
preparation -> rest quality, sleep -> recovery, and sleep deprivation -> night threats.

## What changed
- **Server-authoritative session-based camp rests.** Using a bedroll starts a seven-second
  stillness challenge. Walking away or taking damage interrupts it. Completion restores
  fatigue/stress and a small amount of sleep debt. Two-minute cooldown prevents abuse.
- **One environmental quality model** shared between night sleep and camp rest.
  Shelter, threat, rain, biome temperature, warmth, dreamcatchers, incense, pillow,
  mask, caffeine and stress alter the quality of recovery.
- **Work causes real fatigue:** breaking blocks, fighting, sprinting, swimming and nearby
  danger have distinct effects. No routine world searches every game tick.
- **Proper Sleep Journal screen.** Full-resolution 228x188 vanilla-font pixel-art interface
  with synchronized sleep/fatigue/stress values and last night's quality and cause.
- **New restrained survival HUD**, 138x58 pixels with pixel-art frame and native text.
  Coffee Maker GUI now uses the correct native texture UV dimensions.
- **Modular logic:** RestEnvironment, CampRestSessions, SleepEffects,
  NightEncounters, SleepEngine. All gameplay is server-side, hallucinations client-only.
- **Caffeine** masks sleepiness, builds tolerance, risks crashes. Milk and teas now
  primarily ease fatigue and stress and only slightly affect accumulated sleep debt.
- **All integrations are optional:** JEI, EMI, Jade, Patchouli and Accessories.
  If installed, supported features activate as usual. No hard dependency on GeckoLib.
- Existing farming, brewing, camp equipment, Sleepless mob, dreamcatcher,
  world-space Steve/Alex hallucinations and field guide remain available.

## Compatibility and migration
Original mod ID remains `sleepdebt`, the saved player NBT key remains
`sleepdebt_v2`, and existing registered block and item IDs are preserved.
New quality data stays in the same NBT compound. The network protocol changed to 4,
so **the client and server must use the same 3.0 version**.
Back up worlds before upgrading: runtime compatibility with the older 2.6 series
must still be verified.

## Install
Install Minecraft 1.21.1 NeoForge with Java 21.
Put the mod JAR in `mods/`. Optional integrations can be installed individually.
For a full modpack experience install JEI or EMI, Jade, Patchouli and Accessories.
The core mod does not require any of these libraries.

## Build
```bash
gradle clean build
```
Result: `build/libs/sleepdebt-1.21.1-3.0.0-alpha.8.jar`.
`src/main/resources/assets/sleepdebt/textures` are generated deterministically
by Python scripts during the Gradle resource-processing task.

## Quick tests
```mcfunction
/sleepdebt status
/sleepdebt showcase
/sleepdebt debug set 930 800 880
/give @s sleepdebt:sleep_journal
/give @s sleepdebt:camp_bedroll
```
In Survival, right-click the bedroll and remain still for seven seconds.
Move or take damage to test cancellation. Test in rain/cold/under a roof and compare
sleep scores via the journal. Check both EN and RU locale at multiple GUI scales.

## QA limitations
GitHub CI validates Java compilation, jar packaging, models, textures, guides and
translation JSON. It **does not execute Minecraft gameplay**. Native GUI appearance,
client world rendering, multiplayer and optional-mod startup combinations require
manual in-game checks before declaring this a stable release.

## Alpha 2: predictive planning
The sleep journal now shows both last sleep quality and an on-demand forecast of
the current location, including environmental warnings. Repeated full-bed sleeps
within 6000 ticks are capped at 90 debt recovery to deter sleep spamming.
Core compatibility flags for shelter/hostile penalties are respected in quality calculation.

## Alpha 3: Minecraft Phantom integration
Vanilla phantom spawn attempts now respect Sleep Debt (under 250 = deny),
and a dreamcatcher in a nearby camp prevents an attempt. Extremely sleep-deprived
players may see one extra phantom (never more than four in the requested group).
Minecraft's insomnia gamerule, skylight and natural spawn checks still apply.
The new vanilla.integratePhantoms setting disables this integration.
This uses NeoForge PlayerSpawnPhantomsEvent, not invasive mixins.

## 3.0 Alpha 4 — Visual Overhaul
New reviewed block/item GUI artwork and the 128x64 Sleepless atlas are now version-controlled. Six blocks have per-face UVs, lantern translucent glass, dreamcatcher cutout net, and eight redesigned crop stages. See VISUAL_V4.md. Existing block/item IDs and gameplay NBT are unchanged.


## Alpha 5 detail pass
The Dreamcatcher frame is now a stepped octagon with separate open mesh and three independent suspended feathers. The older square silhouette was removed after visual model preview.


## Alpha 6: Complete item icon remaster
Every one of the 22 item textures has an individual 16x16 pixel-art sprite, including all coffee variants, teas, milk, herbs, dream materials, mask, pillow, poultice, journal and incense. The existing 16x16 texture names and item model references are preserved for save/resource-pack compatibility.


## Alpha 7 | Cluster Discipline Art Pass
Replaced 45 authentic 16x16 PNGs: 15 block materials, 8 coffee crop stages and 22 item icons. Rebuilt clusters and broken organic edges, focused material textures, corrected shading on fabrics/pillows, controlled banding and tile rhythm. The exact authored PNG files are committed in assets/sleepdebt/textures; old generators cannot overwrite them. A new PNG/palette QA test runs against every CI-built JAR. See ART_DIRECTION_V7.md.

## Alpha 8: Dedicated Creative Tab + fast testing
- Added a separate Creative Mode tab **Sleep Debt**, next to vanilla Food & Drinks,
  featuring all 29 registered inventory items in a deliberate gameplay order.
- Retired legacy injection into vanilla Creative tabs. Existing item registry IDs,
  NBT and saves are unchanged.
- Added OP-only test presets:
  - `/sleepdebt debug preset rested` = 0/0/0
  - `/sleepdebt debug preset tired` = 450/350/200
  - `/sleepdebt debug preset exhausted` = 780/700/650
  - `/sleepdebt debug preset critical` = 960/900/950
- Existing fine-grained command: `/sleepdebt debug set <debt> <fatigue> <stress>`.
- `/sleepdebt status` additionally displays the predicted rest quality here.
- Creative mode bypasses the per-tick fatigue simulation. For an actual
  test set `/gamemode survival`, then use the presets.
- Optional vanilla accelerated tick speed: `/tick rate 100` and restore
  `/tick rate 20`. Increasing TPS affects the whole world; presets are safer.
- To test the mob/hallucinations in a loaded open space at night:
  `/time set midnight`, `/sleepdebt debug preset critical`.
- For visual block test: `/sleepdebt showcase` (OP, needs suitable ground).
