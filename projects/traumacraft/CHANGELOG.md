# Changelog

## 0.3.0 - complete medical expansion
- Added 15 functional medical items: Sterile Patch, Compression Wrap, Hemostatic Gel, Tourniquet, Fracture Brace, Bone Mender, Compact Medkit, Field Medkit, Advanced Trauma Kit, Surgical Kit, Recovery Serum, Adrenaline Injector, Guardian Injector, Focus Injector, Endurance Injector.
- Added 5 craftable materials: Sterile Fabric, Herbal Extract, Antiseptic Solution, Empty Injector, Medical Polymer.
- Added 20 survival recipes, 20 original pixel-art textures and item models.
- Added item information pages through the optional JEI plugin for all 23 mod items.
- All 15 new treatments work both via right-click and exact anatomical targeting inside the pre-existing inventory GUI.
- The previous 3 medical items retain the same effects.
- The combat/trauma HP system, injury calculations, item hit locations, world interaction, character UI and HUD remain unchanged.
- Java 21 / NeoForge 21.1.262 / Minecraft 1.21.1 build passed in pull request #15.
- Client gameplay testing is still required.

## 0.2.0 - anatomical UI
- Shift standard survival inventory to accommodate a 128-pixel integrated body status panel.
- Display seven separate local HP pools with green / amber / red / destroyed visualization.
- Mark bleeding (red) and fractures (yellow) on every body region.
- Synchronize server-authoritative health values to the client, on login/respawn and when damaged.
- Target medical treatment by picking up a medical item in inventory and left-clicking the affected zone.
- Add a semi-transparent bottom-left survival HUD.
- Fix client event subscriber registration for NeoForge 1.21.1.
- Fix compact-HUD positioning to avoid covering vanilla hearts.

Note: A prototype. Core vanilla survival health and medical consumables remain as implemented in 0.1.0. Manual gameplay testing required.

## 0.1.0 - injury simulation foundation
- Seven persisted body zones with local HP, fractures, bleeding and three medical items.
- Approximate hit selection and /trauma debug commands.

### 0.2.0 integration details
- JEI: optional ingredient information pages for Bandage, Splint and Medkit; crafting recipes remain discoverable normally.
- Separate TraumaCraft | Medicine creative inventory category for all treatment items.
- Original 16x16 medical icons and per-limb cursor treatment through the inventory panel.
