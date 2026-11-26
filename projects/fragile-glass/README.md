# Fragile Glass 1.0.0
Minecraft 1.21.1 / NeoForge / Java 21 / Gradle 9.2.1.
Build: gradle --no-daemon clean build
Output: build/libs/fragileglass-1.21.1-1.0.0.jar

- One click breaks vanilla glass, including tinted and stained variants and panes.
- Silk Touch uses normal loot rules.
- Arrows and tridents break glass and continue with 75% velocity; fast other projectiles break glass, eggs and snowballs only panes. Potions, experience bottles and ender pearls retain vanilla behavior.
- Living entities falling at least three blocks break glass directly underfoot; fall damage is unchanged.
- Impact shattering has extra block particles and vanilla break effects; no block drop.
- Player-caused impact fires BreakEvent and checks build permissions; nonplayer impact respects mob griefing. Third-party claim support must be tested.
- Data pack tags fragileglass:fragile_blocks, fragileglass:fragile_panes, fragileglass:reinforced allow explicit mod integration.
- No tick scans, custom networking, new blocks or recipes.
- In-game test: bare hand, Silk Touch, arrow/trident through pane, snowball vs pane/block, fall on glass, protected area and mobGriefing false.
