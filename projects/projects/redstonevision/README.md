# Redstone Vision 0.3.0-alpha

Minecraft **1.21.1**, **NeoForge 21.1.262**, **Java 21**, Gradle **9.2.1**.

## How to use
1. Craft Engineer's Goggles or run `/give @p redstonevision:engineer_goggles`.
2. **Hold the goggles and Shift + right-click** to open the charger. Put redstone
   in its slot: charging happens automatically (no button required).
3. Alternatively **wear the goggles and press Shift+V** to charge them without
   taking them off.
4. Goggles now **activate automatically when equipped with fuel**.
   Press `V` to switch the vision on/off manually.
5. Redstone drains at **1 dust per 60 server ticks (3 seconds)** only while
   vision is enabled; 64 dust gives at most 192 seconds.

HUD always shows the charge while goggles are equipped, including when off.
After the charge runs out, goggles turn off and require additional redstone.

## Visual design
Separate custom humanoid head model (front-mounted paired lenses, bridge,
side straps). Texture: 64x32 authored armor UV + 32x32 item icon.

## Important limitations
- The experimental world-transparency mixin still alters chunk render layers
  rather than individual block materials. It is **not guaranteed** to make every
  block precisely 70% transparent. Shader compatibility remains uncertain.
- Full Minecraft client and dedicated-server launch tests are still required.
- Full-bright, no-depth redstone component overlays should remain visible through
  ordinary world geometry while vision is active.

## Build
`gradle clean build` produces `build/libs/redstonevision-0.3.0-alpha.jar`.

## Manual test checklist
- Equip empty: visible low-charge HUD and Shift+V hint.
- Put 1 dust in charger: 60 ticks; no need to press the old button.
- Equip charged goggles: auto-on and redstone highlighting.
- Press V: off; press V: on; confirm off mode does not burn fuel.
- Shift+V: charge worn goggles; close menu; dust not duplicated.
- View from third person: goggles sit on face, not above player's head.
- Test second player and dedicated server, other helmets, different shaders.
