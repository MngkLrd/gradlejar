# WorldLogic TreeChopper public API

Minecraft 1.21.1 NeoForge; mod ID `wl_treechopper`; API version 1 revision 2. Ecosystem logical capability `worldlogic:tree_chopping` (schema 2), runtime key `wl_treechopper:tree_chopping` because Core requires the provider's actual namespace. For Core 1.5.0+ use `new WorldLogicCapabilities.Key<>(TreeChopperApi.CAPABILITY_ID, TreeChopperApi.API_VERSION, TreeChopperApi.Contract.class)` and `find(key, 2)`. Direct public static API and `contract()` remain available without Core or on older Core. Registration occurs in common setup only when the registry exists. No hard Core linkage; no state mutation through the read-only capability.

- `cut(ServerLevel,BlockPos)`: optional immutable CutSnapshot, four individual work values and cumulative progress in permille 0..1000, severed flag. Server thread and loaded in-bounds position only; absent for non-cut blocks. No forced loads.
- `requiresAxe(BlockState)`: actual tool requirement, including raw logs and processed timber.
- `looseFind(BlockState)` / `looseFind(ServerLevel,BlockPos)`: optional twig, pebble or flint identity, source block, collected item ID, nominal volume in m³; unknown twig volume is NaN. Loaded-level overload obeys server thread and chunk bounds.
- `rockForSurface(BlockState)`: exact supported raw substrate mapping, empty for worked stone/unsupported surfaces. Grass worldgen exception is not part of this exact mapping.

Core owns physical composition, wet mass, Janka and fatigue; Terrarian supplies local ecological moisture through the verified public facade. v1 and actual v3 WoodPhysicsApi signatures are accepted; v3 requires Core material revision 2. Trauma arm-function API remains an optional legacy direct bridge pending a declared peer capability and joint test. TreeChopper never owns a parallel stamina pool. Protection-only BreakEvent arbitration remains coordinated in issue #264; no unsupported workaround or private foreign NBT access.

Exact test fixtures: `tools/qa/scenarios/wl-treechopper.json`, `tools/qa/scenarios/wl-treechopper-core150.json`, `tools/qa/scenarios/wl-treechopper-terrarian171.json`. See PR #203 and separate exact-artifact diagnostics; declarations are not runtime verification.
