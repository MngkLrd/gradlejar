# Alpha 15 — Black-veil GPU and anatomy validation

1. Minecraft 1.21.1 NeoForge, Java 21. Test normal rendering without shader packs.
2. In Survival: /time set midnight; /sleepdebt debug preset critical;
   /sleepdebt debug spawn_sleepless.
3. At 35 blocks the screen is clear. At 20 it darkens in moving waves.
   At 5 blocks almost the entire world is black, torches included.
4. Check HUD remains visible. The effect must NOT issue vanilla Darkness.
5. Test attacks, darkness impact shock, sunrise, bed, death and respawn.
6. Test custom shader resource reload (F3+T); no crash or magenta effect.
7. Texture atlas assets/sleepdebt/textures/entity/sleepless.png must be
   2048x1024 with correct face/torso UV. Other artwork and OGG unchanged.
8. Try Shaderpack/Iris compatibility, window resize, GUI scale 2-4.
9. F1 hides this shader with GUI layer; acknowledged limitation.
10. If GPU shader fails during runtime, fallback provides dark opacity.
