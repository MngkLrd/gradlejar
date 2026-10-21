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

### v0.1.12 regression
- Remove incompatible JEI++ temporarily; keep JEI itself installed.
- Reenter an existing world twice; verify no crash.
- Confirm JEI displays recipes for seax, shield, sword, axes and armor.
- On dedicated server, scan log for `Parsing error loading recipe norsegear:` (must be absent).
- Armor visuals still use vanilla geometry; custom 3D armor redesign is not part of this emergency recipe hotfix.


### Norse Gear 0.1.13 3D render regression tests
1. Wear one piece at a time and then full set. Ensure armor does not render whole suit when a single part is equipped. Inspect Steve, Alex, crouching, running and swimming.
2. Hold Javelin for 10+ ticks then release; short tap must NOT spawn projectile. Recover thrown item from ground. Test inventory stack in survival/creative.
3. Hold Spear then throw and retrieve. Verify no extra copy, no unintentional double consumption, correct durability.
4. Hold Throwing Axe; release, observe spin and recovery. All three need visible 3D geometry from front and side.
5. Block with all three shields (right mouse) and verify held-model thickness, the boss, damage prevention and +2 armor while bossed shield is in offhand.
6. War Horn produces audible goat horn music and only buffs allied nearby players.
7. With JEI (without incompatible JEI++), inspect 24 crafting recipes, enter/leave the same world twice.
8. Server boot test validates registry/worldgen and recipe parsing only. It does **not** render anything.


### Norse Gear 0.1.13 3D render regression tests
1. Wear one piece at a time and then full set. Ensure armor does not render whole suit when a single part is equipped. Inspect Steve, Alex, crouching, running and swimming.
2. Hold Javelin for 10+ ticks then release; short tap must NOT spawn projectile. Recover thrown item from ground. Test inventory stack in survival/creative.
3. Hold Spear then throw and retrieve. Verify no extra copy, no unintentional double consumption, correct durability.
4. Hold Throwing Axe; release, observe spin and recovery. All three need visible 3D geometry from front and side.
5. Block with all three shields (right mouse) and verify held-model thickness, the boss, damage prevention and +2 armor while bossed shield is in offhand.
6. War Horn produces audible goat horn music and only buffs allied nearby players.
7. With JEI (without incompatible JEI++), inspect 24 crafting recipes, enter/leave the same world twice.
8. Server boot test validates registry/worldgen and recipe parsing only. It does **not** render anything.


### Norse Gear 0.1.13 3D render regression tests
1. Wear one piece at a time and then full set. Ensure armor does not render whole suit when a single part is equipped. Inspect Steve, Alex, crouching, running and swimming.
2. Hold Javelin for 10+ ticks then release; short tap must NOT spawn projectile. Recover thrown item from ground. Test inventory stack in survival/creative.
3. Hold Spear then throw and retrieve. Verify no extra copy, no unintentional double consumption, correct durability.
4. Hold Throwing Axe; release, observe spin and recovery. All three need visible 3D geometry from front and side.
5. Block with all three shields (right mouse) and verify held-model thickness, the boss, damage prevention and +2 armor while bossed shield is in offhand.
6. War Horn produces audible goat horn music and only buffs allied nearby players.
7. With JEI (without incompatible JEI++), inspect 24 crafting recipes, enter/leave the same world twice.
8. Server boot test validates registry/worldgen and recipe parsing only. It does **not** render anything.


### Norse Gear 0.1.13 3D render regression tests
1. Wear one piece at a time and then full set. Ensure armor does not render whole suit when a single part is equipped. Inspect Steve, Alex, crouching, running and swimming.
2. Hold Javelin for 10+ ticks then release; short tap must NOT spawn projectile. Recover thrown item from ground. Test inventory stack in survival/creative.
3. Hold Spear then throw and retrieve. Verify no extra copy, no unintentional double consumption, correct durability.
4. Hold Throwing Axe; release, observe spin and recovery. All three need visible 3D geometry from front and side.
5. Block with all three shields (right mouse) and verify held-model thickness, the boss, damage prevention and +2 armor while bossed shield is in offhand.
6. War Horn produces audible goat horn music and only buffs allied nearby players.
7. With JEI (without incompatible JEI++), inspect 24 crafting recipes, enter/leave the same world twice.
8. Server boot test validates registry/worldgen and recipe parsing only. It does **not** render anything.


### Norse Gear 0.1.13 3D render regression tests
1. Wear one piece at a time and then full set. Ensure armor does not render whole suit when a single part is equipped. Inspect Steve, Alex, crouching, running and swimming.
2. Hold Javelin for 10+ ticks then release; short tap must NOT spawn projectile. Recover thrown item from ground. Test inventory stack in survival/creative.
3. Hold Spear then throw and retrieve. Verify no extra copy, no unintentional double consumption, correct durability.
4. Hold Throwing Axe; release, observe spin and recovery. All three need visible 3D geometry from front and side.
5. Block with all three shields (right mouse) and verify held-model thickness, the boss, damage prevention and +2 armor while bossed shield is in offhand.
6. War Horn produces audible goat horn music and only buffs allied nearby players.
7. With JEI (without incompatible JEI++), inspect 24 crafting recipes, enter/leave the same world twice.
8. Server boot test validates registry/worldgen and recipe parsing only. It does **not** render anything.

### 0.1.15 targeted in-game regressions

- Equip head, chest, legs and boots one at a time, then together; look for ghost pieces or missing children.
- Walk, sprint, crouch, swing both arms: breastplate/pauldrons/greaves must follow animations.
- Throw a spear/javelin forward, diagonally upward and downward; tip must face velocity, not backwards.
- Throw an axe and observe end-over-end tumble; test block hit, mob hit and dropped stack pickup.
- Test throwing a spear at 1 durability: it should not make an immortal projectile or duplicate the item.
- Verify blocked hits using all three shields and the umbon shield offhand +2 armor; sound from war horn.
- Test fresh world and reopening saved world with standard JEI only (known incompatibility with JEI++ is external).

### 0.1.15 regression checks

- `python3 tools/validate_3d_models.py` checks 3 shield models and 3 projectile item models.
- Inventory crash from latest(6).log points to `jei_plus_plus` and missing JEI, **not** Norse Gear. Remove JEI++ and duplicate old Norse Gear JAR before reproducing.
- Server smoke test does not exercise client UI models or inventory screen. Client screenshot/live testing is required before marking art verified.
