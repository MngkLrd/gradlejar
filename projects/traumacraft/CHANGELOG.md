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
