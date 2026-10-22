# Spider Centaur 0.2.0 Alpha

NeoForge 1.21.1, Java 21, Minecraft's vanilla SkeletonModel + SpiderModel composed into a single entity.

## New in 0.2.0
- Custom cowboy hat model, fully procedurally built using Java ModelParts: turned-up brim, tall crown, obsidian-dark band.
- Sparse purple-black obsidian growths over part of the skeleton skull, ribs, spider back and thorax.
- Fine magical purple cracks and exactly one emissive red eye (other eye remains vanilla).
- All custom artwork consists of manually generated pixel-cluster texture PNGs; no Blockbench export or external model dependency.
- Hat and skull growths follow skeleton head animations, chest growths follow skeleton body; spider plates render on its back.
- No changes to 26 HP, speed, bow AI, night spawn, wall climb, loot, or spawn egg.

## Testing in Minecraft
1. Use Minecraft 1.21.1, NeoForge 21.1.x, Java 21.
2. Replace previous Spider Centaur jar with 0.2.0 in the mods folder.
3. Spawn with `/summon spidercentaur:spider_centaur ~ ~ ~`.
4. Inspect hat brim (no clipping), single red glowing eye, asymmetrical skull plates, purple cracks, and spider back growths.
5. Verify skull ornaments follow head when shooting, and that eight legs still animate correctly.
6. Check regular spawning at night, bow damage and wall climbing on a dedicated server.

## Build
Run `gradle clean build` from this directory. Output:
`build/libs/spidercentaur-1.21.1-0.2.0.jar`.
This repository's Minecraft Mod Build Hub publishes a GitHub pre-release after CI success.

## Known limitations
The first iteration intentionally reuses vanilla body skins with custom decorative geometry. Growth/hat fit must be visually checked inside a Minecraft client. CI verifies compilation and jar metadata but not gameplay/rendering.
