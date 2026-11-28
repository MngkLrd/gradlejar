# World Logic subsystem ownership — approved 2026-11-27

Active target: Minecraft 1.21.1 / NeoForge / Java 21 / official mappings. Core works without addons; addons depend on Core, not the reverse.

| Owner | Systems |
|---|---|
| Core | Geography, climatology, hydrology, meteorology, geology, material physics, structural physics, thermodynamics, atmospheric physics, kinetics, stamina, combat physics, animal ecology |
| Terrarian / plant addon | Plant ecology and vegetation |
| Temperature | Thermoregulation (body temperature, sweat, thermal exposure), not world air temperature |
| Traumatic | Medicine and anatomy |
| Deferred | Carried inventory burden, hydration, nutrition, circadian rhythms |

Physical item/entity mass stays in Core: it is used for impacts, doors, boats, tools and structures; do not extend a separate carried-weight progression system until approved. Plant growth, medical, food and sleep legacy rules still exist in RC20/RC21; the above is the target ownership, not a claim that code has already moved. Do not silently duplicate, delete or disable saved rules. Migrate overlapping rules and state deliberately with addon integration tests and a documented standalone fallback/config strategy. Do not start deferred addons now.

Priority: environmental API, weather, hydrology and geology. Preserve existing world presets and generator behavior until a separately versioned generation update is tested. RC21 adds model estimates for relative humidity, outdoor wind and local vanilla precipitation; it is not a dynamic regional weather simulator.

## Meteorology sequence

RC22: deterministic regional pressure and moving warm/cold/moist/dry fields, cloud and precipitation potential, diagnostics. Next: reconcile regional controller with server/client weather, local precipitation and vanilla mechanics (water, fire, snow, crops); only then update the live environmental API atomically. Later: terrain/near-water and shelter microclimate, fog/dew, storms, seasons and extreme events. No separate weather addon; Temperature consumes the live core API only.

Scientific direction: [METEOROLOGY.md](METEOROLOGY.md). Replace independent weather effects with a coupled, bounded dynamical and thermodynamic model in staged increments; do not advertise institutional validation or use preview fields for gameplay.

Next world generation design: [extended vertical world and geology-led caves](WORLDGEN-EXTENDED.md). Proposed separate new-world preset; do not change existing generation IDs.
