# River Weir 0.4.1 - TFG Forge 1.20.1
Minecraft 1.20.1, Forge, Java 17. Both client and server must install the mod.

## Fixes from real-world test
- Fixed the player inventory and hotbar offset in the net GUI. Player slots are now derived from the same texture offsets.
- Vanilla Jute net storage is **two actual slots**. Bronze: 18, Iron: 27. No hidden ghost slots in the Jute GUI.
- Existing Jute nets with 9 slots from 0.4 are migrated automatically: items from slots 2–8 are merged into the first two; excess items drop safely at the master stake rather than vanishing.
- Added a dedicated Creative inventory tab "River Weir Fishing" / "Речные рыболовные сети" containing posts, cords and all three rolls. Items can now be located in creative search.
- Rebuilt heavy iron and bronze rolled item sprites as validated 16×16 RGBA PNGs (no antialiasing).
- Improved advanced crafting: bronze uses 2 TFC bronze ingots, 2 TFC wool cloths and one Jute roll. Wrought-iron uses 4 TFC wrought-iron ingots, 2 wrought-iron rods, 2 leather and one bronze roll. Both recipes are plain `minecraft:crafting_shaped`, which JEI and EMI automatically index when loaded; neither API is a hard runtime dependency.
- Added `/riverweir recipes` command: server checks whether each recipe ID actually exists after all TFG data packs and scripted changes have loaded. If a recipe is NOT LOADED, check pack scripts and datapack recipe removals.

## Existing mechanics
- One net roll covers 4 blocks width times 3 Jute, 5 Bronze or 8 Iron blocks depth; max spans 16/24/32 and max depths 6/10/16.
- Actual TFC fish caught in loaded chunks, inventory, mesh fish indicator, visible progressive damage and repairs, manual debris cleaning and fish-meat bait.
- Shared catch inventory accessible from any stake or net panel. Server saves inventory and other net data.

Build: `gradle clean build` on JDK 17 (recommended Gradle 8.14.3).
Output `build/libs/riverweir-0.4.1.jar`.
Automated CI checks Java compilation and packaged resources but does not reproduce the full TerraFirmaGreg Modern client UI or external recipe script overrides.

## 0.4.2
- Live fish contact uses the actual hitbox against a mesh cell (0.16 block tolerance). A fish visibly struggles for 5–9 seconds before becoming a catch. No permanent AI or gravity changes; interrupted holds release safely. A full inventory never deletes a fish.
- Torn panels have no collision while retaining selection for repairs. Breaking a panel tears every tier correctly. Forge sneak-use repair interception and validation before material consumption.
- Tier-correct stack dimension and nominal material area tooltips. Existing recipes, storage, and save format remain unchanged. In-game behavior still requires testing.

## 0.4.3
- Keeps one scan per second and unchanged fish AI/models. Active fish struggle in short varying bursts separated by rests.
- One rare escape roll per entanglement: 5% jute, 2.5% bronze, 1% iron; a freed fish swims away and cannot be immediately recaptured for 200 ticks.
- Local client-side tension-cord accent, triggered by vanilla block events on active bursts; no new block-state properties or cloth simulation. Effects expire after 16 ticks and are distance limited. Visual quality with shaders requires in-game testing.
- Pure-Java struggle regression checks, geometry and GUI tests retained.
