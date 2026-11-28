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
