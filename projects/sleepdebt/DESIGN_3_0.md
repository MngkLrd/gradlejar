# Restless Nights — Sleep Debt 3.0 creative / technical redesign

## Player fantasy
Sleep is a resource to manage, not a button to press at sunset. An expedition can
leave you physically exhausted even when well rested. Nighttime stress can ruin
a good bed, and stimulants can delay symptoms but never fix chronic sleep loss.

## A single gameplay loop
1. Travel / fight / mine -> physical fatigue + danger stress.
2. Stay awake -> persistent sleep debt and an escalating circadian burden.
3. At night -> faint visual hallucinations first, then genuinely hostile Sleepless.
4. Prepare a campsite with shelter, warmth, incense, pillow and protective artifacts.
5. Perform an uninterrupted seven-second short rest or commit to a normal full night.
6. Consult the journal: see your state, previous sleep quality and main disturbance.
7. Recover, plan the next expedition, craft supplies from living world resources.

## Design rules
- **No stat that players cannot influence.** Every negative pressure has a practical
  countermeasure, from changing location to creating light or sleeping properly.
- **No arbitrary screen takeover.** Hallucinations use client-only render models;
  no invisible server mobs, forced teleports or fabricated multiplayer entries.
- **Risk rather than punishment.** Microsleeps are brief and configurable.
  Fatigue levels must not make mining or combat permanently impossible.
- **Minecraft-native visuals.** Vanilla font, hard pixels, legible spacing,
  accurate model geometry/collisions, restrained warm copper/umber palette.
- **No mandatory visual or recipe libraries.** JEI/EMI/Jade/Patchouli/Accessories
  remain optional, useful for modpack integration rather than build prerequisites.
- **World compatibility first.** Preserve mod ID, block/item registry IDs, NBT key.

## Module contracts
- SleepEngine: state, migration, action-based fatigue, tick and wake recovery.
- RestEnvironment: one bounded environmental scan per rest/inspection, quality score.
- CampRestSessions: interruption/cooldown server sessions, no NBT sessions.
- SleepEffects: temporary vanilla status penalties, one place to balance them.
- NightEncounters: server mob spawns; light/loaded chunks/protection respected.
- MiragePlayers: client-only world-space hallucinations (unchanged in 3.0 alpha).
- SleepReportPayload/JournalScreen: on-demand server snapshot, pure client UI.

## MVP balance
- Short rest: 140 ticks of stillness, cooldown 2400 ticks, clears fatigue/stress,
  can lower at most 85 debt.
- Full sleep: base recovery 550 × quality/100, short sleep capped at 180.
- Quality: 35–150. Shelter/warmth/privacy reward preparation.
- Coffee: masks symptoms for fixed durations; tolerance grows and rebound can follow.
- Incense: slow area calming for groups, persists in block entity.
- Natural biome temperatures/rain and modded plants work without direct dependencies.

## Next milestones after alpha
- 3.0 closed test: dedicated server, client, GUI scale tests, crash log fixes.
- 3.1 dream sequences with non-blocking procedural scenes and original audio.
- 3.2 abandoned campsite worldgen, biome-dependent sleepers.
- 3.3 configurable sleep schedules, optional Comforts integration and datapack rules.
- 1.0 quality gate: save migration tests, profiler, visual QA, localization completeness.
