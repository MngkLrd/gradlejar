# TraumaCraft 0.3.0 - medical expansion

Minecraft 1.21.1, NeoForge, Java 21.

## Additions

15 new functional medical consumables and 5 custom crafting components. The three original treatments remain present and unchanged. All items are craftable from survival-available resources, have dedicated 16x16 pixel sprites, English and Russian names, and JEI informational recipe pages.

Medical components: Sterile Fabric, Herbal Extract, Antiseptic Solution, Empty Injector, Medical Polymer.

Medical consumables: Sterile Patch, Compression Wrap, Hemostatic Gel, Tourniquet, Fracture Brace, Bone Mender, Compact Medkit, Field Medkit, Advanced Trauma Kit, Surgical Kit, Recovery Serum, Adrenaline Injector, Guardian Injector, Focus Injector, Endurance Injector.

Full medical tab: 5 components + 18 treatments = 23 items.

## Controls

- Right-click a medical consumable from the hotbar to auto-select a valid injury (or apply a buff).
- Or open the usual inventory and hold a treatment on the cursor, then click the intended body part in the right-side anatomy panel.
- Every operation is checked and committed on the logical server. Invalid treatments consume nothing.
- For JEI, search `@traumacraft` to show mod items, press R to inspect the crafting recipe, U to inspect uses, and view the JEI **Item Information** page for intended effect and duration.
- A separate **TraumaCraft | Medicine** creative inventory tab groups ingredients and all treatments.

## Explicitly untouched

Damage events; hit-location calculations; fracture/bleeding application; local health storage; death calculations; anatomy GUI and the lower-left HUD were not altered. The medical action service, the treatment network call, JEI catalog, item registry, creative tab, languages, recipes and assets are the only modified parts.

## Build

```bash
gradle --no-daemon clean build
```

Expected: `build/libs/traumacraft-0.3.0.jar`.

## In-game QA

1. Check old bandage/splint/medkit still work and now have JEI information pages.
2. Check all 20 new recipes in a real crafting table and in JEI.
3. Check all 23 items appear in the medical creative tab and JEI search.
4. Verify item cursor treatment of a selected body part and right-click automatic targeting.
5. Verify buff injectors apply the stated vanilla effects and consume one syringe.
6. Verify surgical kit works at exactly 0 local HP and basic patches don't heal blacked parts.
7. Verify no changes to damage, bleeding, fractures, HP distribution or UI.
8. Verify dedicated server startup without JEI.
