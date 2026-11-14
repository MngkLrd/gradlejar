# Halo Haven 1.2.0 – the Celestial Courts

**Minecraft 1.21.1 | NeoForge 21.1.x | Java 21.**

A bright fantasy pocket of the skies, now with a cohesive architectural language (pearl/aged brass/turquoise), shaped roofs, hand-painted materials, sky gardens, three district styles, boss encounters, a craftable endgame relic, translucent glass, and heavenly inhabitants.

## Installation and build

- Copy `halohaven-1.21.1-1.2.0.jar` to `mods/`.
- **NeoForge 21.1.197** is used to compile; runtime metadata requests **at least 21.1.80**, not the newest patch.
- Java 21, Gradle 9.2.1.
- Build: `gradle --no-daemon clean build`.
- Output: `build/libs/halohaven-1.21.1-1.2.0.jar`.
- Client and server both require the mod. No extra assets or resource packs.

## Ascension ritual

The original ascension ritual remains unchanged: craft the Ascension Sigil from 4 amethyst shards, 2 feathers, 2 ender pearls and 1 ghast tear. Above world Y=170, place a bell on quartz with four amethyst blocks diagonally beneath the corners, and keep the sky open. Right-click the bell with the Sigil. Right-click the air with the Sigil in Heaven to return home.

### Explore

The initial sky village is at X=0 Z=0, with further settlements every 512 blocks. Aether Waystones send you 512 blocks along X; sneak for the opposite direction. The Skybound Compass shows the nearest village, direction, distance, Favor and district.

Each district has its own ground treatment, garden density and building palette:

- **Pearl Court:** ivory architecture, warm gold, airy arcades.
- **Blooming Canopy:** exuberant turquoise canopy trees, floral paths and gardens.
- **Eclipse Verge:** rarer trees, violet moonstone paving and dark stained-glass accents.

First discovery of each district grants 4 Heavenly Favor. Districts follow a deterministic pattern from coordinates, not random volatile mobs.

A new settlement is assembled on first approach only and marked to prevent regeneration. Existing settlements from 1.0/1.1 stay intact: new basilicas, roofs and plants appear in **newly generated villages**. Old 1.0 vanilla inhabitants are migrated nearby to Skyfolk without deleting player builds.

### Architecture and pixel art

- Full-colour 64×64 *designed* textures for cloudstone, sky meadows, pearl bricks, scalloped sungold roofs, azurewood, violet moonstone, marbled mosaic, fluted brass pillars, lanterns, crystals, ore and ritual sites.
- Both Aurora and Dawn Glass use true semitransparent PNGs with stained-glass traceries, the translucent rendering layer, and nonoccluding `TransparentBlock` behavior.
- 3D JSON models for lanterns, Starshade crystals, Aether Waystones and Pilgrim Altars, instead of fully opaque full cubes.
- Real `StairBlock` / `SlabBlock` construction sets for sungold and pearl materials. Interconnecting azurewood fences for balconies.
- New cutout rendered Sky Crown Leaves (woodland trees) and Celestial Bloom cross-plane flowers.
- Fresher silhouettes: curved domed basilicas, stained-glass arches, multi-tier garden terraces, observatories, bridges to satellite wraith ruins, furnished houses and central plazas.
- Every new block/item/entity has a model, a texture and en_US/ru_RU naming, including spawn egg models.

### Creatures

- **Skyfolk:** custom painted face, pale robe, gold embroidery, animated three-dimensional nimbus; trade Skyberries, Stardust and Sunshards.
- **Cloud Hare:** pastel hare with gentle wildlife behavior and flower interactions.
- **Lumen:** luminous flying companion with pale opaline wings and turquoise halo; Skyberry offerings yield Stardust, with a personal cooldown.
- **Dawn Guardian:** massive ivory golem with a gold breastplate, glowing turquoise motifs and restorative aura.
- **Dusk Wraith:** violet nightmare from the satellite ruins, dark custom skin and subtle magical traces.
- **Eclipse Revenant:** enlarged mid-boss with boss healthbar and nimbus. Its second phase summons two wraiths at 55% HP. Spawns in some ruins and drops a mandatory Eclipse Seal.

For stability and compatibility, most mob geometry inherits familiar **vanilla skeletons**, but textures, visual attachments, spawning, placement and gameplay are mod-specific. This is not yet full bespoke rig/animation work.

### Gameplay loop

1. Perform the bell ritual and explore the central floating village.
2. Talk to Skyfolk, visit the basilica and observatory, discover new districts.
3. Mine Star Ore, feed Lumens with Skyberries, and make offerings to the Pilgrim Altar.
4. Earn Heavenly Favor (Skyberries, dandelions fed to Cloud Hares, wraith trial milestones, first discovery of each region). Exchange 12 Favor at the altar for 2 Sunshards.
5. Traverse glazed bridges to hostile moonstone ruins. Defeat Dusk Wraiths (3 and 8 defeat milestones) and the Eclipse Revenant.
6. Forge the **Sunward Oath Relic** from an Eclipse Seal, four Sunshards and four Stardust. Right-click to gain Slow Falling, Absorption, Regeneration and Fire Resistance on a **90 second cooldown**.
7. Craft the **Feather of the Open Sky** from a feather and four Sunshards. Holding it in either hand mitigates falls over the void.

Aether Tonic remains a consumable air-travel aid with Slow Falling, Speed and Jump Boost. Ruin chests, basilica archives, villager trades and active boss encounters supply useful materials.

## Tests performed

GitHub Actions compiles the mod and checks texture/model/blockstate recipes, real PNG files, the correct glass render type and biome/dimension JSON. A separate job starts a NeoForge 1.21.1 dedicated server and verifies readiness.

## In-game QA checklist

- On a **new world**, enter the Heaven dimension using the Sigil and check the central region renders at X=0/Z=0 around Y=111.
- Verify both glass types are transparent from inside/outside with other blocks visible through them.
- Inspect the rounded basilica, arches, pitched roofs, fences, illuminated trees and sky bridge from nearby and at render distance.
- Check no pink/black missing-texture faces on Skyfolk, Eclipse Revenant, Lumen and the other mobs.
- Kill a few Dusk Wraiths, then test Eclipse Revenant's bossbar, two-phase summon and guaranteed Seal drop.
- Craft/use the Sunward Oath Relic, test cooldown and the Aether Feather.
- Teleport using a Waystone along X to visit 3 district styles, monitor reward messages and Skybound Compass.
- Try a dedicated multiplayer server. Teleport, trading, Favor, loot and structure generation must all be server-side.

## Known limitations

- Dedicated server start is verified, but this pipeline cannot establish gameplay visuals against a real interactive client. Final aesthetics need an in-game visual check and screenshot review.
- Original entity skeletons are still used, with hand-painted UV textures and custom 3D halos. Future improvement would include bespoke rigging and animation.
- Region variation is currently **code-driven district palettes**, not three independently registered noise biomes. Ambient sky is a single biome.
- First-time generation of a complex village can still cause synchronous chunk updates and a short pause.
- Altars and waystones have multi-element visual models but retain full-block collision.
- Waystones link in the X direction; there is no multidirectional travel UI yet.
