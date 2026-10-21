# Norse Gear 0.2.0 client acceptance tests

1. Verify **one** `norsegear-0.2.0.jar` in `mods`; remove old 0.1.xx copies and
   incompatible JEI++ unless its required JEI is installed.
2. Open the creative inventory and survival inventory; verify all 24 item sprites and no crash.
3. For all three shields, inspect offhand/front/back and blocking. They must sit OUTSIDE the
   forearm, not clip through the forearm, and not have transparent holes/stray edge bars.
4. Throw spear and javelin and throwing axe. Ensure distinct 3D flight, impact and recovery.
5. Confirm `stone_shot` looks like a pebble in inventory and like a small irregular stone in flight;
   hit mobs, walls and ground. Confirm sling firing and cooldown.
6. Wear each armor item individually, then full suit; inspect Steve and Alex with standing,
   sprinting, sneaking and arm swings. Check face exposed, chainmail gaps, and no clipping.
7. Verify war horn sound/ally buff and crafting/JEI displays.
8. Worldgen: create a NEW Overworld area; no grass replacement. Inspect bog deposits in soil.
9. Quit/rejoin existing world to verify no save/load errors.

Server startup CI is not a substitute for these client visuals. Give screenshots of any
clipping or equipment offsets for a focused follow-up.

## 0.3.1 model regression checks
- Inspect bearded vs Dane axe silhouettes at GUI, first-person and third-person scales.
- Inspect spear and javelin flight orientation and Better Combat integration.
- View round shield from front/back; verify painted panels, raised boss, leather grip, no gaps and held offset.
- Equip helmet and chestplate with Steve and slim Alex. Check open face, nose guard, pauldron animation and mail transparency.
- Compare preview images with client output. The preview is software rasterization of real model data, not a Minecraft screenshot.
