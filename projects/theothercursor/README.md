# The Other Cursor (NeoForge, Minecraft 1.21.1)

A tiny client-side psychological horror mod. An extra mouse cursor appears when you open
your player inventory, chest, workbench, or another inventory-style menu.

## Behavior
- The cursor begins motionless near a screen corner.
- It then drifts towards items, with a strong preference for valuables and swords.
- Approach it with the real pointer and it jumps out of the way.
- It pauses, watches, and sometimes tries to carry a fake single-item sprite across the UI.
- Items **never** actually move: no Slot.set, no menu clicks, no networking, no saved changes.
- The cursor is hand-drawn in pixel shapes using GuiGraphics, without external textures.
- No world/server events and no extra dependencies beyond NeoForge.

## Build
Use Java 21 and Gradle 9.2.1:
`gradle clean build`
Output: `build/libs/theothercursor-1.0.0.jar`

This repository's CI also accepts `projects/theothercursor` as a build directory.
Install the resulting JAR in the **client** mods folder for Minecraft 1.21.1 + NeoForge 21.1.x.

## Test checklist
1. Open inventory and wait 2-4 seconds; the second cursor should start wandering.
2. Equip a diamond or netherite sword, then reopen inventory; see it drift toward that slot.
3. Move your real cursor within about 23 GUI pixels; the ghost must immediately recoil.
4. Leave inventory open for at least a minute to observe rare false drag attempts.
5. Confirm items do not move or duplicate in the actual inventory.
6. Test while connected to a multiplayer server: no server installation required.

## Limitations
- Rendered inside vanilla and modded AbstractContainerScreen menus only.
- Faux drags draw a duplicate icon rather than altering the real slot rendering.
- GUI elements added by other mods may overlay the cursor if rendered later.
- GitHub CI proves compilation, not an in-game runtime smoke test.
