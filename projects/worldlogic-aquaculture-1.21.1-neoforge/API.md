# Aqua integration (1.21.1 NeoForge)
Aqua owns actual fish/roe and marine population/lifecycle, fishing traits and species-specific AI. Core owns climate, water classification, river advection and PhysicalMaterials. Required Core 1.5.1, API revision >=21; core material revision >=2. 
- `WorldLogicApi.riverCurrent` supplies velocity in m/s; absent is undirected/unknown. Aqua chooses active swim direction and habitat preferences, never applies duplicate drift.
- `PhysicalMaterials` per-catch mass in kg, count-one pure provider; unknown mass stays unknown. Existing catch IDs unchanged.
- Optional Trauma API v1, public reportInjury only, fallback one vanilla damage call; no private NBT.
- Food processing and public edible serving contract (#259) are pending. No nutrition side effects on catching or holding.
