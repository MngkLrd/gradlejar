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
