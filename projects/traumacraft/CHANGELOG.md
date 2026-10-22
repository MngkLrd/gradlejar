# Traumatic 0.4.2 | Blood-loss survival hotfix

Mainline: Minecraft 1.21.1, NeoForge 21.1.262, Java 21.

## The bug
Before this update blood loss damaged local body HP by up to 22 points per 2 seconds, then separately called vanilla player.hurt(), and an empty head or chest caused an immediate forced kill. The additional damage also interrupted medical applications based on a displayed-heart comparison.

## New behavior
- Mild bleeding: 0.35 local HP every 40 ticks (2 seconds).
- Heavy bleeding: 1.20 local HP every 40 ticks (2 seconds).
- Only a direct hit that destroys head or chest remains immediately lethal.
- If head/chest reaches 0 HP because of *ongoing bleeding*, a **30-second critical-surgery window** begins. During this time emergency surgery can restore the zone and cancel the pending collapse. Bandaging alone stops future bleeding but does not revive an already destroyed vital zone.
- Critical countdown is stored in player NBT to prevent reconnect-reset exploits.
- Critical vitality is shown as at most 30% of vanilla maximum hearts even when undamaged limbs are healthy.
- Bleeding now subtracts local HP only, allowing VitalityBridge to update hearts once; no duplicate LivingDamageEvent or player.hurt per bleed tick.
- Bleeding from already-blackened limbs causes slow secondary circulatory damage to the torso, not instant death or harmless infinite bleeding.
- Treatment is interrupted by genuine incoming damage via TraumaEvents; passive blood loss or routine heart synchronization does **not** cancel the ongoing bandage/surgery.
- Unchanged: damage location, fracture generation, medical items, treatments, textures, inventory/HUD, JEI, and direct-hit fatality rules.

## QA scenarios
1. Create heavy head bleed with surviving head HP; use stopwatch to confirm ~1.2 local HP lost every 2s, not 22.
2. Once bleeding blacks the head, confirm the player stays alive long enough to finish an 8-second surgical treatment within the 30-second window.
3. Apply a bandage while bleeding: countdown completes without passive blood loss canceling the action.
4. Black head/chest by direct impact: it should remain immediately fatal.
5. Bleed a 0HP limb: ensure circulatory damage continues slowly but no sudden one-shot.
6. Quit/rejoin during the vital 30-second countdown; remaining timer should persist in player state.
7. Verify medical recovery clears critical status only after both head and chest have positive HP.
8. Verify synced HUD, vanishing food-healing mismatch, JEI, dedicated-server load, and respawn.

Build: `gradle --no-daemon clean build`. Output: `build/libs/traumacraft-0.4.2.jar`.

# Traumatic 0.4.1 - Medical Polish

**Mainline:** Minecraft 1.21.1 / NeoForge / Java 21.

- Vanilla-style beveled and softly rounded 166px medical panel; 78% small text in medical rows; compact HP and effect labels never overlap health bars.
- Hotbar anatomy widget lowered, aligned to the vanilla hotbar bottom, with stacked bleed, fracture, concussion and medication status badges.
- Fractures no longer recolor healthy limbs yellow: the health tint reflects HP alone, while fracture symbols and status labels remain separate.
- Food regeneration now heals at one-third vanilla speed, not one-tenth. Blacked legs or arms can recover gradually with food at half of that local rate; existing fractures/bleeding remain, and fully destroyed head or chest remains lethal.
- Added timed concussion on damaging head impacts and restrained, configurable-free low-alpha edge distortion. Added Analgesic Tablets (2 from sugar, honey bottle and glow berries): short Resistance I, subdued warm sepia for two minutes, no fracture cure.
- New item help tooltips on all medical consumables expose effect, target and time, using the existing JEI description translations.
- Rag dressing now crafts 2 from any one wool block via minecraft:wool tag, applies in 1s (faster than the 1.5s Sterile Patch), heals only 4 local HP.
- Hand-held medicine now stays in the use pose while the right mouse button is held; stopping, sprinting, damage or item switching cancels application. Walking does not cancel bandages or other field medicine; surgery still demands remaining near the starting point.
- State is server-authored; concussion and analgesic duration are serialized and synced to clients. All existing 24 item registry IDs, body HP, hit-location targeting, JEI recipes, and fracture odds preserved.

Build: `gradle --no-daemon clean build`; output `build/libs/traumacraft-0.4.1.jar`.

QA: Hold RMB for a whole bandage; release midway; walk while treating; sprint to cancel; verify one wool -> two dressings; heal blacked leg by hunger without removing fracture; apply painkillers and inspect mild sepia; head-hit concussion; GUI scales 2/3/4; hover tooltip text; check official JEI recipes; dedicated server; relog.
