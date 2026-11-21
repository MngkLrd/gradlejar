# Last Witness 0.5.0 – in-game acceptance tests

Runtime target: Minecraft 1.21.1, NeoForge 21.1.x, Java 21. Server first, then client.

1. Spawn 3 villagers facing different directions. Attack a villager while hidden behind a solid wall. Confirm no eye and no alarm. Step into sight, repeat: ONLY resident who actually faced both culprit and scene can report.
2. Test a broken bell: direct sight of bell required, not just distance.
3. Let a witness report, observe the eye change gold -> red; after 3s, original custom name and prior glow restored. Intimidation still works.
4. Visit vanilla/CTOV village in daylight, observe voluntary social movements and meetings without interrupting work, trading, night sleep, or panic. Leave/re-enter chunk.
5. Spawn guard and provoke both zombie and wanted pursuit. Patrol around home. Archer moves sideways if firing lane is blocked, keeps distance, only draws when preparing to shoot. Check no damage to nearby villagers.
6. Check farmer, fisher, fletcher, smiths, librarian and other professions trade level progression, 1-5. Prices and output counts must not be free.
7. Existing villager: `/lastwitness refreshtrades` as operator refreshes offers within 24 blocks. It deliberately discards old offers; warn before using in production.
8. Die while wanted: wanted status and guard pursuit must stop. Save, restart, revisit the same village.

Known unverified areas: runtime pathfinding under heavy CTOV structures, interaction with mods that replace the vanilla villager Brain/trade event or renderer. CI Java compilation alone is not in-game acceptance.