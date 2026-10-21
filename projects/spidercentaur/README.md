# Spider Centaur 0.1.0-alpha

A one-entity skeleton/spider hybrid for NeoForge Minecraft 1.21.1.

## Alpha feature set
- One original entity type `spidercentaur:spider_centaur` with a spider body and eight animated legs, a skeleton upper torso, skeleton arms/head, and a visible bow.
- The body and torso are composed **at render time** from vanilla spider/skeleton model geometry with separate vanilla textures; no Blockbench or custom PNG required for this prototype.
- Vanilla skeleton bow attack AI, enhanced run speed, 26 HP, wall climbing, and default vanilla arrows.
- Rare natural Overworld MONSTER spawns at night (weight 4, groups of 1), plus creative spawn egg.
- Basic bones/strings/arrows/spider eyes drops.

## In-game testing
1. Install NeoForge 21.1.x for Minecraft 1.21.1, Java 21.
2. Place the JAR in the `mods` directory on both server and client.
3. Create a test world. Use `/summon spidercentaur:spider_centaur ~ ~ ~`.
4. Check the eight legs, hidden vanilla spider head, elevated skeleton chest/head, bow held in the right hand, walking/shooting animations.
5. Test skeleton bow damage, tracking, climbing a wall, drops, night-only natural spawns, and spawn egg.

## Building
```sh
gradle clean build
```
Result: `build/libs/spidercentaur-1.21.1-0.1.0.jar`.
GitHub Build Hub publishes successful CI JARs to Releases as prereleases.

## Prototype limitations
Vanilla model/texture parts are reused and not fused into a pixel-perfect custom skin. Pose blending and skeleton-spider seam need in-game inspection. Spider-centaur aims its upper body at targets but does not yet have custom circling AI, dedicated idle audio, unique drops or attack particles. No integrated client runtime test in CI.
