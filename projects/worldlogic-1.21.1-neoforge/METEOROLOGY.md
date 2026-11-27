# World Logic meteorology: scientific design and implementation boundaries

References (scientific inspiration, not certification or code reuse):
- UK Met Office Unified Model: https://www.metoffice.gov.uk/research/approach/modelling-systems/unified-model
- Met Office continuous equations: https://www.metoffice.gov.uk/research/foundation/dynamics/continuous-equations
- Met Office fronts: https://weather.metoffice.gov.uk/learn-about/weather/atmosphere/weather-fronts
- Met Office air masses: https://weather.metoffice.gov.uk/learn-about/weather/atmosphere
- Met Office pressure: https://weather.metoffice.gov.uk/learn-about/weather/how-weather-works/high-and-low-pressure
- Met Office humidity: https://weather.metoffice.gov.uk/learn-about/weather/types-of-weather/humidity
- Met Office fog: https://weather.metoffice.gov.uk/learn-about/weather/types-of-weather/fog
- Royal Meteorological Society stability: https://www.rmets.org/metmatters/when-air-stable-or-unstable
- RMetS convection: https://www.rmets.org/metmatters/what-are-convective-storms
- RMetS professional accreditation: https://www.rmets.org/chartered-meteorologist

## Target model

The core owns atmosphere state and environmental API. Temperature owns organism heat exchange. Do not equate professional NWP with this game approximation. The climate generator is the long-term boundary condition; weather is evolving state, not independent random noise. World scale and time compression are explicit design parameters.

1. Dynamics: regional grid with pressure, wind vector, temperature and water vapor; upwind conservative flux transport; pressure-gradient acceleration, signed-latitude Coriolis and drag; bounded time step and boundary conditions. Distinguish horizontal transport from vertical forcing. Verify closed-domain mass and tracer conservation before coupling.
2. Thermodynamics: hydrostatic pressure, gas properties, specific humidity/mixing ratio, saturation vapor pressure, dew/wet-bulb, dry and moist adiabats, latent heating, inversion/stability and buoyancy. Cloud formation depends on saturation and lift.
3. Synoptic systems: coherent air masses exchange heat/moisture over ocean and land; identify fronts from temperature/moisture gradients and wind convergence. Low/high centers are relative pressure features, not unconditional weather types. Jet steering, occlusion and blocking are later parameterizations.
4. Mesoscale: orographic ascent and lee drying, convection triggered by instability + moisture + lift, sea/land breeze and cold pools. No random thunder independent of these inputs.
5. Boundary layer: day/night radiative budget, soil/water heat capacity, roughness, humidity, fog and dew. Distinguish actual shelter from sky occlusion; no duplicated body thermoregulation.
6. Hydrometeors: prognostic cloud liquid/ice, precipitation formation and evaporation, melting/freezing layer, snow/rain/mixed phases. Maintain water budget with runoff, soil storage and evaporation; precipitation mass is not a normalized visual intensity.
7. Coupling: same authoritative state for server rain, snow, fire/crops/animals, per-player rendering, and addon environmental API. Support configuration, world saves, deterministic restart and unloaded locations without forcing chunks. Separate renderer and state. Preserve vanilla fallback in dimensions with no atmosphere model.

## Current implementation (RC22 research branch)

Seeded moving synoptic pressure/temperature/moisture waves remain provisional forcing, not conserved advection. New `AtmosphericColumn` calculates vapor pressure with a Magnus approximation over liquid water, mixing ratio in kg/kg dry air, dew-point inversion, a psychrometric wet-bulb estimate, hydrostatic layer pressure, dry and moist pseudo-adiabatic parcel cooling, condensate removal constrained by initial vapor, and positive buoyancy integral for reachable saturated layers. It diagnoses cloud depth, precipitation and radiation-fog potential; indices are uncalibrated. The environmental lapse and lifting height are heuristic, the fixed 100 m vertical step does not resolve a real cloud, and the 4 km cap omits much of the troposphere. The condensation level is a discretized parcel estimate, not exact LCL; the buoyancy integral is not full CAPE. No conservation between neighboring columns, ice phase, actual winds, regional precipitation rendering or live gameplay change. Existing RC21 atmosphere API stays authoritative.

QA: real dedicated-server command checks dry/wet, stable/unstable, no-lift and fog/mixing controls, temperature/dew/wet-bulb ordering, water budget, deterministic sampling, unavailable locations and live-state isolation. Keep all existing tests. The prior RC22 run timed out during the two-seed terrain check; split by seed without reducing samples or assertions. A successful numerical diagnostic is not evidence that local precipitation or rendering works.

## Next implementation gate

Replace provisional synoptic forcing with a bounded persistent transport grid and explicit energy/water exchanges, verify conservation/stability and regional continuity; then couple the column to regional weather events and client synchronization. Do not alter live temperature or rain until the complete gameplay and API path is tested.

## First live integration (RC22)

Opt-in `modeled_weather` (off in all presets), every 200 ticks, uses the loaded overworld spawn column to select clear/rain/thunder from condensate and convection with hysteresis. Actual vanilla weather state, not independent effects. Respects `doWeatherCycle`, no forced chunk loads. This is intentionally a single anchor, NOT per-region rain; actual temperature and wind still use the previous live API. Regional transport and per-position precipitation synchronization remain necessary before default enablement.

## Conservative regional transport (RC22 research increment)

9x9 grid, 1024-block spacing, fixed closed boundaries, persisted via a separate SavedData entry. Donor-cell fluxes move normalized air mass, a temperature tracer (mass times Kelvin), and vapor mixing ratio mass together across interfaces. Prescribed synoptic steering and pressure-gradient correction determine direction, bounded 8% per face. Bilinear local sampling, no chunk generation. Initial grid uses spawn climate reference and seeded regional anomalies; no global or terrain-resolved climatology yet. Each 200-tick opt-in controller update uses the grid cell and parcel diagnostics to drive actual single overworld weather state. Atmospheric pressure uses proposed 10 m/block scaling; player ambient temperature API unchanged. This is a conservative transport prototype, not full energy conservation: no evaporation/radiative forcing, latent energy transport, condensate sink, open boundaries, global circulation, local client precipitation or validated physical forecast. QA checks budgets after 200 advances, positivity, evolution, interpolation, altitude, NBT roundtrip and live controller consumption. Existing weather fixture commands use 2400 ticks to avoid expiring before vanilla weather transition finishes.

## RC27 local weather
Previous modeled_weather was opt-in and spawn-anchored. New regional_weather rule defaults on for geographic presets; local deterministic moving fronts, pressure-driven steering, moist/dry/cold controls, compressed eight-day monsoon cycle, dust storms, fog, snow/blizzards, rain/downpours/thunderstorms. Each loaded position gets its own authoritative snapshot, with temperature anomaly and solar attenuation in environment API. Client packet once per second; vanilla rain/snow rendering, local dust/snow/rain particles and density/color fog. No world-wide rainfall while local mode is active, old global controller disabled only in local mode. WetGear, mud tracks, snow weight and wet torch effects use local API. Limits: per-player precipitation sheets, vanilla phase/biome renderer still influences sheet appearance, no graphical client validation, no natural lightning strikes or regional cauldron/crop/vanilla rain routing, no full NWP coupling. Monsoon is a simplified gameplay seasonal cycle, not a real seasonal circulation solver. New weather fields and API do not affect worldgen or climate geography.
