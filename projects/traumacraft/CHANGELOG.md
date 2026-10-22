# Traumatic 0.3.3 | NeoForge 1.21.1 UI hotfix

- Removed an obsolete second medical GUI and HUD that were rendering over the current anatomy inventory and showing a different body state cache. The only GUI is now TraumaInventoryScreen, and the only HUD is TraumaHud.
- Fixed the vanilla inventory texture overdraw caused by rendering a 320+ pixel image from a 256px inventory atlas. Vanilla texture now renders at exactly 176 x 166 pixels; expanded panel draws separately.
- Responsive right-side panel with 7 rows: exact current/max HP, explicit bleeding, fracture, critical states, and HP bars. Redesigned properly aligned anatomy zones; HUD uses the same zones.
- Mouse tooltips are clamped inside screen bounds. Treatment targeting by body part is preserved.
- Removed old UI-only networking, extra client HP cache and unused inventory mixin; primary ModNetwork and attachment data remain unchanged.
- Gameplay formulas, AI, body state, damage handling, item registry, recipes, JEI and 23 item IDs untouched.

Build: Java 21, NeoForge 21.1.262, Gradle 8.14.3. Expected output: build/libs/traumacraft-0.3.3.jar.
