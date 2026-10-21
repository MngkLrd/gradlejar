# TraumaCraft 0.2.0

Minecraft 1.21.1 / NeoForge 21.1.262 / Java 21.

## GUI
- Press E. The vanilla inventory moves left, making room for a pixel-styled body health panel to its right.
- Seven body zones show local HP and damage colors (green, pale green, amber, red, black).
- Red squares indicate bleeding, yellow squares indicate fractures.
- Mouse over a zone for its exact HP, bleeding severity and fractures.
- Left click bandage, splint or medkit in a vanilla inventory slot to **pick it up on your cursor**. Then left-click the body zone (either silhouette or corresponding HP row) to apply it. The server checks and consumes the item. Left-click back on a slot to put remaining medicine down.
- When inventory is closed, translucent injury silhouette + local HP bars appears in the bottom left of the gameplay HUD.
- HUD and medical screen are client-side only. Local HP data syncs from server on login, respawn and damage/treatment.

## Existing gameplay
Seven independently saved HP zones (440 combined), approximate projectile and melee zone damage, leg fractures and slowdown, bleeding, medical items and /trauma commands.

## Prototype limitations
- The display is an extra panel attached to the right of vanilla inventory, not a complete inventory retexture. The extended panel needs at least 312 logical pixels horizontally; at lower resolutions the normal inventory remains unchanged.
- Some inventory modifications (recipe books and mods replacing InventoryScreen) might require compatibility adapters.
- Vanilla hearts still govern normal survival and death.
- Textures for medical items are vanilla placeholders.
- Body damage / hit detection are early prototypes and need gameplay QA.

## Build
Run `gradle --no-daemon clean build` with Java 21 and Gradle 8.14.3.
Output: `build/libs/traumacraft-0.2.0.jar`
