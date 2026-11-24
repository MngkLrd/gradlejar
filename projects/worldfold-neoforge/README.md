# World Fold: Spatial Engineering 0.2.0
Minecraft 1.21.1 / NeoForge 21.1.228 / Java 21.
Requires Immersive Portals CE 6.1.3.1 plus Cloth Config for NeoForge on both client and server. CE provides renderer, collision, cross-portal block interaction, entity transit and scale; World Fold owns survival tools, persistent topology, room generation, routing, resource transport and load simulation.

## First fold
Craft two Seam Anchors and a Fold Compass. Place nodes on two clear floors with space above and to either side. Their patterned front faces the placing player. Right-click air with compass to cycle aperture 3x3, 5x5, 9x7, 15x9; sneak air-use cycles destination size 1x, 0.5x, 2x. Click A and B; each is bidirectional. A link may be same or different dimension; multiple links can form physical loops. CE controls recursive rendering. A compass can create unlimited links within the network limit.

## Resource seam
Place a barrel or compatible tank directly below each anchor (top capability). Connect A and B; use Spatial Tuner mode Resource direction on either anchor to cycle OFF, A→active, active→A. Items and fluids move in bounded batches via persistent in-flight buffer; both ends must be loaded. Redstone enters the back of the anchor, exits at the front of the other. It is relayed every five ticks. Tuner Inspect reports route, load, strain and pending resources. Do not break receivers while resources are in transit. Do not connect both sides to the same logical inventory.

## Router
Select Tuner Branch mode via air-use; click A then another unused anchor C, same size as B. Input signal >=8 behind A chooses C, otherwise B. Route change is deferred if a creature, item, obstruction or buffered resource prevents safe switching. A single active route at a time. Inactive anchors do not relay. Dismantle mode + sneak click removes a link (unless occupied/buffered). Repair mode recreates geometry.

## Stability
Stress increases with aperture area, distance, scale, dimension and routing. Place Spatial Stabilizers on horizontal faces of A and the active destination. High strain slows resource throughput; 95+ pauses new transfers and redstone. Portal remains open for safety. Stabilize to recover. No forced background chunk tickets or destructive instability.

## Foldspace
Use Chamber Seed on unused anchor. Air-use chooses 17x17, 25x25, 33x33 initial footprints, costing 1/2/4 seeds. Creates a real independently buildable room in dedicated void dimension. Its opening is 3x3. Expansion by construction is allowed; rooms are spaced 96 blocks. If stranded, /worldfold recall returns near the original entrance or overworld spawn. /worldfold status lists owned links. Chamber allocation persists even if a portal is dismantled. No moving/copying existing structures or universal vanilla neighbor remapping. Tanks use NeoForge fluid capability, not natural river fluid simulation. No automated game test, no shader guarantees; use disposable world. Prototype 0.1.0 had different storage; start a fresh test world.

Credits: Immersive Portals CE, qouteall, Nick1st, NsTut and contributors: https://github.com/UpperMoon0/ImmersivePortals.
