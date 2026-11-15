# Quick Stack Nearby 1.0.0 - NeoForge port

* Minecraft: 1.21.1
* Loader: NeoForge 21.1.1+ (built using 21.1.197)
* Java: 21 / Gradle 9.2.1
* Key: V (changeable in Controls)
* Config: `quickstacknearby-server.toml`, radius 10 default, 1..16.

Server-side logic performs at most one bounded chest scan per player per 20 ticks.
Loaded chunks only; 1024 block entities / 32 chests maximum per action.
Scans double chests exactly once and avoids inaccessible ones.
Only main inventory slots 9..35 are moved. Hotbar, armor and offhand remain untouched.
Only existing incomplete stacks of the exact same item/components are topped up; never insert into empty slots.
Only vanilla chests/trapped chests are supported in this release.

Build via `gradle clean build`; jar at `build/libs/quickstacknearby-neoforge-1.21.1-1.0.0.jar`.
Requires testing in actual Minecraft singleplayer and dedicated server.
