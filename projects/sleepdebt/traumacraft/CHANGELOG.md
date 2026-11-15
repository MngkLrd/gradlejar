# Traumatic 0.4.4 | Combat / HUD / Medical Pouch

Minecraft 1.21.1, NeoForge 21.1.262, Java 21. Display name Traumatic; modid remains `traumacraft`.

## Updated combat
- `LivingDamageEvent.Pre` already sees armor-reduced vanilla damage. Normal body damage conversion now has a 3.0 multiplier (previously 8.0), projectiles 4.0. Fall and explosion algorithms stay unchanged.
- Additional small target-area protection when the matching armor slot is worn and modest protection based on the player's existing armor points.
- The objective is to prevent a standard zombie killing a fully netherite-armored player in ~5 normal hits. Verify in Survival (on Easy/Normal/Hard).

## HUD
- Removed 68x76 black background box and text badges (BLD2/FX/CON/MED).
- Anatomical silhouette aligned to hotbar bottom, with 9x9 transparent original pixel-art icons to the right for bleeding, fracture, concussion and analgesics. No repeated status labels.

## Medical pouch
- Added `traumacraft:medical_pouch`, craftable with leather, wool and string; open by right-click while held in the main hand.
- Four separately synchronized slots (2 columns x 2 rows), accepts medical consumables only, rejects bags nesting inside each other.
- Contents stored on the bag's own `minecraft:container` data component, not globally or on the client; survive saves, deaths and item transfers.
- Added `traumacraft:oversized_medkit`, uses the existing Trauma Kit treatment profile, unstackable, craftable, available from hotbar and regular inventory treatment cursor.
- **Experimental 1x2:** in the medical pouch, oversized medkit can be inserted only into an upper cell. It reserves the cell directly below it. The lower cell cannot be filled or taken while that medkit remains. One real ItemStack occupies the two visual grid positions; no duplicate/phantom second item exists.
- **Limitation:** Vanilla player inventory and chests still show the oversized medkit as one vanilla ItemStack (one slot). Fully integrated two-slot items in arbitrary vanilla/modded containers are not yet supported. This is deliberately constrained to avoid item loss and duplication.
- Shift-click transfers in the pouch are disabled while testing two-cell semantics; normal click interactions supported.

## QA
Check HUD screenshots; zombie hit survival in full netherite and without armor; save/reload pouch; pick up pouch and die; place oversized medkit in both upper positions; ensure lower cell is blocked; remove medkit and reuse lower cell; attempt to store nonmedical items or another pouch; use large kit from hotbar and anatomy cursor; use creative mode; inspect JEI recipes; dedicated server, cross-mod inventories and Shift-click. 

**Unverified until in-game QA.** Gradle compile only checks APIs/resources.
