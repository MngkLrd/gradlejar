# Primitive Waterworks 0.3.0

Minecraft 1.20.1 | Forge 47.3.0+ | Java 17 | TerraFirmaCraft 3.x

## Changes since 0.2.0
- Fixed models to show physical carved-out ports, not a decal.
- Right-click a **side** of a rain collector with a TFC knife (tag `tfc:knives`) to carve/open its outlet. Use again to close it. It costs 1 durability.
- Only opened collector sides send water into adjacent horizontal gutters. Direct vertical output to downspout below remains available.
- Collector only produces vanilla fresh water when raining outdoors. Internal capacity: 1000 mB, collection: 40 mB per rain second. Nothing else can be pumped into its tank.
- Fluid flows only horizontally/down, via checked connected pipes (max 128 positions). No pumps, upward flow, lava, alloy or beer.
- Water appears in a collector when stored or briefly passing through; only segments of gutter/pipe used by a successful transfer show water. A transient water layer disappears after ~1.5 seconds.
- Native species variants for 20 TFC wood types, plus 10 ArborFirmaCraft species when AFC is installed. Uses native wood textures from TFC/AFC without importing blurry textures.
- The 3 prior default block IDs remain the oak variants, preserving placed blocks.
- All previous crafting JSON recipes deliberately removed due collisions; use creative or /give until recipe screenshots are provided.
- This build has not been end-to-end tested in TerraFirmaGreg Modern; verify with your modpack.

## Test
1. Put collector on open roof under rain; inspect collected/transfer totals with empty-hand RMB.
2. Point your cursor at an outer SIDE and right-click with any knife in #tfc:knives. A notch should appear.
3. Put wooden gutter adjacent to the open notch; lead to a downspout and then a barrel from its allowed input face.
4. Run `/weather rain`. Filled receiver increments the transfer count and gutter water becomes visible.
5. Close the notch with the knife, remove one pipe, or raise a pipe a block: flow must stop.
6. Try a variety of wood species via creative tab; when AFC is installed its 10 new species should also be present.

## Controls
- RMB with TFC knife against side of collector: toggle fluid outlet.
- RMB with empty hand on collector: tank, lifetime collected, lifetime transferred.

## Known limitations
- No survival recipes in 0.3.0 (waiting for custom shapes).
- Fluids do not remain stored in wooden conduits: their temporary water surfaces only show successful transfers.
- Rendering with some third-party shaders may differ.
- A placed outlet can connect if the receiver supports Forge sided-fluid input; some modded tanks might not.
