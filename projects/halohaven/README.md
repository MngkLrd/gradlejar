# Halo Haven 1.3.0 – Skyseed & True UV

**Minecraft 1.21.1 | NeoForge 21.1.x | Java 21.**

A bright fantasy pocket of the skies, now with a cohesive architectural language (pearl/aged brass/turquoise), shaped roofs, hand-painted materials, sky gardens, three district styles, boss encounters, a craftable endgame relic, translucent glass, and heavenly inhabitants.

## Installation and build

- Copy `halohaven-1.21.1-1.3.0.jar` to `mods/`.
- **NeoForge 21.1.197** is used to compile; runtime metadata requests **at least 21.1.80**, not the newest patch.
- Java 21, Gradle 9.2.1.
- Build: `gradle --no-daemon clean build`.
- Output: `build/libs/halohaven-1.21.1-1.3.0.jar`.
- Client and server both require the mod. No extra assets or resource packs.

## Creative inventory

All 27+ registered mod blocks and items, materials, mob eggs and entry tools
now appear in a **dedicated Halo Haven tab**, identified by the Skyseed icon.
They no longer clutter the vanilla Building Blocks, Tools or Food tabs.
Both English and Russian tab labels are included.

## Skyseed: a second way to reach the portal

**Skyseed / Семя небосвода** is a new, consumable item with a bespoke feather-and-eye icon.

Craft on a 3×3 table:

```
FEF
QGQ
 A
```

- **F** = Feather (2)
- **E** = Eye of Ender (2)
- **Q** = Quartz Block (2)
- **G** = Ghast Tear (1)
- **A** = Amethyst Shard (1)

Use it in the **Overworld under an open sky**. The item takes one charge,
places an oasis around 19 blocks to the east of the launch column so that your
ascending player cannot crash against the new island's underside, and lifts
the player through a visible column of cloud and enchanted particles.

The small floating island is assembled over roughly 20-25 game ticks:
cloudstone underside, skygrass top, sky orchard, a tiny sealed pond,
a carved marble ceremonial dais, surrounding lanterns and flowers.
Its centre already contains the ritual **quartz foundation** and four
**diagonal amethyst blocks**. The player places only a **Bell** on the central
quartz, and uses the **Ascension Sigil** as before.

The terrace is at least Y=183 and automatically goes higher over tall terrain.
There is a collision / space check before consumption to avoid overwriting
existing builds. If construction is blocked, no Skyseed is spent.

## Portal animation

- Every time a ritual is completed by hand placing the final relevant
  block (quartz, corner amethyst or bell), a server-driven spiral forms
  around the bell, rising for about 55 ticks with amethyst chimes.
- Right-clicking a valid ritual bell with the Ascension Sigil plays a
  48-tick portal-opening sequence before teleporting the player.
- Portal preparation is visible to all nearby clients, because particles
  originate on the server. If the bell is broken or the player walks
  far away during activation, the pending transfer is cancelled safely.

## Correct model UV maps

Old Halo Haven textures painted motifs across arbitrary rectangular regions
and often landed on the WRONG parts of the model. The faulty painting was
removed. One dedicated `uvtextures.gradle` module now draws only mapped
cuboid faces, with explicit named parts and empty/unused pixels transparent.

| Creature | Exact PNG dimensions | Defined mapped parts |
| --- | --- | --- |
| Skyfolk | 64×64 | Villager head, nose, hood, robe, coat, folded arms and legs |
| Cloud Hare | 64×32 | Rabbit head, ears, body, paws, legs and tail |
| Lumen | 32×32 | Allay head, chest, arms and individual flat wings |
| Dawn Guardian | 128×128 | Iron Golem head, torso, arms, leg sections and breastplate |
| Dusk Wraith | 32×32 | Vex head, torso, arms and wings |
| Eclipse Revenant | 32×32 | Vex head, mask, torso, arms and wings |

Model-part mapping is tied to the corresponding Mojang cuboid UV offsets,
not random pixel placement. The generator also clears stray pixels outside
registered face regions; this can be checked in its Gradle output.
It does **not** claim that the custom-rigged 3D animation models already exist;
those are still vanilla-derived shapes with custom textures and halos.

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
