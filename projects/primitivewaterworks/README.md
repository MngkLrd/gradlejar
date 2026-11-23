# Primitive Waterworks 0.2.0 (Minecraft 1.20.1 / Forge / TFC)

## New: working gravity distribution, anchored models and oriented collectors

* Rain collector holds 1000 mB and collects 40 mB/s in natural rain.
* No upwards flow, including when connected to another pipe below the collector.
* A rain collector has a player-facing horizontal outlet and an optional drain directly underneath.
* Sneak + empty hand + right click on the collector rotates its horizontal outlet clockwise.
* Empty hand + right click shows tank contents, lifetime collected and lifetime transferred.
* Gutter channel sits at y3..7 aligned with the collector's lower outlet instead of y9..12.
* Gutters visually join to adjacent gutters, downspouts and the collector's facing output.
* Downspouts also have side ports for gutters, join vertically, and transfer to lower pipes.
* A gutter directly above a downspout gets a drain collar. Outlet can also feed a compatible fluid tank directly below.
* Pipes route through a connected graph at most 128 visited blocks, only across loaded chunks.
* TFC upright unsealed barrels accept fluids only from UP if server automation is enabled.
* Does not automatically pull fluids from third-party source collectors (such as Firmalife devices).
* Models reuse TFC's genuine 16x16 oak plank texture and include separate 3D items.
* All recipes use #tfc:lumber, compatible with TFC's wood species.

## Build
Java 17, Gradle 8.14.3, Forge 47.3.0+. Run `gradle build`.
Output: build/libs/primitivewaterworks-0.2.0.jar

## Verification layout A: direct vertical drop
- Place collector outdoors with sky clearance.
- Under collector, stack several downspouts leading down to an OPEN upright TFC barrel directly below.
- Use /weather rain and confirm the collector's delivered counter rises and barrel fills.

## Verification layout B: roof gutter / basement tank
- Put collector on roof. Face where the gutter should lead (Shift+empty-hand right-click to rotate).
- Put a gutter at equal Y in the output direction, and connect gutters to a downspout.
- Stack downspouts vertically, descend into basement, then horizontally with gutter(s) to a TFG keg.
- Confirm no transmission if you remove a middle pipe or face the collector away from the gutter.
- No water should move into pipes or tanks higher than the collector.

## Known limitations
- Compatibility of TFG / Firmalife tanks is not end-to-end tested in-game.
- Tank contents are not visually rendered inside channels; models are wooden only.
- Some external fluid handlers do not expose usable sided capabilities, thus may not be detected as connecting.
- Gutters remain straight/open when placed by themselves; their X/Z placement axis sets a visual default.
- A connected receiver visually requires its Forge fluid capability to expose an input side.
