# Phantom Lure 0.1.1

Minecraft 1.21.1 / NeoForge 21.1.x / Java 21

## Gameplay
- The lure is a normal placeable block: **day or night, indoors or outdoors**. It does not immediately disappear when placed in daylight.
- It waits until **Overworld nighttime** (13,000-22,000 day ticks) before starting its 12-second countdown.
- A lure without an inserted diamond summons **one vanilla phantom**.
- Before the countdown ends, right-click with **one extra diamond** to insert it in the center socket and summon **3-5 vanilla phantoms** instead.
- After summoning, the lure disappears. Vanilla phantom stats and loot tables are unchanged.
- If any player within 96 blocks sleeps, the lure is destroyed without dropping items; it summons nothing.
- If dawn interrupts an active countdown, that lure breaks without drops. A daytime lure that has not started counting down remains until nighttime.
- The mod's summoned phantoms are silently removed when night ends or a nearby player sleeps, avoiding daylight-burning membrane farms. Natural phantoms are untouched.
- Manually breaking the lure before it summons returns the block and the inserted diamond (if any).
- Placement in dimensions other than the Overworld works, but the lure remains inactive there.

## Crafting
```text
     String
Amethyst  Redstone  Amethyst
Ender Pearl Diamond Ender Pearl
```
The optional swarm charge requires **another diamond** after the lure is placed.

## Build
Java 21, Gradle 9.2.1, Minecraft 1.21.1, NeoForge 21.1.262.

```sh
gradle --no-daemon clean build
```

Expected JAR: `build/libs/phantomlure-1.21.1-0.1.1.jar`

## In-game test checklist
1. Daytime placement on grass, stone, and other solid ground succeeds and leaves a visible block.
2. The placed lure waits during daytime, then begins countdown at night.
3. Nighttime placement on solid ground works without any sky exposure checks.
4. A normal lure summons one phantom after ~12 seconds of night.
5. Right-click with one diamond; the central model changes and 3-5 phantoms spawn.
6. Sleeping during the countdown destroys the lure without a summon or refunds.
7. Sleeping after the summon despawns tagged phantoms; natural phantoms are unaffected.
8. Dawn during a live countdown cancels it, and summoned phantoms disappear at dawn without burning and dropping membranes.
9. Save and reload the world during a countdown to confirm the timer/charge state is retained.
10. Break a charged lure manually: collect the lure and inserted diamond.
11. Check crafting grid and JEI for the bottom row Ender Pearl / Diamond / Ender Pearl.
12. Test on a dedicated server (all gameplay and registration code stays common/server-side).

## Notes
- **No changes to the textures, 3D models, blockstate JSON, or item visuals in 0.1.1.**
- GitHub Actions build success is not an in-world test.
