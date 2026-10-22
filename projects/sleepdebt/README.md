# Sleep Debt 2.6.0-alpha.1

Minecraft 1.21.1, NeoForge 21.1.x, Java 21.

## What's new
- **Clay Incense Burner**: fueled 3D block, burns for up to 10 minutes, consumes incense bundles or dried herbs. Every 2 seconds it calms players within seven blocks, including multiple players, without accumulating permanent negative effects. Flame/smoke uses vanilla particles.
- **Incense Bundle**: craftable fuel lasting 120 seconds.
- **Herbal Poultice**: a quick, cooldown-restricted emergency treatment for Stress and Fatigue, but *never* a replacement for sleep.
- **Sleep Journal**: report of current Sleep Debt / Fatigue / Stress, and the last recorded sleep quality and amount recovered. No custom non-native font, chat-based server report.
- **Real sleep assessment**: roof, safety, wet weather, biome temperature, dreamcatcher, pillow, mask, active incense and calming effects all contribute to a quality score. An on-screen wake-up report shows the result.
- **Microsleeps** at extreme exhaustion: short darkness/movement disruption, only while off stimulants; configurable in server TOML. Never forces the player to lose items, teleport or sleep involuntarily.
- Tags at data/sleepdebt/tags/item/incense_fuels.json support modpack recipes and compatible modded incense.
- Three new 16x16 item textures and original block textures. New Patchouli EN/RU chapter and recipes; JEI/EMI standard recipes.
- Existing world IDs, NBT storage key (sleepdebt_v2), old model IDs, old recipes and the four core integration declarations preserved.

## Dependencies
JEI and EMI client-required; Jade and Patchouli as in META-INF/neoforge.mods.toml. Accessories remains optional for the mask. Comforts is optional.

## Build
`gradle clean build`
Expected: `build/libs/sleepdebt-1.21.1-2.6.0-alpha.1.jar`

## Test
`/sleepdebt debug set 940 840 880` to simulate deprivation.
Craft and place incense burner. Right click while holding fuel to add 30 seconds from herbs or 120 seconds from an incense bundle. Empty hand shows remaining burn time.
Wait in its 7-block radius and use `/sleepdebt status` to check Stress/Fatigue reduction.
Use journal to see last night report. Sleep outside in rain versus indoors and test differences. Poultice: use while stressed, check 90-second cooldown.
Use JEI/EMI, Patchouli, Jade, with and without Accessories. Check dedicated-server startup.
CI compilation and JAR verification do **not** substitute for in-game testing.

