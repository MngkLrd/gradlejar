# River Weir - TerraFirmaCraft 1.20.1 Forge

Build hub project for MngkLrd/gradlejar. Java 17, Gradle 8.14.3, Forge 47.3.0.

## Gameplay
* Two hand-crafted wooden stakes are placed across a river, horizontally aligned and at the same Y-level.
* Work the TerraFirmaCraft loom with 12 jute fibers to weave one 4-block net roll.
* Right-click the first stake with a roll, then the other. Spans: 2-32 water blocks; consumption: ceil(span/4) rolls.
* Automatically forms actual waterlogged net segment blocks, up to 3 blocks deep, including across chunk borders.
* Right-click a stake or connected net piece to open the shared 9-slot inventory.
* Server scans for existing AbstractFish entities near the structure while chunks are active, inserts their normal entity loot into the inventory, and only then discards the fish.
* Netting wears, progressively frays, and finally tears. No break notification. Crouch + right-click with tfc:jute_net to patch it.
* Destroying a stake dismantles the panels, restores water and drops caught items. The other stake remains.
* All fish drops use their actual entity loot tables, including TFC fish.

## Crafting
* Sticks: 2 vertical sticks in the left column, a TFC saw in the top-right; yields 2 stakes, damages the saw, fits the 2x2 player grid.
* Net rolls: TFC loom, 12 tfc:jute_fiber / 12 steps -> 1 net roll. The normal TFC jute net craft remains available for repairs.

## Known limits
* Does not generate synthetic fish and doesn't accumulate catches in unloaded chunks.
* For MVP the top net row needs a straight uninterrupted water corridor, at either stake Y or Y-1.
* Repairs regenerate missing net panels only where water is available. Do not place solid blocks inside the completed structure.
* Standard client/server mod: must be installed on both ends alongside TerraFirmaCraft.
