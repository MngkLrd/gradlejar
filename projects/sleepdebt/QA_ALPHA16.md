# Alpha 16 — zero-darkness regression test

1. Minecraft 1.21.1 NeoForge with only sleepdebt in mods. Use Java 21.
2. Remove Alpha 15 JAR. Do not need to reset any old client TOML settings.
3. /gamemode survival ; /time set midnight ;
   /sleepdebt debug preset critical ;
   /sleepdebt debug spawn_sleepless
4. Stand 8, 5, and 3 blocks away. Blackness MUST visibly cover even torches.
5. Turn off sleeplessVoidGpu in client TOML, reload: blackout still works
   because alpha compositor is ALWAYS drawn.
6. Toggle sleeplessVoidEnabled=false: darkness off, audio should continue.
7. Set sleeplessVoidDiagnostics=true; look for real hunter distance and
   actual calculated pressure. If blackness is absent, capture this readout.
8. /sleepdebt debug vanish_sleepless -> the hunter retreats briefly,
   disappears in smoke, relocates ahead or behind, then becomes visible/rushes.
9. Natural hunt: teleport roughly every 95-145 ticks in aggressive modes
   subject to terrain safety. Check caves, hills, forests and server lag.
10. Day, sleep, death, dimension switch and repeated F3+T shader reload.
11. F1 hides GUI overlay in current build; acknowledged, not yet fixed.
12. New texture file is identical to Alpha 15; 8 audio OGG files unchanged.
13. Repeat on dedicated server and with external shaderpack disabled/enabled.
