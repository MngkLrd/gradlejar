# River Weir 0.2.0
Minecraft 1.20.1, Forge, Java 17, TerraFirmaCraft + TerraFirmaGreg Modern.

Install two wooden stakes at the same Y on opposite river banks, with 2 to 32 water blocks between them. Select each post using a woven 4x3 net roll, twice with right-click. Each net roll covers four horizontal blocks by three vertical blocks. A stack of N rolls covers N*4 blocks if water is 3 deep, or fewer horizontal blocks at greater depth. Requirement is calculated per strip of four blocks using its deepest point. Max depth 12.

TFC-specific crafting: two jute fibers and two straw in a 2x2 diagonal yield four twisted net cords. A TFC loom consumes 12 cords in 12 steps for one net roll. TFC saw with two vertical sticks makes two stakes in a 2x2 grid. For repairs, crouch + right click the weir with a tfc:jute_net.

Panels cover liquid source water only, tracing each column until the riverbed without destroying stone or vegetation. Connections render rope links all the way to each stake. Ropes appear at the surface and each third/lower row, not every meter. Pixel art is opaque/transparent 16x16 pixel clusters, with a 4-frame low amplitude animated texture for subtle ripple; no full cloth physics. Icon rebuilt as a rolled net bundle with knots. All animations use nearest-neighbor with no interpolation.

The weir scans existing AbstractFish mobs standing against the mesh every 10 ticks when the chunk is loaded, moves one TFC food item of the matching species (or vanilla fish) into its nine-slot inventory, then removes the fish. The previous standard loot-table approach was broken because TFC fish require killed_by_player. A full inventory does not delete the fish. The weir wears and can tear without notifications, only visible damage. Right-click any panel or stake to open the inventory.

No synthetic fish, no catch accumulation while unloaded. Existing v0.1 nets retain their original depth until rebuilt. Fresh water only, max 12 blocks deep, no aquatic plants in the construction corridor. In-game integration not tested by CI.
