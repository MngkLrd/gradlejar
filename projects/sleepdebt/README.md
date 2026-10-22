# Sleep Debt 2.4.0-alpha.1 — Field Survival Expansion

**Minecraft:** 1.21.1 · **Loader:** NeoForge · **Java:** 21 · **Gradle:** 9.2.1

## Features
- Three persistent stats: Sleep Debt, Fatigue, Stress. Client-synced native survival HUD.
- New four-slot copper coffee maker with native-themed GUI, 200-tick progress, ingredients, coal/charcoal fuel, output-only slot and inventory drops on break.
- Brewing coffee and herbal tea. Dried herbs from ferns. Caramel coffee, dream tea, bitter tonic, cocoa biscuit and Dream Shard.
- New 3D blocks: copper coffee maker, camp bedroll, camp awning, Rest Lantern and Dreamcatcher.
- Bedroll recovery from shelter, fire, lantern, pillow and mask, with cooldown. Dreamcatcher provides a stress-recovery interaction and bed bonus.
- Night hallucinations: native pixel shadow silhouettes and local soul particles on the client. Configurable off.
- The Sleepless mob: hostile only to severely tired players, darkness spawning, daylight retreat, spawn egg and rare Dream Shard loot.
- Server operator commands: `/sleepdebt status` and `/sleepdebt debug set 950 800 700`.
- Patchouli guide EN/RU and progression advancements, with JEI / EMI and Jade metadata.
- Original 16x16 pixel sprite generator for item and block textures and humanoid skin.

## Dependencies
Required: NeoForge 21.1.x; JEI, EMI and Jade on client; Jade and Patchouli according to mod metadata. Comforts is optional via vanilla sleep observation.

## Build
```bash
gradle clean build
```
Expected JAR: `build/libs/sleepdebt-1.21.1-2.4.0-alpha.1.jar`

## Testing
Read `CONTENT_2_4.md`. This CI tests Gradle compile and JAR structure only. Test dedicated server, client, and modpack compatibility manually. The camp is player-built, not worldgen. The Sleepless shares a vanilla zombie humanoid rig. Halo/hallucinations are client effects, not interactive mobs.

## Persistence
Old item and block IDs are preserved. Player state remains under NBT `sleepdebt_v2`. Old world compatibility is not yet verified in runtime.

