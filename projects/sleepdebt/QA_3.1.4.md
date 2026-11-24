# Sleep Debt 3.1.4
Minecraft 1.21.1 / NeoForge / Java 21.
- All 25 item sprites, 15 material maps, eight crop stages and three effect icons rebuilt in native pixels.
- Copper, iron, timber, canvas, cloth, glass and ceramics have dedicated pixel clusters; lantern cap and coffee display sample the relevant UV region.
- HUD Sleep is perceived pressure; journal and persistent state retain actual debt. Caffeine masks pressure, tolerance limits relief, crash temporarily raises pressure. Status icons show alertness, calm and crash; immediate server sync on consumption.
- Existing NBT key and IDs remain; no new network fields.
- Fix verifier's ZIP handle shadowed by local z dimension; retain all geometry checks.
In-game QA: set needs high via existing commands; drink each stimulant, verify lower Sleep and effect icons, wait for expiry and crash, sleep and check reset. Check HUD, journal and all item/block faces at multiple angles. Verify in survival and creative. CI verifies technical assets, not visual quality or gameplay.
