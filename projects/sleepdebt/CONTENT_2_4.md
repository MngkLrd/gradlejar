# Sleep Debt 2.4: Field Survival Expansion
## Core mechanics
- Persistent Sleep Debt, Fatigue and Stress. Work raises fatigue; danger raises stress.
- Fully functioning four-slot copper coffee maker and server-synced recipe timer.
- Coffee or herbal tea from vanilla water + grounds/herbs + coal.
- Expanded consumables: herbal tea, dream tea, caramel coffee, bitter tonic, cocoa biscuits.
- New camp blocks: Rest Lantern and Dreamcatcher with special resting rules.
- The Sleepless, its egg and rare Dream Shard loot.
- Vanilla-pixel texture pass: hand-authored sprites, procedural cluster block textures, detailed native GUI.
- Patchouli guide with original entries and Russian translations.
- Three vanilla-style advancements for play progression.
## Machine interface
Slot 0 water bucket, slot 1 grounds or dried herbs, slot 2 coal or charcoal, slot 3 output.
Each batch consumes one bucket of water, one ingredient and one fuel item; an empty bucket remains.
The output slot cannot accept player items, and is checked before processing to prevent duplication.
## Compatibility and integrity
Keep mod id sleepdebt, NBT key sleepdebt_v2 and all old item ids. Never require a client-only class on a dedicated server.
Legacy worlds: new items and blocks use new unique ids; old player status can still be read.
## Manual QA
1. Java 21 Gradle build and JAR check
2. Dedicated server + client with JEI EMI Jade Patchouli
3. Shift-click valid/invalid inputs in machine; test output full; save/reload; break and pick up
4. Inspect pixel art and machine custom screen at GUI scale 2,3,4
5. Bedroll bonuses and cooldown; test no coffee effect on debt
6. Verify /sleepdebt status and /sleepdebt debug set 950 800 700
7. Night summons, daylight retreat, Dreams Shard drop, spawn egg, achievements
8. Check non-hostile hallucinations in client and client/server sync after death
9. Verify Patchouli English/Russian pages and recipes; ensure required libs.


## Showcase command (operators only)
Run /sleepdebt showcase on level terrain with empty blocks around the player.
The command places a bedroll, awning, Rest Lantern, Dreamcatcher and prefilled Coffee Maker.
It never replaces existing blocks. Use /sleepdebt debug set 950 800 700 to test the HUD and night terrors.

