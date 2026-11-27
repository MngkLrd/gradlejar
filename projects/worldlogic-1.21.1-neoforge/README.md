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
