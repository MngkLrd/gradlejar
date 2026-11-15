# No Food in Furnace v1.0.1

NeoForge 21.1.x, Minecraft 1.21.1, Java 21.

- Forbids food smelting in the ordinary furnace, including modded foods.
- Food is detected by cooking recipe category and the FOOD data component on input or result.
- Shows vanilla anvil's red error X over the ordinary furnace arrow when forbidden food is inserted.
- Non-food recipes, smoker, campfire, and blast furnace are unaffected.
- Dedicated-server-safe: all GUI code registered for client only.

Build using Java 21 and `gradle --no-daemon clean build`.
Expected output: `build/libs/nofoodinfurnace-1.21.1-1.0.1.jar`.

Testing: furnace food (no output, red X), furnace iron ore (works, no X), smoker food (works, no X), campfire food (works), modded food whose input/output has the FOOD component (blocked).
