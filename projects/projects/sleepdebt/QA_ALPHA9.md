# Alpha 9: The Sleepless test plan
### Natural, increasing horror
1. /gamemode survival; /time set midnight; /sleepdebt debug preset critical
2. /sleepdebt debug why_no_sleepless
3. Stand in a dark forest without dreamcatchers. Within 30-100 seconds check distant shadow/smoke or a REAL hostile mob.
4. Turn behind you, see a distant Sleepless apparition. Stare for ~0.55 seconds; it fades. Approaching within five blocks makes it vanish. It has no name/health/hitbox.
5. The real Sleepless has a hitbox. It approaches and may flank behind you; isolated footsteps, smoke and a growing dark vignette appear as it closes.
6. The first successful blow should trigger 200+ ticks of DARKNESS; the second 30 ticks BLINDNESS plus WEAKNESS. More blows renew darkness. Daylight still removes it.
7. Die to the real mob and respawn. If persistAfterDeath is true, sleep debt persists but the Sleepless stops targeting that player for 3600 ticks.
8. Test /sleepdebt debug spawn_sleepless to create the real entity immediately; the command bypasses natural spawn checks.
9. Test /sleepdebt debug why_no_sleepless: report threshold, nighttime, nearby dreamcatcher, respawn grace, enabled status and spawn interval.
10. Set server config spawnIntervalTicks=400 and respawnGraceTicks=0; confirm behavior changes.
11. Change client setting sleeplessApparitions=false or sleeplessNearVignette=false; the actual mob should still work.
### Vanilla visual style
12. Check every 16x16 icon at native inventory scale, 3D blocks with vanilla lighting, eight coffee crop stages, translucent lantern and 138x58 spruce wood HUD.
13. Check GUI scale 2/3/4, EN/RU native fonts, alt resource packs, Iris/Oculus if available.
14. Verify no duplicate entity after world switches, no apparition collision or server entities, dedicated server startup.
15. Test compiled JAR with NeoForge 1.21.1 and Java 21. CI does not replace runtime QA.

