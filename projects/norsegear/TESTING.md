# Norse Gear 0.1.10 testing checklist

1. Gradle: `gradle --no-daemon clean build` under Java 21. Inspect first compiler error if failed.
2. Start client with only NeoForge + Norse Gear (no JEI). Confirm loading without `ClassNotFoundException` from JEI plugin.
3. Repeat with JEI 1.21.1 NeoForge: search bog iron; `U` shows the conversion recipe and a JEI ingredient information entry with location and tool cost. (`R` on the iron ingot will list the recipe among other sources.)
4. Create a new world or explore **new chunks**. In plains, forests, taiga, and swamps dig around 2–4 blocks below the local surface for natural brown deposits. Test at **different absolute Y-levels** (including hills, not only Y55-62). Old generated chunks won't retrofit. Count deposits over 50+ new chunks from several biomes; a 50% chunk gate and 2 attempts are NOT guaranteed ore. Ensure no grass-block replacement and no air-exposed deposits. Deserts/sand and exposed stone are not viable targets; that's intentional. Verify Nether/End are excluded.
5. ` /setblock ~ ~ ~ norsegear:bog_iron_deposit` (omit leading space) for quick testing.
6. Break deposit by hand, wooden shovel, stone shovel, iron shovel, iron pickaxe. Only stone-or-better shovels should give 1–2 drops.
7. Craft 1 bog iron + stone pickaxe in 2×2, confirm 1 iron ingot and pickaxe returned with +1 durability damage.
8. Try a nearly broken pickaxe: it must disappear when last durability used.
9. Try wooden pickaxe, 2 bog iron stacks, 2 pickaxes, unrelated extra ingredient, and no pickaxe. Must not yield iron ingot.
10. Try bog iron in furnace and blast furnace: must not cook. `forged_iron` still crafts from charcoal + iron ingot.
11. Run `gradle runServer`, connect a NeoForge client, test multiplayer crafting, JEI syncing and loot.
12. Test with JEI absent, with JEI present, and alongside existing modpack. Watch logs for recipe JSON or biome modifier errors.

## Visual review (32×32 sprites)

1. Inspect all five textures in inventory and hotbar at standard GUI scale; confirm silhouette survives display downscaling.
2. Hold the seax and both axes in first- and third-person; ensure `handheld` model orientation is acceptable.
3. Compare bearded vs Dane axe icons at a glance; head size, beard and handle should read differently.
4. Verify celtic/bossed shield textures are shown in the item inventory; their 3D held shield render remains a future task.
5. Verify the remaining 19 item textures and world-generation resources were not changed in this visual pass.

## Regression: worldgen crash on creating world (0.1.9)

1. Install the new 0.1.10 JAR, REMOVE 0.1.9.
2. Start Minecraft 1.21.1 NeoForge 21.1.264 (player-tested environment).
3. **Create a new world** and verify it loads fully: 0.1.9 failed here while decoding `norsegear:bog_iron`.
4. Check `latest.log` for an absence of `Failed to parse norsegear:worldgen/configured_feature/bog_iron.json`, `Unbound values`, or `No key block`.
5. Test `setblock ~ ~ ~ norsegear:bog_iron_deposit` then mine with a stone shovel.
6. Open a newly generated Overworld chunk and verify natural deposits under soil (not replacing grass blocks).
7. Verify 5 batch 4 inventory sprites appear at native 32x32 and all armor icon/UV assets remain unchanged.

### 0.1.11 art and runtime regression

1. Confirm all 24 inventory textures render and none are purple/black missing-texture squares.
2. Open creative inventory and survival inventory (1x, GUI Scale 2/3) to check silhouette and color contrast.
3. Check carved horn vs war horn and bog iron vs forged iron visually.
4. Create a new world with NeoForge 21.1.264 and confirm no `configured_feature/bog_iron` errors.
5. Keep a separate test save; the improved worldgen is newly generated chunks only.
6. Verify equipped armor in third-person and on an armor stand; render appearance is not confirmed by the CI smoke test.
