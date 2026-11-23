# Primitive Waterworks 0.3.3 — Grouping Fix

Minecraft 1.20.1 / Forge 47.3.0+ / Java 17 / TerraFirmaCraft 3.x

This release ONLY changes the item-viewer integration. All water flow, 3D models,
knife-carved outlets, species colors, barrel interfaces, and recipes are preserved.

### Correct TFG integration
TerraFirmaGreg Modern uses EMI and Reliable EMI (REMI) stack groups, not just REI.
The groups are defined under `assets/primitivewaterworks/stack_groups/`,
with the exact `remi:tag` structure used by the pack's own grouping system.

Three groups:
- Rain Collectors (primitivewaterworks:rain_collectors)
- Wooden Gutters (primitivewaterworks:wooden_gutters)
- Wooden Downspouts (primitivewaterworks:wooden_downspouts)

Their members are item tags already present in 0.3.2. Each includes TFC
species and optional ArborFirmaCraft species. All labels are localized.

### Testing
1. Install only this JAR, deleting 0.3.2.
2. Ensure EMI + REMI are enabled. TerraFirmaGreg stock config has enableStackGroups=true.
3. Enter a world, reload resources with F3+T (or restart Minecraft).
4. In EMI item index, group entries should collapse to three rotating category icons.
5. Hover or open a group to verify member count and species.
6. Check that searching `@primitivewaterworks` may filter and unpack groups based on search settings.

If your TFG instance uses a version of REMI which reads groups from
`config/remi/stack_groups` only instead of resource packs, a separate
compatibility setup may be needed. The stock TFG scripts use resource-pack
assets paths, matching this release.
