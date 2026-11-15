# Anvilborn 0.1.1 — risky equipment forging

Minecraft **1.21.1**, **NeoForge 21.1.262**, **Java 21**, ModDevGradle **2.0.148**.
Independent project in the Minecraft Mod Build Hub.

## Play
- Sneak + right-click a vanilla anvil with the **main hand**.
- Place a single non-stackable, damageable equipment item in the forging slot.
- Each strike costs **one experience level** (creative mode free).
- Outcomes: unchanged, improved, great success (+2), worsened (-1), or item destruction. Each anvil hit plays a metallic sound.
- The 6-hit bar builds towards **two Frenzy strikes**, which cannot shatter the item. Bar/Frenzy are stored on the item itself, not on the menu.
- Stop at any time and take your gear. The working item is returned on menu close.
- Forging quality ranges from **-2** to **+10**. Positive aspects roll on new tier thresholds; negative aspects may appear on a damaged item.
- **50 seeded aspects** across melee, mining, armor, ranged and universal groups; 10 in each. Tooltips expose rolled names and ranks (I/II/III).
- Vanilla/modded damageable items are accepted; unknown items receive general aspects.

## Caveats for 0.1.0 alpha
All 50 named aspects are selectable and route to implemented attribute/combat/mining/durability/XP/fall effect families; several share an effect family with different strengths and rarities. The result is a 50-aspect starting catalog, not 50 wholly unique active skills. Gameplay testing of balance and compatibility is still pending.
New pixel-art texture-backed forge window (256x256) and widget atlas (256x128); native mouse/keyboard-accessible button, GUI state sprites, rarity badge, risk, 6-segment progress and strike animation. Forging reuses vanilla anvil sounds. No game-client testing claimed.

## CI build
Actions > Minecraft Mod Build Hub > Run workflow > `projects/anvilborn`.
On main push, autodiscovery builds all projects. PR builds validate only and do not publish.
Release JAR: `build/libs/anvilborn-0.1.1.jar`.

## UI 0.1.1 design\n- Screen texture: 256x256 source, rendered at natural size; forge stage and inventory use aligned Minecraft slot coordinates.\n- Widgets texture: 256x128 source with 200x22 button strips (normal/hover/pressed/disabled), 30x9 progress sprites, hammer and sparks.\n- Text remains localization-driven; no lettering is baked into texture images.\n- No external asset/runtime dependencies.\n\n## Manual runtime checklist
1. Sneak-right-click an anvil and forge a sword. Check the bar and hammer sound.
2. Confirm XP level is consumed exactly once per strike, except creative mode.
3. Take the item out, close the GUI, reopen, and verify quality/aspects/frenzy persist.
4. Try inventory shift-click, closing with an occupied slot, and breaking the anvil while open.
5. Try an enchanted item, modded damageable item, bow, pickaxe and armor.
6. Verify tooltips and visual quality colors, fail outcome, item shatter, multiplayer server sync.
