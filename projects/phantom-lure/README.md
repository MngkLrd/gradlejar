# Phantom Lure 0.1.0

Minecraft 1.21.1 / NeoForge 21.1.x / Java 21

## Gameplay
- Place the Phantom Lure outdoors in the Overworld at night (13,000–22,000 day ticks).
- The lure starts a **12 second** countdown and summons **one vanilla phantom**.
- Before the countdown ends, right-click with **one diamond** to put it into the center socket and summon **3–5 vanilla phantoms** instead.
- The device is consumed on summoning. Vanilla loot tables are unchanged.
- If any player within 96 blocks goes to sleep, or night ends during the countdown, the lure breaks without drops and summons nothing.
- Phantom entities created by this mod are tagged and silently removed as dawn approaches or when a nearby player sleeps. Natural phantoms are untouched.
- Manual mining before activation returns the lure and any installed diamond. Explosions / sleeping cancellation do not refund the diamond.
- No effects, phantom buffs, custom mobs, interface, or mandatory dependencies.

Recipe:
```
  String
Amethyst Redstone Amethyst
 Iron   Iron    Iron
```

## Compile
Java 21, Gradle 9.2.1, Minecraft 1.21.1, NeoForge 21.1.262.

```sh
gradle --no-daemon clean build
```

Result: `build/libs/phantomlure-1.21.1-0.1.0.jar`

## Test
1. Place indoors / in daylight / in another dimension: placement denied.
2. At nighttime outdoors, place a lure. After 12s one phantom should spawn, and the block should disappear.
3. Place a second lure, right-click with a diamond before 12s. Diamond appears in the central socket; 3–5 phantoms spawn.
4. Try sleeping before the countdown finishes: the lure should disappear without drops or summons.
5. Sleep after summoning: summoned phantoms vanish without membrane drops; natural phantoms are unaffected.
6. Allow dawn to approach: summoned phantoms despawn before vanilla fire damage.
7. Reload world during countdown: countdown and diamond charge persist.
8. Break a charged lure by hand: get lure + diamond back.
9. Dedicated server: no client classes loaded by common logic.

## Limitations
- This is a build-verified MVP, not a claim of in-game runtime validation.
- The block has no countdown HUD or advanced animation yet.
- Lure placement is restricted to open sky.
