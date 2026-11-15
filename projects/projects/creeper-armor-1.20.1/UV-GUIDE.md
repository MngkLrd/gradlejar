# Creeper Armor 0.3.0 texture map

This is a Vanilla biped 128x64 HD pixel-cluster texture (2x texel sampling relative to native 64x32). No animations, client-side renderers or shaders are needed.

**Layer 1:** helmet, chestplate and boots.
**Layer 2:** leggings.

The layout follows vanilla armor UV with every 64x32 coordinate multiplied by two:

| Surface | 128x64 physical pixels |
|---|---|
| Helmet front | x16..31, y16..31 |
| Helmet back | x48..63, y16..31 |
| Torso front | x40..55, y40..63 |
| Torso back | x64..79, y40..63 |
| Arm front | x88..95, y40..63 |
| Leg front | x8..15, y40..63 |

The neighboring side faces use separate gradients and metal trims. The shield JSON faces outward in both hands (0-degree yaw, instead of 90-degree edge-on yaw).

### In-game QA

1. Test helmet visor in first person inventory preview and third person on Steve/Alex.
2. Test all 4 armor slots against armor with same durability at close range to TNT; Creeper Armor cancels player damage but each piece loses 4 durability per blast.
3. Verify the 7 items in the dedicated creative tab.
4. Test JEI with and without the optional JEI mod installed: crafting recipes, translated information pages, no hard runtime crash.
5. Raise Creeper Shield in mainhand and offhand, first person and F5. If a hand offset looks wrong, adjust only the corresponding item model display transform.
6. Check entire body from front, left, right and back, including leggings layering over boots.

The complete texture generator and larger QA preview are included in the separately distributed 0.3.0 source ZIP.
