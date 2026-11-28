# Terrarian public API

Core is the required parent; ecosystem declaration matches the Core RC55 branch module ID, but the currently pinned compiled dependency is RC54 (Core 1.2.1, API revision 15, material revision 2). Core's schema-2 manifest is on its separate development branch, not in this checkout. Direct public classes below are the supported contracts on the active Minecraft 1.21.1 NeoForge target; no claim of RC55 runtime capability registration.

## Fungal ecology v1 revision 1
`dev.minemods.worldlogicterrain.growth.FungalEcology`: `Species.values()` provides stable species IDs, substrate kind, temperature/moisture preferences and the block/item accessors after registry initialization. `snapshot(ServerLevel, BlockPos)` returns Optional<Snapshot> on the server thread, loaded positions only, no forced loads. Snapshot reports species, age (0 mycelium, 1 immature, 2 mature), substrate validity, temperature C, moisture 0..1, local growth probability 0..1 conditional on a growth opportunity and status. Unknown/non-colony/unloaded => empty. `rate(Species,double,double,int)` is a pure ecological fitness calculation, not a calendar. World and HUD use the same calculation. Harvest is through the existing player block interaction, not public unchecked mutation. No conversion/decay of logs or soil; spread limited to nearby loaded suitable substrate.

## Plants and soil
`growth.SeasonalGrowth.view(ServerLevel,BlockPos,BlockState)` provides actual HUD snapshot, progress/quality/speed percent (negative = unknown); `evaluate(PlantSpecies.Profile,Input,boolean)` evaluates supplied climate input without world mutation. Calendar-independent fruiting for fruit trees and berry bushes; crop planting/growth unchanged. `soil.SoilExhaustion.exhausted(BlockState)` is a cultivation substrate query; `plantGrowth(ServerLevel,BlockPos,BlockState)` records a successful growth transition, called by the owning plant mechanic, not a second consumer-side charge.

## Wood species
`api.WoodPhysicsApi` revision 3, `available()`, `profiles()`, `profile(BlockState/ResourceLocation)`, `sample(ServerLevel,BlockPos)` and `atMoisture(Profile,double percent)`. Density kg/m³, hardness N, bending MPa, moisture dry-mass percent; mass kg for supplied volume m³. Core revision 2 owns physical calculations; older Core => empty queries and unsupported explicit moisture evaluation. Nether Janka unknown. See README for diagnostic dependency and QA provenance.

Snapshot.count() exposes the persistent 1..4 fruiting capacity; legacy constructor defaults to one. Harvest yield equals count, same-species placement increments capacity up to four, daughter colonies start at one.

## Mossy deadwood v1 revision 0
`tree.MossyDeadwood.original(BlockState)` returns original log resource ID or empty; `suitable(CoreHabitat.Sample)` checks local moss ecological constraints, not a climate model. Moss variants use Core original material composition, retain log axes and block tags; no independent wood profile.


## Moss ecology v1 revision 0
`moss.MossSpecies.values()`, `block()`, `supports(BlockGetter,BlockPos)`, `suitable(CoreHabitat.Sample)` describe terrestrial moss and lichen substrate and Core-provided habitat. Irish moss is a marine `water.BenthicSpecies.IRISH_MOSS` and retains native source identity. Food values are normal Minecraft food components; Metabolism may consume them through its normal food path. No new energy model or independent weather simulation.
