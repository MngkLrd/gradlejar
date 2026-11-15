# Sleep Debt Alpha 12 — art, fog and audio runtime tests

1. Start NeoForge 1.21.1 with Java 21, only sleepdebt. All 5 OGG sounds must be inside built JAR, valid OggS magic, model atlas 256x128.
2. /gamemode survival, /time set midnight, /sleepdebt debug preset critical, /sleepdebt debug spawn_sleepless.
3. Observe at 25, 15, 10, 6 and 3 blocks. Fog transitions smoothly from normal rendering to a near-black 12-block visibility boundary, while the hunter remains visible.
4. Confirm no standard Darkness / Blindness status effect caused by the hunter. Light sources nearby should remain legible at close range.
5. Check custom breathing, whispered phrases/noises, growl on hurt, attack sound, and heartbeat as danger intensifies. Sound slider: Hostile/Ambient volumes must work normally.
6. Test client sleeplessFogStrength=0,1.0,1.5 and sleeplessNearVignette=false and sleeplessHorrorAudio=false separately.
7. Turn on daylight during encounter: hunter disappears and fog/audio fades back. Spectator/Creative/bed entry/respawn should not trap a player in persistent black fog.
8. View monster in motion: skull, hanging jaw, ribs, long crooked limbs and fingers all render. No missing purple textures, mirrored UV holes or invisible body.
9. Verify taller collision still allows movement on plains; hunter will not naturally spawn in a 2-block-high cave.
10. Test Sodium/Embeddium/Iris if installed: shader mods might override fog; client-side compatibility is not asserted by CI.
11. Confirm multiplayer and dedicated server startup, old saves and Registry NBT stability. Check possible OGG download failure on a clean CI runner.
