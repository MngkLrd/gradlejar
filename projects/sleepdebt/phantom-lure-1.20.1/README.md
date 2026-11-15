# Phantom Lure 1.20.1
Minecraft 1.20.1, Forge 47.1.0, Java 17.
This port keeps the 1.21.1 gameplay and all four original PNG textures and block models unchanged.

- Place lure during day or night; in the Overworld it waits for night to summon.
- An uncharged lure summons 1 phantom after 12 seconds of nighttime.
- Insert one extra diamond via right click for 3-5 phantoms.
- Nearby sleeping players break the lure and cancel the summon; tagged summoned phantoms vanish before morning to avoid free drops.
- Manually breaking a charged lure drops the installed diamond.

Recipe: [space,string,space] / [amethyst,redstone,amethyst] / [ender_pearl,diamond,ender_pearl].
Note: another diamond is necessary for the swarm.

Build: `gradle --no-daemon clean build`.
Output: `build/libs/phantomlure-1.20.1-0.1.1.jar`.
Test before release: recipe loading, world creation, block placement, ticking, sleeping, dawn cleanup, charged model and JEI.
