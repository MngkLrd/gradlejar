# Primitive Waterworks 0.3.1

Minecraft 1.20.1 | Forge 47.3.0+ | Java 17 | TerraFirmaCraft 3.x

Changes:
* 30 species-specific variants covering 20 TFC woods and 10 ArborFirmaCraft woods when AFC is installed.
* Knife-carved outlets on rain collector sides: right-click with any TFC knife on the required wall. Empty-hand right-click displays current, total collected and total delivered water.
* Carved ports are visually open; uncarved walls are solid and never carry water sideways.
* Fresh rain water only. No fluid input capabilities, no lava, molten metal or non-water liquids in pipes.
* Gutter geometry uses a finite shape mask: only actual connected arms remain open. Idle ends are capped; no extra straight segments or connector stubs on turns.
* Downspout sitting directly ABOVE a gutter is connected via a visibly taller gutter inlet, allowing roof-to-basement gravity networks.
* Water layers appear in a non-empty rain collector and for active transfers through gutters / downspouts (brief transient flowing indicator).
* Previous duplicate survival recipes removed. New screenshot-defined distinct recipes are still pending.
* Model textures inherit genuine TFC / AFC wood pixels.

Usage:
1. Roof: place collector under open sky; carve desired lateral outlet with a knife.
2. Connect horizontal gutters; water travels sideways and DOWN only.
3. Place downspout above (or below) a gutter to form vertical routing.
4. To fill a vertical TFC barrel, ensure the terminal pipe is directly over it; enable TFC barrel automation in server config.
5. During rain right-click collector with EMPTY hand to inspect collected/delivered counters.
6. Validate by removing a middle pipe: no water should be transferred through the broken route.

Known limits: real TFG modpack runtime compatibility and shader rendering must be verified in game.
