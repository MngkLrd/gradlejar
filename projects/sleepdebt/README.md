# Sleep Debt: Restless Nights — 3.0.0-alpha.15

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
Result: `build/libs/sleepdebt-1.21.1-3.0.0-alpha.15.jar`.
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

## Alpha 9: Hunter's Shadow and vanilla art
- The real Sleepless has more reliable loaded-chunk nighttime spawn attempts (every 200 ticks, 1/3 chance, multiple candidate sites), still at 830+ debt by default and one hunter per area.
- New client-side Sleepless apparitions at 570+ debt stand 19-30 blocks away, mostly behind the player. They last 6-10 seconds and vanish after ~11 ticks of focused gaze; separate from real AI and hitboxes.
- Real Sleepless stalks and flanks more slowly, pauses under gaze, has its own periodic isolated soul-sand steps and black smoke.
- Successful blows apply much longer darkness, slowing, and on alternate hits brief blindness/weakness and new horror sound cues.
- A distance-dependent darkened edge vignette grows as the real Sleepless approaches; optional client config.
- A death grants a configurable 3600-tick (3 minute) grace from hunting. Existing persistAfterDeath semantics remain, but Fatigue/Stress fall somewhat on respawn.
- Diagnostics: /sleepdebt debug why_no_sleepless and /sleepdebt debug spawn_sleepless.
- A full vanilla-art pass redoes 45 original 16x16 material/item/crop sprites and HUD wood frame, respecting the source PNGs' alpha/material roles. (See ART_ALPHA9.md).
- New client config: sleeplessApparitions, apparitionMinDebt, sleeplessNearVignette.
- Java 21, NeoForge 21.1.x, Minecraft 1.21.1.


## Alpha 10: The Pursuit, stronger fog and vanilla HUD
- Distant non-interactive Sleepless hallucinations pause, then slowly stalk from behind without being added to world entities.
- Actual hunter has a faster nocturnal encounter cadence (160 ticks), lower initial threshold (760 debt), and stress-adjusted probability.
- Genuine attacks now have three distinct beats: long darkness and slowing; periodic blindness; periodic weakness/mining fatigue. Attack gap is at least 32 ticks.
- A hostile hunter within 11 blocks creates ambient dread; within 7 blocks, darkness pulses before physical attacks. Darkness and stress persist across multiple strikes.
- Stronger, pixel-aligned peripheral dark smoke overlay reacts to the real hunter's distance.
- Hand-designed, vanilla-oriented oak HUD panel in native 158x68 PNG format with separated labels, custom corners and intentional wood grain clusters. The original 45 item/block texture pass from Alpha 9 remains intact.
- Death grants 3600 ticks of protection, including for previously active hunters. Existing NBT, recipes, block IDs and optional mod integrations are retained.
- Real nightly encounters still require a dark loaded area and can be blocked by nearby dreamcatchers.


## Alpha 11: Gaze, death cleanup, and horror polish
- Fixed the cancelled gaze counter on client-only apparitions. Sightings stay
  visible for at least 1.8 seconds before dissolving under direct observation.
- Ghosts are more often behind the player, occasionally near peripheral vision,
  and never appear during daylight, even at high stress.
- New server config defaults to persistAfterDeath=false; previously saved server
  TOML files still honor their saved value. Set it to false for debt reset.
- Real hunters are dismissed at the death site or on successful bed entry;
  respawn still grants 3600 ticks of protection. Vanilla bed rules remain.
- Hunter attacks use variable post-hit pauses, prolonged darkness, smoke and
  thicker distance-dependent vignette. Spawn probes no longer load chunks.

## Alpha 12 — The Unwelcome Shape (adult horror art and sound pass)

- Complete 256x128 authored-for-UV skin and a radically altered skeletal silhouette:
  asymmetrical elongated skull, independent open jaw, torn chest, exposed ribs,
  broken elbows/knees, extremely long fingers and breathing/twitch animations.
- Rendering scales the creature vertically. Its collision box was updated to
  0.77 x 2.75 blocks; natural spawns now require 3 blocks of clear headroom.
- **Custom black-violet fog** via NeoForge ViewportEvent.ComputeFogColor and
  RenderFog. Fog reaches around 12 blocks at maximum threat, keeping the player
  able to navigate while shrouding the surrounding world. No vanilla Darkness/
  Blindness status effect is used by the Sleepless itself.
- 9-layer dynamic black vignette and localized hit pulse; server gameplay remains
  separate from client-only visual and audio effects.
- Four real CC0 audio recordings (breathing, whispers, growl, strike) remixed into
  OGG at build time from pinned, verified GitHub raw files plus one original
  synthesized low heartbeat. Provenance and links: AUDIO_CREDITS.md.
- Two optional client settings: sleeplessHorrorAudio and sleeplessFogStrength
  (0 to 1.5). Existing sleeplessNearVignette toggle now covers fog and vignette.
- Build prerequisites in addition to Java 21/Gradle: Python 3 + ffmpeg + network
  access to pinned CC0 sources on first build. CI will package all OGG files into
  the JAR, so **end users install only the JAR** and require none of these tools.
- Existing mod ID, saved NBT schema, item IDs, recipes and optional integrations
  remain unchanged. Manual in-game visual/audio QA required.

## Alpha 13: horror audit corrections
- Fixed UV mapping: face, void eyes, jaw and open chest now paint the actual FRONT
  faces of the new model rather than the adjacent side of the cube.
- Added three high-intensity CC0 recordings from documented Freesound creators,
  with reproducible pinned blobs and OGG processing: demonic throat rattle,
  screamed strike and isolated skitter steps. See AUDIO_CREDITS.md.
- Every sourced sound now uses carefully tuned filtering/pitch and optional echo.
- Reduced predictable audio looping: throat sound arrives infrequently and from
  behind, footsteps vary and sometimes fall silent; scream is only on every
  fourth successful strike. Removed the recognisable vanilla soul-sand step.
- Fixed client config bug: disabling sleeplessNearVignette no longer silently
  disables sleeplessHorrorAudio. Disabling it does disable both fog and overlay.
- Server/player state, recipes, blocks and mod ID unchanged. Gameplay and sound
  volumes need an in-client QA pass before release.


## Alpha 14: Active Sleepless and coherent cluster remaster

- Replaced the ineffective Zombie melee and targeting goals with an independent
  five-stage Sleepless controller: WATCH, STALK, FLANK, PURSUE, RUSH.
- Debug spawn now explicitly targets the invoker, even in Creative for
  demonstration. Damage only works in Survival/Adventure. Natural spawns
  still respect death grace, debt, night/day and survival mode.
- Watching a hunter freezes it briefly, never indefinitely. It can accelerate,
  pursue, approach from the flank and relocate to valid loaded dark ground
  behind the player after poor pathing or a long failed rush. No force-loaded
  chunks, blind teleports into walls, daylight hunts or spawn-kill loops.
- Stronger native world fog: visibility can fall to around 6.5 blocks on
  maximum threat, with an 11-ring custom dark vignette and hit response.
  No use of vanilla Darkness for the hunter's approach.
- Remastered every non-Sleepless texture: 15 detailed block materials,
  8 crop stages, 22 original item icons, and the HUD/Journal/Coffee GUIs.
  A uniform cabinet palette connects cloth, walnut, brass, parchment and
  oxidized copper without texture noise. The real mob's 256x128 skin and
  the Alpha 13 sound bank remain unchanged.
- The complete 29-item catalog remains in its own Sleep Debt creative tab,
  now positioned before Tools & Utilities for easier discovery.
- Legacy generators run first; the new tools/remaster_assets.py deterministically
  rewrites the reviewed non-mob art during Gradle. Saves, recipes and registry
  IDs remain stable. Java 21, Gradle 9.2.1, NeoForge 21.1.x, Minecraft 1.21.1.


## Alpha 15: Black Void shader and full-resolution Sleepless skin

This release replaces vanilla fog-driven rendering with a standalone full
screen fragment shader registered through NeoForge. The shader paints the
already-rendered world, under the HUD. It uses animated black flow,
distorted filaments, narrow central visibility and a near-total impact pulse.
There are no RenderFog/ComputeFogColor handlers or standard Darkness potion
effects from the real Sleepless.

The monster now has an independently painted 2048x1024 RGBA anatomical atlas.
The Java model still declares 256x128 UV coordinates: each of those UV
units now samples 8x8 physical pixels. The art depicts cavities, broken
bone, pale collagen, damaged teeth and exposed ribs, not vanilla textures.

Client settings sleeplessNearVignette, sleeplessFogStrength and
sleeplessHorrorAudio remain usable. Audio bank unchanged from Alpha 14,
including all 8 licensed/independently synthesized OGG samples.
Shaderpack conflicts and the current F1 GUI-layer visibility remain known
runtime limitations. Automated CI cannot evaluate the fear factor or verify
shader reload behavior in a live client.
