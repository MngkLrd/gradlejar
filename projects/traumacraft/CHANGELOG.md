# Traumatic 0.4.0 | First aid and vitality

Mainline: Minecraft 1.21.1, NeoForge 21.1.x, Java 21.

- Treatment is no longer instant. All medical items now start an authoritative server-side timer: 1 second for injectors, 1.5 seconds for improvised dressings, 2 seconds for bandages, 3 seconds for splints, 4 seconds for medkits, and 8 seconds for surgery.
- A low-noise progress ring with a seconds countdown is shown at the crosshair. The timer also appears when treatment is selected through the anatomy inventory.
- Damage, sprinting, large movement, item switching, or loss of inventory focus cancels an unfinished treatment without consuming the item.
- Vanilla hearts reflect the aggregate of all seven local health pools; legacy players are migrated on first load, so an old half-health player is not abruptly restored to full by synchronization.
- Passive hunger regeneration is multiplied by 0.10 and routed into the most damaged living body zone. Potion regeneration remains stronger.
- Bleeding damages localized HP as well as the vanilla hearts so the two displays do not drift apart.
- Added cheap Rag Dressing: 2x from paper + string, restores 4 HP to a wounded but non-destroyed body part. Registered in creative medical tab and JEI info/recipe listing. Custom 16x16 pixel texture.
- Existing hit-box algorithms, fracture and bleed chances, and the original medical item IDs remain unchanged.
- Net-new medical items: 1, total catalog: 24.
- Does not change any 1.20.1 or 26.3 port; test this mainline before porting.

Build: `gradle --no-daemon clean build`. Output: `build/libs/traumacraft-0.4.0.jar`.

QA: (1) damage to each zone and compare hearts (2) verify no full-HP body with low hearts after several ticks (3) hunger regeneration is slow (4) activate RMB medical tool and see circle timer (5) open body inventory, cursor-click heal zone, see progress (6) interrupt by damage/sprint/movement/switch item, no consumption (7) finish surgery and verify zone (8) relog and repeat including existing 0.3.4 save (9) verify all recipes and JEI (10) dedicated server loading.
