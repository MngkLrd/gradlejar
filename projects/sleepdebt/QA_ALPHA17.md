# Alpha 17: diagnosing invisible darkness

1. Install only alpha17 Sleep Debt JAR, NeoForge 1.21.1 Java 21.
2. Enter ANY world and press F8, even in daytime with no mob.
   The entire scene must become almost black. Press F8 to restore.
3. Press F9 and inspect live PRE/POST render event counters.
4. /time set midnight, /sleepdebt debug spawn_sleepless.
   Diagnostics must show real>=1 and nearest distance.
5. At 5 blocks target/pressure should approach 1.0 and alpha approach 248.
6. F8 works but real encounter does not -> examine real, config, target.
7. F8 fails -> inspect render event and HUD state, F1, other GUI mods.
8. /sleepdebt debug vanish_sleepless -> blackness persists briefly.
9. F8/F9 can be rebound in Controls. No custom GLSL required.
10. Test old worlds, client resource reload, multiscreen & dedicated server.
