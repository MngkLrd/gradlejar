# World Logic 1.0.0-rc6
Minecraft 1.21.1 / Forge 52.1.0+ / Java 21 / Gradle 8.10.2 / official mappings.
Build: gradle --no-daemon clean build. Output: build/libs/worldlogic-forge-mc1.21.1-1.0.0-rc6.jar.
Install on both server and clients. No external dependencies. Burning Arrows excluded.

Config: config/worldlogic.properties. Mods > World Logic > Config (local world or main menu).
Remote multiplayer server settings are administered with permission level 2:
- /worldlogic preset light|realistic|hardcore
- /worldlogic rule <key> <true|false>
- /worldlogic strength <impact|movement|air|weather|fire|ecology> <0.25..2>
- /worldlogic reload, inspect, selftest
Realistic defaults preserve existing redstone and opt out of destructive mechanics. Hardcore enables all rules; use fresh worlds only. Strength changes selected shared calculations, not every individual threshold.
CI core assertions cover profiles, impulse reflection, item mass, torch light, furnace mixin, material tags and SavedData creation. Dedicated server smoke test is separate from full gameplay QA.
Known limitations: existing logs placed before the mod was installed cannot be identified reliably; tree decay is off by default. Air and ventilation use bounded searches, no fluid simulation. Smoke follows sampled air paths; snow shedding is discrete. Furnace batches use last cooking time; not individual food units. Client visuals, mixed-mod interactions, network movement, claim protection and performance require in-game validation.

RC6: client receives authoritative server friction and mining streak settings, with no writes to the local config. Expired hot-food components are removed from player inventory. Dedicated-server smoke gate is mandatory for CI prerelease; if installer downloads are denied, the dev server is tested with explicit mixins. Production testing must still be confirmed separately before stable release.
