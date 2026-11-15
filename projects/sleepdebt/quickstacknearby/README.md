# Quick Stack Nearby

Minecraft 1.21.1 / Forge 52.1.0+ / Java 21.

Press **V** to top up existing stacks in nearby vanilla chests and trapped chests.
Player main inventory slots 9..35 only (hotbar/armor/offhand untouched).
Double chests are handled once, blocked chests are skipped.
Server authoritative; radius defaults to 10, configurable (1..16) in the server config.
Protection: 20-tick cooldown per player; no chunk loads; scan cap 1024 block entities and 32 chests.
No support for modded storage, barrels or shulker boxes in 1.0.0.

Run `gradle clean build` with Java 21 / Gradle 8.8. Test actual gameplay on integrated and dedicated servers.
