# World Logic 1.0.0-rc22 — NeoForge 1.21.1
Java 21, official mappings, NeoForge ModDevGradle 2.0.148, Gradle 9.2.1, compile/test loader 21.1.262.
Build: gradle --no-daemon clean build
Output: build/libs/worldlogic-neoforge-mc1.21.1-1.0.0-rc22.jar
Install the same build on client/server. API v1: [API.md](API.md).
Full RC20 source port of World Logic, not a feature subset. Same registry IDs, config rules, world preset IDs, climate equations and exertion storage keys. NeoForge payload protocol 7, NeoForge-specific tick and damage event adapters. Damage consequences use final post-mitigation damage; shield hooks require actual block eligibility.
New native HUD: ten 9x9 energy sprites, full/half/empty, placed using NeoForge Gui.leftHeight after vanilla armor/health, reserving a 10 px row. No numeric label. Exertion disabled/creative/spectator hide HUD.
QA: enrolled NeoForge server scenario validates core calculations, tags, live world support, API fatigue/recovery/jump and fallback environment, stamina icon quantization, and actual registered custom preset's sampler and base heights on two seeds. The common runner is a flat world; preset tests are sampled, not a fresh custom world startup or graphical HUD test. No network-player backend for this modded protocol. Other gameplay parity, existing save migration, and graphical HUD remain unverified. Use new worlds or backup before migrating from Forge.

RC21: additive atmospheric snapshot and executable dry/rain checks. See [API.md](API.md) and [approved subsystem ownership](ARCHITECTURE.md). No terrain, registry, save-format or existing rule behavior changes.

RC22 adds an experimental continuous regional weather field (pressure, advected thermal/moisture waves, pressure-gradient wind, cloud and precipitation potential) and `/worldlogic weather`. The preview does not drive real rain, snow, thunder, player thermoregulation, or visuals. Existing live RC21 atmosphere remains unchanged. Server QA includes deterministic field, continuity, climate dependence, bounds, live isolation and command output.

RC22 research extension: physically informed parcel-column diagnostics (water vapor, dew/wet-bulb, dry/moist adiabatic cooling, condensation budget, stability and fog proxy). No live weather change. Existing two-seed terrain regression split into two server commands to avoid an observed RCON timeout; both seed surveys and height checks retained.

RC22 active recovery: 2.4 fatigue points/s walking, 3 sneaking, 4 standing; one-second post-effort cooldown, hunger reduces recovery. Actual position delta distinguishes standing and walking; no recovery while airborne, swimming, riding or climbing. Weather checks use force-loaded open coordinates and return explicit failure diagnostics.

RC23 research: new world preset `worldlogic:earthlike_extended` (-256..511, sea 63) with deterministic geological provinces, local cave systems, surface shafts, static water levels and eight ore families. Old presets untouched. New preset disables vanilla noise caves, carvers and standard ore clusters; custom chunk generation only. Full new-world and client validation pending.

Extended-world cloud height is configured at Y440 through a client-only NeoForge dimension effects registration. The old preset's cloud height is unchanged. The client renderer remains unverified.

RC24: one selectable World Logic Realistic preset (`worldlogic:realistic_unified`), 51 new Overworld biome classes, original RC22 upper density and climate, RC23 geology and extended underground, clouds Y440. Old registry IDs retained. Terrarian supplies plants; without it vegetation is intentionally absent. Initial prototype, real new-world and addon checks pending.

RC25: smaller regional deposits, frequent small shallow and deep ore pockets, improved karst galleries and lake chambers; same unified Realistic preset, no terrain/biome changes. Existing chunks are not regenerated. Use a new world for a uniform ore distribution. Full generated-world visual and economy checks still pending.

RC26: 61 custom biomes, 10 refinements of RC24 climate intervals; WorldLogic API revision 4 includes an immutable worldgen habitat sampler for Terrarian. Temperature, moisture, soil fertility/dryness/waterlogging/coldness, generic suitability, zone/surface family, slope and wind exposure; no chunk reads or live weather. Existing RC22 terrain and RC25 geology unchanged. See BIOME-ECOLOGY.md and API.md. Full addon and generated-world checks pending.

RC27: regional_weather defaults on for World Logic geography presets. Nine local weather types with synced visual rain/snow, dust, fog and temperature impact. `/worldlogic weather_local` shows the current weather at command position. `/worldlogic rule regional_weather false` restores legacy weather. Temperature API revision 5 includes localWeather; existing environment and atmosphere already include changes. No world recreation needed. Full client and addon QA pending.

Operator test: `/worldlogic weather_demo dust_storm` (or `monsoon`, `blizzard`, `thunderstorm`, `fog`, `downpour`, `rain`, `snow`, `clear`); `stop` ends test. Limited to local radius 128 and 120 seconds; actual API follows demo. No real lightning entities yet.

RC28: five climate-related soil blocks with original 16px textures, groundwater/snow/soil/pond storage, precipitation, infiltration, melt, evaporation, recharge, discharge and runoff; `/worldlogic hydrology` displays current modeled values. API revision 6 supports future Hydration. New ground blocks only in newly generated chunks, no terrain density changes. Existing worlds can use live model. Full actual world/client QA pending.

RC28 API revision 7: detailed water origin, gameplay quality, salinity/contamination estimates, evidence and source/flow flags. Rivers/springs drinkable by game balance, sea/estuary desalination, other freshwater treatment recommended. Legacy method retained. Natural origin inference is provisional; no dedicated spring generation or fluid provenance tracking yet.
