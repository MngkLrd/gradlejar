# NON-NEGOTIABLE OWNER REQUIREMENT: RC22 GEOGRAPHY MUST NOT REGRESS

Preserve RC22 Earthlike Climate surface relief, coastlines, continents, mountains, plains and drainage. Do not amplify, flatten, reroll or replace its terrain noise to add biomes. New biome classes partition the same climate fields. Compare exact base height at identical coordinates and seeds against the immutable RC22 preset; intentional local cave mouths/soil features are separately reviewed. Do not remove old registry IDs needed by existing saves. This rule overrides older plans to amplify mountain height. Core owns geography, climate, biome classification, geology, hydrology and animals; Terrarian owns plant generation (trees, grass, shrubs and other flora). One selectable Realistic preset for new World Logic worlds, with legacy IDs retained for old saves. Full generated-world and client checks remain required before release-quality claims.

# World Logic API v1 (RC20 baseline, RC21 additive revision)

Minecraft 1.21.1 / NeoForge 21.1.80+ (CI baseline 21.1.262) / Java 21 / official mappings.

## Dependency

Put the verified core JAR in the addon's `libs/` folder. This project uses official mappings and NeoForge 21.1; for the same configuration use `implementation files('libs/worldlogic-neoforge-mc1.21.1-1.0.0-rc20.jar')` in the addon's Gradle dependencies. Do not shade or duplicate the core classes. Install the core and addon in the runtime mods directory; do not install duplicate core JARs. If using a Gradle workspace, choose either the file runtime dependency or an equivalent project dependency. Verify runClient/runServer classpath separately.

Add to addon META-INF/neoforge.mods.toml (replace your_addon with actual mod ID):

```toml
[[dependencies.your_addon]]
modId="worldlogic"
type="required"
versionRange="[1.0.0-rc20,)"
ordering="AFTER"
side="BOTH"
```

The public Java entry point is `dev.minemods.worldlogic.api.WorldLogicApi`, API_VERSION=1. Verify the API version when developing new integrations; only this package is the supported addon contract. RC20 requires matching NeoForge core client and server (payload protocol 7). Do not combine Forge and NeoForge binaries.

## Temperature / hydration / health

On the authoritative server thread, sample once per second or slower per player, reuse the immutable snapshot within the tick:

```java
import dev.minemods.worldlogic.api.WorldLogicApi;
import net.minecraft.server.level.ServerPlayer;

void update(ServerPlayer player) {
    var env = WorldLogicApi.environment(player.serverLevel(), player.blockPosition());
    if (env.isEmpty()) return;
    double airC = env.get().ambientCelsiusEstimate();
    boolean sheltered = env.get().covered();
    var fatigue = WorldLogicApi.fatigue(player);
    // Apply your own body's thermal/hydration model and own server->client sync.
}
```

| Method | Contract |
|---|---|
| environment(ServerLevel, BlockPos) | Optional immutable snapshot of loaded location; no chunk generation. |
| Environment.ambientCelsiusEstimate() | Approximate air °C, no body/clothing/heat-source model. |
| Environment.elevationAboveSea() | Blocks relative to dimension sea level. |
| Environment.covered() | No direct sky visibility, not proof of enclosed insulation. |
| Environment.rainingAt(), globalThunder() | Actual vanilla weather state. |
| Environment.solarExposure() | Approximate 0..1 daytime/sky/weather factor. |
| Environment.regionalClimate() | Optional, present on new RC19+ Earthlike Climate worlds; absent elsewhere. |
| RegionalClimate.thermalIndex() | Unmodified physical climate index -1..1, not biome selection temperature. |
| RegionalClimate.moistureIndex() | 0..1 modeled climate moisture, NOT relative humidity. |
| RegionalClimate.absoluteLatitude() | 0..90 modeled absolute latitude. |
| RegionalClimate.prevailingWindX(), prevailingWindZ() | Dimensionless prevailing direction components, not wind speed. |
| RegionalClimate.modeledSurfaceY() | Modeled terrain elevation, not actual block height. |
| fatigue(ServerPlayer) | enabled, fatiguePercent 0..100, remainingStaminaPercent=100-fatigue. |
| addExertion(ServerPlayer,double) | Add finite 0..100 exertion units, core movement multiplier applies; respects rule and mode. |
| recoverExertion(ServerPlayer,double) | Recover finite 0..100 units; respects rule and mode. |
| clientFatiguePercent() | OptionalInt for local client; absent before sync or if disabled. Do not call on server. |
| itemWeight(ItemStack), entityMass(Entity), materialFamily(BlockState) | Core relative gameplay material/weight helpers, not SI units. |
| ruleEnabled(String), ruleStrength(String) | Core rule and tuning values; read on server for authoritative gameplay. |

## Temperature addon — responsibility boundary (owner direction, 2026-11-27)

World Logic Core is the authoritative provider of world/environment conditions: geography, hydrology, climatology, meteorology and ambient air temperature. Core must remain fully usable without Temperature or any other addon. Temperature depends on Core, never the reverse.

- Temperature owns player body temperature, heat exchange, perceived temperature, clothing insulation, exposure/wetness effects on body heat, sweating, overheating, hypothermia, physiological penalties and its own HUD. Local heat sources, water contact and insulation can refine the player's heat exchange; do not replace the core's regional climate/ambient air model.
- Consume `WorldLogicApi.environment(level, pos)` on the server thread. RC20 provides approximate ambient Celsius, sky exposure, rain/thunder, solar proxy and optional modeled regional climate. It does NOT provide relative humidity, actual wind speed, precipitation intensity, seasonal weather, indoor insulation or a complete heat-source model. Use explicit bounded fallbacks when data are absent; do not present invented values as core readings. Request missing shared environmental data from the Core developer.
- Core already has `cold_shock`, `wet_gear` and `wet_bow` rules. Do not silently duplicate their effects or overwrite persistent item wetness (`worldlogic_wet`). Temperature owns thermal consequences of wetness; physical gear/bow effects remain core for now. RC20 has no public item-wetness API or automatic rule arbitration: coordinate the integration and any overlapping cold-shock rule explicitly; do not silently edit server configuration. No core behavior changes are made by this documentation.
- Use Core's fatigue/addExertion/recoverExertion API when thermal stress affects fatigue; do not create a second stamina pool. Hydration is a future optional addon: Temperature should expose sweat/fluid-loss information through its own stable integration contract when available, without requiring Hydration or modifying thirst itself. Keep vanilla health authoritative and coordinate with Traumatic for medical consequences.
- Active target: Minecraft 1.21.1 NeoForge / Java 21. Integrate with the verified RC20 JAR; no Forge fallback. Verify standalone core, core + Temperature, and absence of duplicate penalties. The above ownership is a design contract, not evidence that the planned integrations are implemented.

## Ownership

Core owns fatigue, physics, environment and geography. Temperature addon owns body temperature, heat exchange, overheating and hypothermia; hydration addon owns water balance and thirst; health addon owns body-region medical mechanics. Do not maintain a second fatigue counter or duplicate effects. Existing core injury rules remain active unless explicitly configured off; no auto-disable/health arbitration in v1. Addons must coordinate disabling overlapping rules, not silently rewrite global config. Core owns fatigue HUD; add-ons own their own UI. Server sends fatigue percentage through NeoForge payload channel every 10 ticks on changes. Use separate addon networking for addon-specific HUD values. Do not interpret absence of regional data as zero humidity; choose an explicit fallback. Do not call the server API from render callbacks or async workers.

Climate estimate is deliberately approximate; seasons and instantaneous regional wind/precipitation simulation are not implemented. Existing RC18 worlds keep their generator. The API can sample local conditions there using biome fallback, without RC19 regional fields.

QA: the production server regression exercises environment sampling vs seeded router, unloaded location rejection, material weights, fatigue add/recovery/bounds/invalid input using NeoForge FakePlayer. No graphical HUD certification or external addon integration test yet.

## RC21 atmosphere API (additive, API_REVISION=2)

`WorldLogicApi.atmosphere(level, pos)` returns an optional immutable snapshot, on the server thread, for loaded positions in skylight dimensions. Use it instead of a separate environment() call to obtain both. Fields: `environment()` (unchanged RC20 record), `relativeHumidityPercent()` (0..100 modeled from approximate dew point, ambient temperature and weather; not the generator moisture index), `outdoorWindXMps()`, `outdoorWindZMps()`, `outdoorWindSpeedMps()` (deterministic modeled outdoor wind, not measured fluid dynamics), `precipitation()` (NONE/RAIN/SNOW from vanilla weather and biome exposure), `precipitationIntensity()` (0..1 vanilla rain strength proxy; not mm/h). Covered locations have no direct precipitation; outdoor wind is NOT an indoor wind estimate. Non-skylight dimensions/unloaded positions return empty. Vanilla world fallback uses a simple precipitation-capable vs dry biome moisture prior. No regional storm system, seasons, heat sources or fluid/terrain changes. No change to existing v1 records or method signatures. Addons using the new method must depend on RC21 or later, after CI release; RC20 remains sufficient for existing API consumers.

[Approved subsystem ownership](ARCHITECTURE.md).

## RC22 regional weather preview (API_REVISION=3)

`WorldLogicApi.regionalWeatherPreview(level, pos)` exposes a separate experimental continuous field: sea-level/local pressure (hPa), air-temperature anomaly (C), moisture index, modeled cloud fraction, precipitation potential, pressure-gradient wind components and gust estimate (m/s). It is seeded, moves with server game time, samples loaded skylight locations only, and does not change generator or weather. Moisture here is not relative humidity; precipitation potential is not live rain intensity or calibrated probability. `environment()` and `atmosphere()` remain authoritative for Temperature and other gameplay addons. Do NOT apply preview anomalies, gusts or cloud values to body heat or water loss until the core explicitly integrates the regional controller with the live API and client rendering. `/worldlogic weather` shows live and experimental values separately. Existing RC20/21 API signatures and behavior are unchanged.

### RC22 vertical atmospheric diagnostics

`weatherColumnPreview(level,pos)` is an experimental parcel-column diagnostic for loaded skylight locations. It uses the regional weather anomaly/moisture field, station pressure, a parameterized vertical temperature profile, dry/moist adiabatic lapse rates and 100 m steps up to 4 km atmospheric height. Values: air/dew/wet-bulb C, vapor mixing ratio and cumulative condensate g/kg dry air, pressure hPa, environmental lapse C/km, condensation/cloud levels m AGL (-1 absent), positive buoyancy integral J/kg (not validated full CAPE), bounded cloud/precipitation/fog indices and a convective flag. Liquid-water Magnus approximation even below freezing; no ice microphysics. These values are not live weather and MUST NOT be applied by Temperature. `/worldlogic weather_column` exposes them for QA. See [meteorology design](METEOROLOGY.md).

Opt-in `modeled_weather` selects actual overworld vanilla weather from the spawn parcel column every 200 ticks. Default off in all presets, respects `doWeatherCycle`. Existing live API observes vanilla weather flags; regional anomaly previews are still not authoritative. No per-player local weather yet. `/worldlogic rule modeled_weather true` enables the controller. Stamina now recovers while walking at 2.4 points/s, sneaking at 3, standing at 4 after a one-second effort cooldown, with reduced recovery at low hunger.

RC22 transport grid preview: `transportGridPreview(level,pos)` is present only with modeled_weather enabled, a persisted grid, loaded position, and inside the bounded overworld domain. Pressure in hPa, air temperature in C, vapor mixing ratio g/kg, RH percent, last model tick. It is not authoritative player temperature; live API remains unchanged. `/worldlogic weather_grid` inspects the active model.

RC23 extended preset: `regionalClimate` recognizes `earthlike_extended`, reports amplified modeled surface height and adjusts thermal index for extra relief. Existing preset and public records unchanged. New geology is worldgen-only and no ore location is exposed via public API.

RC24: `realistic_unified` retains RC22 modeled regional climate/surface height. Stable biome tags `worldlogic:vegetation/{polar,tundra,wetland,boreal,grassland,desert,temperate_forest,scrub,tropical_forest,savanna,marine,coast,alpine,subterranean}` are the Terrarian integration contract; core generates no vegetation features for these biomes. New registry IDs under worldlogic namespace. Old API records unchanged.

## RC26 / API revision 4: Terrarian worldgen contract
`WorldLogicApi.worldgenSampler(WorldGenLevel level, ChunkGenerator generator)` or pure-state overload `(RandomState, RegistryAccess, ChunkGenerator)` returns `Optional<WorldgenSampler>` for the three World Logic geographic presets; unsupported generators return empty. Use once per feature batch. `sampler.sample(BlockPos pos, Holder<Biome> biome, BlockState ground)` receives the feature's already-accessible local biome and ground; no live weather, chunk queries, block reads, force-loading, thread assertion, level writes or mutable world state in sample. The immutable seeded model uses bounded concurrent caches. Do not retain across world changes/reloads. Example: `var sampler=WorldLogicApi.worldgenSampler(context.level(), context.chunkGenerator()); var habitat=sampler.map(s->s.sample(pos,context.level().getBiome(pos),context.level().getBlockState(pos.below())));` caller must check generation region bounds before reading blocks. Temperature C is a long-term estimate, moisture/dryness/waterlogging/coldness/fertility/environmentStrength/windExposure/riverbankPotential are 0..1 proxies (not measured RH, nutrients, soil chemistry, wind speed or proof of a river). Slope in degrees from ±16-block macro height samples; wind vector is prevailing direction. Biome zone and surface family are enums, UNKNOWN for unclassified ground/legacy biome, stable custom vegetation tags unchanged. `environmentStrength` is generic habitat suitability, not species suitability or biome membership confidence. Terrarian applies species-specific tolerances and local placement constraints. Existing live methods remain server-thread-only.

## RC27 / API revision 5: authoritative local weather for Temperature
Core owns local air temperature, atmospheric humidity, wind, pressure, precipitation, clouds, fog and solar attenuation. Temperature addon owns physiological heat exchange, insulation, sweating and body response. `WorldLogicApi.localWeather(ServerLevel, BlockPos)` returns Optional<LocalWeather> (WeatherKind CLEAR/RAIN/DOWNPOUR/THUNDERSTORM/SNOW/BLIZZARD/DUST_STORM/MONSOON/FOG, intensity, temperatureDeltaC, RH%, wind X/Z and gust m/s, cloud/fog/solar transmission indices, precipitation and exposed intensity, pressure hPa, exposed). Server thread, loaded positions only. Geographic presets only; `regional_weather` enabled by default, respects doWeatherCycle. `environment().ambientCelsiusEstimate()` already incorporates the weather anomaly and solar attenuation; NEVER add `temperatureDeltaC` again. `atmosphere()` returns the same local humidity, wind and precipitation, so existing Temperature consumers receive updated air conditions without a second weather simulation. Covered positions have no precipitation, but the same regional air mass; addon computes shelter/insulation separately. `Environment.globalThunder()` means local thunderstorm in regional mode, vanilla global flags otherwise. Legacy regionalWeatherPreview/column/transport remain diagnostics, not additive weather. The new local runtime is a gameplay front model, not a fully conservative atmospheric solver. The old grid is disabled in this mode. Rendering uses per-player packet, vanilla client rain/snow sheets and additional dust/snow/rain particles and fog. Other vanilla rain-dependent gameplay (e.g. cauldron filling, lightning ignition, crop hydration) is not yet regionally redirected; no actual lightning bolts, rain sound additions or independent per-column weather sheets. Temperature integration with real addon not tested.

RC27 operator-only `/worldlogic weather_demo <kind>` temporarily overrides the same authoritative local snapshot (radius 128, two minutes, tapered edge), `stop` clears it. Useful for client inspection; never affects generation or normal weather outside test zone, nonpersistent. Distant thunder audio and sky flash are client-side cosmetic, no destructive lightning entities. This is not a substitute for client QA.

## RC28 / API revision 6: hydrology and Hydration
Hydration is a separate addon: thirst, drinking, body water balance, dehydration and water treatment belong there. Core owns precipitation, natural water classification, soil saturation, runoff, snow and groundwater. `WorldLogicApi.waterSource(ServerLevel,BlockPos)` classifies the actual fluid as NONE/FRESH_SURFACE/SALINE/GROUNDWATER/UNKNOWN; **not a potability guarantee**, no chemical or pathogen model. `hydrology(level,pos)` is an optional 16x16 surface-cell water budget, stores in modeled mm water equivalent; `simulated=false` is a climate-based prior, not persisted observations. Live API server thread, loaded chunks only. `soilType(BlockState)` is pure worldgen-safe classification; five new soil blocks registered under worldlogic namespace and minecraft:dirt, mineable/shovel tags. Terrarian may use soil classification plus existing worldgenSampler, or live hydrology for plant response. No thirst, player penalties or duplicate physiology. Model rates game-calibrated, no direct fluid block flooding, intercell transport or full aquifer solution yet.

## RC28 / API revision 7: detailed source classification for Hydration
`WorldLogicApi.waterProfile(ServerLevel, BlockPos)` is the preferred method. Call on the exact fluid block before filling/drinking; the addon persists the returned kind/quality in its own container data. `WaterKind` SEA/RIVER/LAKE/SPRING/GROUNDWATER/WETLAND/POND/BRACKISH/NONE/UNKNOWN; `WaterQuality` DRINKABLE (river/spring in game balance), TREAT_RECOMMENDED (lake/groundwater/wetland/pond), DESALINATE (marine/brackish), UNKNOWN, NOT_WATER. `salinityGkg` and `contaminationRisk` are gameplay estimates, not real chemistry or real-life drinking advice; -1 unknown. `sourceBlock`, `flowing` describe vanilla fluid state, not river current. `WaterEvidence` distinguishes biome tag, terrain proxy, local geometry and unknown. Legacy `waterSource()` remains binary-compatible and projects the richer result. Ocean and estuary tags take precedence. Groundwater requires a covered water block and solid roof at least six blocks above; not ocean-floor height. Rivers use biome tags or broad surface water + RC22 drainage potential; springs use exposed rock-backed source and actual neighboring outflow. Lakes/ponds use bounded water extent. These are inference rules, not proven hydrologic origin; player-placed water may inherit classification. Dedicated spring generation, actual basin connectivity, pollution and source provenance are future work. No chunk generation, fluid replacement, thirst or drinking effects. Do not equate UNKNOWN to drinkable; boiling alone does not remove salt. `/worldlogic water` prints classification at command position.

Owner balance correction: lake and pond water are DRINKABLE by default, alongside river and spring. Roof classification uses solid geometry rather than potentially delayed sky-light updates.

## RC29 / API revision 8: registry-backed natural water
Eight source fluids `worldlogic:{sea_water,river_water,lake_water,spring_water,groundwater,wetland_water,pond_water,brackish_water}`, flowing IDs `flowing_<id>`, matching LiquidBlock and BucketItem `<id>_bucket`. Native NeoForge FluidType and fluid capabilities, common water tags and local names. `waterProfile(Fluid)` returns intrinsic kind/quality for tanks/buckets (no local soil); `waterProfile(ServerLevel,BlockPos)` prioritizes exact registry identity and applies local soil risk from bed and banks. Lake/pond/river/spring are drinkable by default; risk >=.65 can downgrade to TREAT_RECOMMENDED; salt never becomes drinkable due to soil. Hydration must snapshot positional quality before collecting if it needs to retain contamination; ordinary bucket preserves source kind only. Registry names and bucket tooltips support standard ingredient displays; actual JEI/Jade compatibility not yet tested and no dedicated Jade overlay plugin. New Realistic chunks replace standalone vanilla water after decoration; underground water uses depth below solid surface, surface water uses biome/drainage classification. Standalone spring/pond worldgen not independently generated, lake/river classification approximate. Existing chunks, waterlogged blocks, later vanilla/modded placement and ice melt remain vanilla compatibility paths; the vanilla registry ID is not removed. No whole-world destructive migration or global water replacement. No custom fluid mixing chemistry yet. Keep old world IDs and RC22 density intact.

## RC30 / API revision 9: environmental observation
`environmentalMetrics(level,pos)` is an optional server-thread snapshot for the current 16x16 cell. Air load, natural dust, combustion load, soil deposition are modeled 0..1 indices, not measured concentrations. Local observed burning blocks, player log/rock/soil removal and placements are aggregate counts, not causal player attribution. Water quality positional API adds local soil deposition to existing bed/bank risk. Pause menu opens analytics screen with atmosphere, water, soil, activity and history tabs, nearby 3x3 observed cells and up to 48 samples at 1200-tick intervals. Updates near players every 100 ticks, persistent maximum 4096 cells per dimension; unknown cells are not clean. Samples are near current player height, not full vertical or global survey; no advection, specific chemical pollutants, ecology counts, per-player attribution or offline simulation yet. No equipment needed. Client GUI and actual multiplayer interactions not yet verified.

## RC31 / API revision 10: Terrarian plant–soil integration
`WorldgenSampler.predictWater(waterPos,surfacePos,oceanFloorY,surfaceBiome,bed)` is a default method (preserves functional interface and binary compatibility). Supply actual or hypothetical water position, same column OCEAN_FLOOR_WG height (first-free-block convention), surface biome and bed state already available to caller. Pure prediction of the same kind used by RC29 conversion; does not determine water existence, or generate springs/ponds. In existing registered water, prefer `waterProfile(Fluid)`.
`applyVegetation(level,pos,new VegetationInput(demandMm,cover,rootRetention,litter))` is server-thread only. Aggregate all plants in a 16x16 cell into ONE call per 200 ticks. 0..4 mm demand per interval (256 L/mm per cell); normalized cover/root/litter. Returns Optional.empty for unsupported/unloaded/no soil/full budget, or VegetationResult with applied flag and actual consumed amount. Never withdraw more than accepted. Zero demand is valid for reporting no cover after removal. Root effect expires after 1200 ticks without reports. Roots reduce evaporation/recharge and raise infiltration/pond retention; litter modestly increases fertility, uptake decreases it. Transpiration removes water from soil but does not yet feed atmospheric vapor transport.
`irrigateSoil(level,pos,offeredMm,salinityGPerLiter)` reports water physically supplied by caller, 0<mm<=20, 0..100 g/L, one accepted report per cell per 200 ticks, returns accepted mm; source/container debit remains caller responsibility. Root-zone salt is conserved across uptake/evaporation, leached via recharge, export reported but no groundwater salt transport. Natural coastal salt intrusion not simulated yet. `soilEcology(level,pos)` provides salt mass, pore-water salinity, effective fertility, plant cover, root retention, latest and total transpiration, leached salt, cumulative positive cover recovery and last report tick (-1 unknown). `hydrology().fertility()` now reflects effective fertility. `forestObservation(level,pos)` independently observes canopy fraction (top leaf blocks of 256 columns), baseline, cumulative increases and tick. No claim of tree counts, regrowth attribution or ecosystem completeness. Analytics now has Ecology tab. Addon actual integration/client testing remain pending. RC22 density untouched.

RC31 follow-up: transpiration has a bounded, short-lived near-surface microclimate proxy (up to +8 relative humidity percentage points and -1.2 C, decays over 400 ticks) applied to exposed local weather; not a conservative atmospheric vapor solver. Hydrology picks one of five deterministic same-cell soil columns to avoid losing the entire cell to a central trunk or pond. It remains a cell-scale representative soil, not an individual root network. Canopy sampling only uses loaded observed chunks.

## RC32 addon ownership diagnostics
Exact addon IDs: `hydration`, `worldlogicterrain`, `worldlogic_thermal`, `worldlogic_trauma`, `wl_treechopper`. `/worldlogic addons` reports presence and ownership. Hydration has no duplicate thirst mechanic in Core; keep `hydrology` enabled and preserve all water APIs. Terrarian uses the habitat, water prediction, vegetation and soil ecology APIs; preserve those and Core crop-fertility fallback (Terrarian current registered handlers do not replace vanilla crop exhaustion). Its continuous surface-speed modifier is distinct from Core friction, snow deformation, injury/exertion and rough-ground trip risk. Do not blanket-disable those. Actual combined addon runtime verification remains required.
