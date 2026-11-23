# River Weir 0.3.0 (Minecraft 1.20.1 Forge / Java 17)

River-spanning physical fishing net compatible with TerraFirmaCraft and TerraFirmaGreg Modern.

- Stakes placed at same height, 2–32 blocks apart. Hold a net roll and right-click both stakes to assemble. Sneak-right-click a stake with a roll to clear an old first-stake selection.
- Material: 2 jute fibers + 2 straw -> 4 jute cords in 2x2 crafting; 12 cords -> one 4×3 net roll on the TerraFirmaCraft loom.
- Variable-depth panels trace the freshwater column down to a solid riverbed, stopping at rock, gravel or plants, max 12 blocks deep. No replacement of solid or plant blocks. Consumes additional panels for deep 4-block horizontal strips. A deep, plant-filled or excessively wide river is rejected without consuming rolls.
- TFC `tfc:river_water` flow direction is preserved inside panels and restored when dismantling them.
- Captures ONLY physically present live fish entities near the actual occupied mesh, once per 20 ticks max, without generating new fish. Correct food item is resolved from the TFC species registry (TFC's standard fish loot tables require a player kill). Full inventory leaves fish alive.
- Shared 9-slot inventory via right-click on stake or panel. Shows net condition percent. A trapped fish sprite appears in a net panel when a catch is stored; disappears when inventory is emptied.
- Condition visibly degrades to frayed/torn without off-site alerts. Repair using sneak-right-click with tfc:jute_net.
- Dismantling a recently installed net recovers 100% of the panels if untouched, 75% if lightly worn, 50% if moderately worn and none if badly torn. All inventory contents are dropped safely. Old 0.2 nets are preserved (they lack material-cost tracking, so no roll salvage).
- Fixes selecting two posts using different stacks, stale stake selection while chunks are unavailable, detection from either side of the span, deep-water GUI distance, and delayed cleanup after the opposite stake is destroyed while its chunk was unloaded.
- Vanilla-style 16x16 cluster pixel art; discrete 4-frame water sway without AA or gradients. No fabric simulation.

`gradle clean build` runs pure-Java geometry regression checks via the `check` task. Build output: `build/libs/riverweir-0.3.0.jar`. GitHub CI validates compile and packaged JAR. In-game TFG integration still requires live testing.

For new depth, catch indicators and material salvage, dismantle old nets and place new ones. Existing inventory saves remain compatible on upgrade.
