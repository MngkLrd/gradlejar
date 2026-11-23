# River Weir 0.4.0
Minecraft 1.20.1 Forge, Java 17, TerraFirmaCraft / TerraFirmaGreg Modern.

## Three physical net tiers
| Tier | Size/roll | Max width | Max water depth | Max wear | Fish slots | Initial chance per fish scan | Min time between catches |
| Jute | 4 x 3 | 16 | 6 | 100 | 9 | 26% | 180 ticks |
| Bronze-reinforced | 4 x 5 | 24 | 10 | 220 | 18 | 40% | 120 ticks |
| Wrought-iron heavy | 4 x 8 | 32 | 16 | 360 | 27 | 55% | 80 ticks |

One roll covers up to four horizontal cells by the tier's panel height; material needs are calculated per four-column group using its maximum river depth. Keep both stakes at the same Y-level. Place the rolls by right-clicking both stakes with any one type of roll; the entire net uses the selected type. No oceans: width and depth are strictly capped. Preserve water types and TFC river current. Never replace rocks, plants or occupied underwater blocks.

## Material progression
- Jute: two jute fibers + two straw → twisted net cords. 12 cords on the TFC loom → 4 x 3 jute roll.
- Bronze: one jute net roll, two TFC bronze rods and two TFC wool cloth → bronze-reinforced 4 x 5 roll.
- Wrought iron: one bronze roll, four TFC wrought-iron chain blocks, two wrought-iron rods and two leather → wrought-iron heavy 4 x 8 roll.
- Two vertical Minecraft sticks plus one TFC saw (2x2 crafting) → two wooden stakes.
- Jute net from TFC used for repairs on any tier; upper tiers have larger total wear so need more repairs over their lifespan.

## Deep gameplay
- Scans existing real water fish only, within a narrow distance of an actual submerged mesh cell. Loot items are matched to registered TFC fish species because the TFC fish loot tables require killed_by_player. No passive item generation in unloaded chunks.
- A fish caught with inventory space is removed, otherwise lives. One fish per capture, with tier-dependent chance and cooldown.
- Fish meat bait: right-click any connected stake or section while holding an item in #riverweir:bait. Consumes 1 bait to gain 4 charges (maximum 16). Each successful catch consumes one charge. Bait multiplies catch probability by 1.8.
- Dirt and silt accumulation from catches and occasional loaded-world maintenance ticks. At 90% debris the trap ceases working. Sneak-right-click an empty hand on a stake or section to clean. Client menu shows debris %, condition %, and bait charges. Damage state still appears only visually in-world, no remote failure notifications.
- 9 / 18 / 27 output slots, depending on material. Same inventory shared by both stakes and the net panels. Fish silhouette appears in the mesh after the first catch.
- Broken nets stop catching and remain visibly torn; sneak-right-click with a TFC jute net to repair. Dismantling a structure may refund a fraction of its exact tier's rolls based on wear. Fish catches always drop on teardown.
- v0.3 save compatibility: old 9-slot ItemStackHandler saves are migrated to the 27-slot internal handler; old meshes default to tier 0. Existing v0.3 installations with larger than current Jute limits are grandfathered, but newly built Jute nets respect tier caps.
- Animated 16x16 frame-by-frame pixels without AA/gradients, material-specific palettes and roll icons for three tiers.

## Testing
`gradle clean build` on JDK 17, Gradle 8.14.3. Compiled jar under `build/libs/riverweir-0.4.0.jar`.
Pure-Java arithmetic regression included. GitHub Actions verifies packaging and compilation; full TFG modpack gameplay needs testing.
