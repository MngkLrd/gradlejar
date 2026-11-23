# Primitive Waterworks 0.1.1

Minecraft 1.20.1 | Forge 47.3.0+ | Java 17 | Requires TFC 3.x

Early-game wooden rain collectors, gutters and gravity downspouts for TerraFirmaCraft and TerraFirmaGreg Modern.

## How to use
1. Place a Wooden Rain Collector outdoors with open sky above its top. It collects 40 mB of clean water per rain second, up to 1000 mB.
2. Attach a Wooden Gutter horizontally, then a Wooden Downspout at its end. Stack downspouts vertically to go down.
3. Position an upright, unsealed 10,000 mB TFC barrel directly BELOW the last downspout; barrel fluid insertion only works from TOP. For sideways TFC barrels, insert from the back face, not the tap.
4. Alternatively, place the piping adjacent to a TFG 80,000 mB keg and test automation from its supported faces.
5. Run /weather rain. Right-click the collector with empty hand to inspect stored amount.
6. System transfers up to 100 mB per tick batch per collector (one batch per second), never upward.
7. A network traverses at most 128 conduit nodes, only in loaded chunks.

Recipes use every wood species of TFC's #tfc:lumber tag. No vanilla planks required.

## Visual design
World and inventory models use genuine TFC block plank textures. Item previews now use block-style isometric GUI transforms and chunkier three-dimensional geometry, no image filtering or gradients. Oak is visual default (species-agnostic beta).

## Known limitations
- Large TFG keg and other modded inventories have not been runtime-tested.
- TFC barrel fluid automation needs its open filling face (usually UP).
- No pumps, no ascending flow, no world water extraction, no chunk loading.
