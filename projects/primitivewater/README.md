# Primitive Water 0.1.0 - TerraFirmaCraft rain collection
**Minecraft 1.20.1 / Forge / Java 17**. TFC required at runtime. Built via MngkLrd/gradlejar.

## Blocks
- Rain Collector: 1000 mB storage, gathers vanilla fresh water (the same fluid used by TFC barrels) while exposed to rain. 4 mB every 4 ticks; no collection during snow, when blocked from sky, or in unloaded chunks.
- Wooden Gutter: connects horizontally and routes water horizontally/downhill.
- Wooden Downspout: vertical descent. Downspouts route water **only downward** to the next pipe or tank.

Collection never creates an infinite source when dry. Water moves without power, at most 20 mB per second per collector through up to 64 connected wooden pipe nodes, into the first accepting tank. Pipelines can't move water uphill. Pipes carry rain collector water only; they do not move arbitrary fluids or extract from TFC barrels. Standard Forge fluid handlers can draw water from the collector (e.g. Create).

## Crafting
Recipes use only #tfc:lumber (normal TFC lumber pieces, including any addon that registers in the same tag). No vanilla plank block recipe.
Wood model uses the **original TFC oak-plank pixel texture**, avoiding blurry/generated art. Wood variants and visible flowing water are not implemented in this MVP.

## Usage
Place rain collectors beneath open sky, connect wooden gutters and downspouts, then attach a TFC barrel/keg. For a normal TFC barrel, the game's own fluid capability and sided restrictions decide whether insertion is accepted. Try an **unsealed** barrel and connect the proper side if it does not accept water. Right click collector with empty hand to see volume.

## Testing
1. Start TFC + this mod, /weather rain, verify a sky-exposed collector fills.
2. Cover collector, verify rain stops. /weather clear verifies no generation.
3. Connect gutter -> downspout -> small barrel and large keg. Compare source/destination mB to detect duplication.
4. Confirm a full or incompatible tank rejects water without deleting fluid.
5. Break/re-place collector (tank is intentionally emptied on break; no portable fluid).
6. Run dedicated server for side-safety and chunk border tests.

Source code has **no direct TFC class references**: integration is by #tfc:lumber and Forge fluid API. The addon's runtime dependency on TFC is declared in mods.toml.

Known limitations: no wooden variants, no separate water surface animation, no cross-chunk loading, no pump/extraction from arbitrary tanks, pipe path cap 64, no TFG-specific Keg runtime smoke test yet.
