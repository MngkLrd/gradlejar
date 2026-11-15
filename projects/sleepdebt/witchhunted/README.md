# Witch Hunted, Alpha 0.1.1

Minecraft **1.21.1 / NeoForge / Java 21**. A pocket nightmare inspired by Orwellian surveillance and cosmic horror.

## Play
1. Kill vanilla witches until **The Hunted Hat** drops (20% independent chance per death).
2. Place the 3D hat on any block and right-click it with an empty hand.
3. Enter **The Hollow**, a custom dimension with fixed midnight, an obsidian eye-shaped sanctuary, and hostile watchers.
4. Cross the two blackstone causeways to the two handmade vaults (at x=46 and x=-46).
5. Recover **The Observer's Eye**: right-click to see nearby hostile mobs through walls and get night vision (18 seconds, 45-second cooldown).
6. Return by right-clicking the hat in the center. Return location is stored per-player.

## Contents
- Fully modeled 3D hat, handcrafted 32x32 pixel-art fabrics, self-painted eye icon.
- A real JSON-defined dimension, with fixed time 18000 and End-like skies.
- Two architectural landmarks with hand-stocked loot chests.
- Cursed zombies and masked skeletal Witnesses which pursue players.
- Multiplayer-friendly per-player return coordinates; spawning logic server-side.

## Alpha limitations
- First-generation sanctuary/ruins are fixed coordinates; the surrounding world is flat, not a finite bounded island.
- Creatures reuse vanilla animations and materials. Custom mob rigs, procedural ruins, custom shaders and boss encounter are for later milestones.
- Opening the portal requires empty-handed right-click on a *placed* hat.
- No client/in-game QA is claimed simply because Gradle compiles.

## Build
Install Java 21 and Gradle 9.2.1, then run `gradle clean build`.
JAR: `build/libs/witchhunted-1.21.1-0.1.1-alpha.jar`.

## Sanity test
- Start a new world, summon witches, check rare hat drops.
- Place hat and enter on survival/server, verify lighting, flatland, ruins and chest rewards.
- Use eye near mobs; verify status/cooldown.
- Return and check preserved coordinates.
- Leave/re-enter: hub, chests and paths must not reset.
- Test multiple players, dying inside, and restarting the server.
