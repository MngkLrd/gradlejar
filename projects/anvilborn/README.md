# Anvilborn 0.1.0 — risky equipment forging

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
Not every aspect is yet connected to gameplay. The stat and combat effects are implemented; the XP, durability preservation and fall reduction effect families need dedicated gameplay hooks. These are visible as experimental aspect definitions, not claimed to be completed features.
No custom wav/ogg assets yet: the mod reuses vanilla anvil sounds, and the GUI is code-drawn in Minecraft's pixel style. No game-client testing claimed.

## CI build
Actions > Minecraft Mod Build Hub > Run workflow > `projects/anvilborn`.
On main push, autodiscovery builds all projects. PR builds validate only and do not publish.
Release JAR: `build/libs/anvilborn-0.1.0.jar`.
