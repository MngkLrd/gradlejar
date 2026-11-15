# Phantom Lure 26.2
Minecraft 26.2, NeoForge 26.2.0.87, Java 25.
This port keeps the 1.21.1 gameplay and all four original PNG textures and block models unchanged.

- Place lure during day or night; in the Overworld it waits for night to summon.
- An uncharged lure summons 1 phantom after 12 seconds of nighttime.
- Insert one extra diamond via right click for 3-5 phantoms.
- Nearby sleeping players break the lure and cancel the summon; tagged summoned phantoms vanish before morning to avoid free drops.
- Manually breaking a charged lure drops the installed diamond.

Recipe: [space,string,space] / [amethyst,redstone,amethyst] / [ender_pearl,diamond,ender_pearl].
Note: another diamond is necessary for the swarm.

Build: `gradle --no-daemon clean build`.
Output: `build/libs/phantomlure-26.2-0.1.1.jar`.
Test before release: recipe loading, world creation, block placement, ticking, sleeping, dawn cleanup, charged model and JEI.
