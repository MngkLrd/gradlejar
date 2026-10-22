# Sleep Debt 2.2 alpha.2
Minecraft 1.21.1 NeoForge, Java 21. Source in Minecraft Mod Build Hub.
## Features
9 items (7 new concepts plus classic coffee and energy). Native 10-pip HUD. Server-saved fatigue and stimulant tolerance. Sleep quality based on shelter and hostile presence. Vanilla recipes exposed to JEI and EMI, and a fully crafted Patchouli field guide with six entries.
**Required for client:** JEI 19.21+ and EMI 1.1.22+. **Required on both:** Patchouli. Comforts is compatible through vanilla sleeping state and is not a required dependency.
Art textures: generated from strictly 16x16 hand-authored pixel maps with no antialiasing.
## Build
gradle clean build
build/libs/sleepdebt-1.21.1-2.2.0-alpha.2.jar
## Limitations
Compilation is CI-only; test client/server, sleeping mods, JEI+EMI simultaneously. Pillow and mask are normal items and have no bonuses yet. No old-version save migration tested.

## Jade integration
Jade is required on client and server. Hover a vanilla bed to see the sleep-quality hint (Jade extension compiled against 15.10.6 NeoForge API).
