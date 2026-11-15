# TraumaCraft 0.2.0 in-game UI testing

1. Install traumacraft-0.2.0.jar in NeoForge 1.21.1 with Java 21.
2. Enter a world and run /trauma. Seven zones should be intact.
3. Press E at GUI scale 2 or 3. The inventory shifts to the left and shows the BODY STATUS medical panel attached to the right.
4. Run /trauma hit left_arm 35. The left arm bar should drop in the inventory and on the HUD.
5. Run /trauma fracture right_leg. Inspect the yellow fracture pixel, test the slowdown, and take a splint from the creative Tools tab.
6. In inventory, click the splint in a slot (so it follows the cursor), then click R.LEG on the new panel. The server should consume exactly one splint and clear the fracture.
7. Run /trauma bleed left_arm 2 and treat the left arm with a bandage.
8. Damage a zone and use medkit on that specific zone; confirm the affected zone alone gains 25 HP.
9. Test at different GUI scales, in survival and creative, and on a dedicated server.
10. Confirm the recipe-book toggle and crafting controls still work. Record screenshot, GUI scale and first error log if something is misaligned.

Notes: The anatomy panel is intentionally a transparent-ish pixel-rendered extension, not a new texture pack. The game HUD is left-bottom. Existing vanilla hearts still apply.
