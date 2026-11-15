# Sleep Debt: Restless Nights — 3.0.0-alpha.1

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
Result: `build/libs/sleepdebt-1.21.1-3.0.0-alpha.1.jar`.
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
