# Halo Haven 1.1.0 — Minecraft 1.21.1 / NeoForge

A fully self-contained heavenly sky-realm expansion for Minecraft 1.21.1. Requires Java 21. **Compiles against NeoForge 21.1.197 but does not demand the newest NeoForge build.** The mod dependency in `neoforge.mods.toml` accepts NeoForge `[21.1.80,)`, together with Minecraft `[1.21.1,1.21.2)`.

## Entering Halo Haven

1. Craft the **Ascension Sigil** from 4 amethyst shards, 2 feathers, 2 ender pearls and 1 ghast tear.
2. Build a 3×3 platform **at Y=170 or higher**. A quartz block must sit in the centre of the platform and amethyst blocks must occupy the four diagonal corners.
3. Put a bell on top of the central quartz. Leave the sky directly above the bell unobstructed.
4. Right-click the bell while holding the sigil. You arrive on the central village island in Halo Haven.
5. Right-click in the air with the sigil in Heaven to return to your stored Overworld position.

## Regions and exploration

The sky realm has its own dimension type, pastel biome, daylight ambience and vast floating cloudstone islands. In addition to background End-noise islands, settlements are created programmatically on self-supporting islands around points spaced 512 blocks apart. Villages appear on first approach, with deterministic positions. This is not vanilla village worldgen.

Each village is built from an original pale-mint/gold/pearl architecture and includes:
- Four furnished houses with windows, varied roofs, workstations, rare supply chests and special inhabitants
- A sunlit circular fountain and paved crossroads
- Gold-and-glass observatory with an explorer's supply chest and spyglass
- Sacred meadow and Pilgrim Altar with constellation lights
- Cherry groves, flower gardens, pillars and meadow wildlife
- Ruined satellites with Dusk Wraiths, rare chests and suspended glazed bridges
- A portal-like Aether Waystone atop a small ceremonial structure

**Aether Waystones:** Right-click to travel to the next village along the X axis. Sneak and right-click to go the other direction. Distances are exactly 512 blocks. The destination generates before the teleport.

**Skybound Compass:** Right-click in Halo Haven to show the nearest settlement's coordinates, distance, angle and your Heavenly Favor.

## New creatures

| Entity ID | Identity | Behavior |
| --- | --- | --- |
| `halohaven:skyfolk` | Skyfolk / Celestial villager | Merchant with unique trades for Skyberries, Stardust and Sunshards. Wears an original painted celestial tunic; no vanilla villager clothing overlays |
| `halohaven:cloudhare` | Cloud Hare | Gentle rabbit-like island wildlife. Offer dandelions to receive Favor |
| `halohaven:lumen` | Lumen | Floating luminous helper based on Allay behavior; accepts Skyberries and returns Stardust, once every 3 minutes per creature |
| `halohaven:dawn_guardian` | Dawn Guardian | Golem village protector. Nearby players gain a short periodic regeneration aura |
| `halohaven:dusk_wraith` | Dusk Wraith | Aggressive aerial ruin guardian based on Vex behavior. Drops Stardust |

Each has **a custom UV-specific pixel-art texture**. Spawn eggs are in the creative inventory. Vanilla Overworld villagers are not retextured; only Skyfolk have the new look.

## Heavenly Favor and rewards

The Pilgrim Altar appears in the sacred garden. Offer one **Skyberry** to receive one Favor. Give a Cloud Hare a dandelion for another point, or visit Lumens to exchange Skyberries for Stardust.

At 12 Favor, right-click the altar with an empty hand to redeem **two Sunshards**. The balance persists on the player.

You can also mine **Star Ore** on the island's lower stone layers, or fight Dusk Wraiths. Stardust is used to craft new equipment and travel supplies.

## Blocks and supplies

### 15 fully registered blocks
Cloudstone, Sky Meadow, Pearl Bricks, Sungold Tiles, Azurewood Planks, Aurora Glass, Starshade Lantern, Dawn Glass, Moonstone, Celestial Mosaic, Gilded Pillar, Cloud Lantern, Aether Waystone, Star Ore and Pilgrim Altar.

**Glass has proper transparency:** both Aurora and Dawn Glass use `TransparentBlock`, `noOcclusion()`, truly transparent PNG pixels and explicit `minecraft:translucent` block-model render types. No opaque solid colored cubes.

All blocks have generated 32×32 PNG textures, blockstate JSON, block/item models, language names and loot tables. Mob textures are 64×64 (128×128 guardian). Spawn egg models and item icons are included in the JAR. Sources produce assets at build time and embed them directly in the mod JAR; players do not need an extra texture pack.

### Items
Ascension Sigil, Skyberries, Stardust, Sunshard, Skybound Compass and Aether Tonic. The tonic grants slow falling, jump boost and speed. Stardust comes from Star Ore, Dusk Wraiths and Lumen exchanges. Skyfolk have custom offers.

## Build and install

- Minecraft: **1.21.1**
- Loader: **NeoForge** (compile: 21.1.197; minimum declared: 21.1.80)
- Java: **21**
- Gradle: **9.2.1**
- `gradle --no-daemon clean build`
- Output: `build/libs/halohaven-1.21.1-1.1.0.jar`
- Copy the JAR to your game's `mods` folder on both client and dedicated server.

## QA and limitations

Automated CI validates the Gradle build, resulting JAR and resource files. Gameplay must still be checked in a real client (especially pack/model rendering and positioning). Existing 1.0 Skyfolk villages retain terrain and house structures to avoid destroying builds; old vanilla villagers in Halo Haven migrate to Skyfolk upon nearby player activity. New ruins, bridges and shrines appear in newly generated settlements.

Known limitations: mob models reuse familiar vanilla rigs (Allay, Rabbit, Iron Golem, Villager, Vex), with fully custom skins and contextual gameplay. The dimension has one biome and is not yet a multi-biome worldgen system. Village generation is performed synchronously on first approach and can cause a brief pause. Waystones currently link along X only. No unique music, boss encounter or custom animation rig in this version.
