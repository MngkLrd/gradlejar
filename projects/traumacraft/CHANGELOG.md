# Changelog

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
