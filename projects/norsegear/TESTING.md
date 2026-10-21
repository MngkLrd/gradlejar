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
