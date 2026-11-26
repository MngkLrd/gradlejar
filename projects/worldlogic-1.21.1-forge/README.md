# World Logic 1.0.0-rc9
Minecraft 1.21.1 / Forge 52.1.0+ / Java 21 / Gradle 8.10.2 / official mappings.
Build: gradle --no-daemon clean build. Output: build/libs/worldlogic-forge-mc1.21.1-1.0.0-rc9.jar.
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

Hard survival update: persistent work fatigue, recovery through rest and warmth, longer fall injuries, smoke inhalation in small enclosed areas. Light preset disables these additions. Further rules are implemented in the subsequent terrain and campfire pass.

Hard survival terrain pass: finite campfire fuel (coal/charcoal/logs/planks/sticks, relight with flint and steel), persistent crop rotation and fertilization, opt-in roof instability below Y48 with supported logs/fences and two-second warnings. Old player builds cannot be identified retroactively; mine instability is off by default and on in Hardcore. Collapses are local and capped. Fuel ticks only in loaded active campfire chunks. This update is not yet gameplay-tested.

RC8 expedition consequences: damage interrupts food use without consuming it; shield blocks consume existing fatigue and become less efficient at high fatigue; heavy armor increases oxygen use underwater; sheltered full sleep recovers more fatigue than exposed sleep; noisy mining attracts nearby idle hostile mobs through navigation (sneaking avoids the noise). No recipes, item weight HUD or new progression. Light disables the five rules. Shield and sleep require exertion. New core checks cover armor, shield and rest calculations; multiplayer and client gameplay remain untested.

Fatigue recovery now starts after three seconds without strenuous work; active item use prevents ordinary rest recovery. Mining noise targets the miner's last reachable position rather than the still-solid block being broken. /worldlogic inspect reports fatigue.

RC9 consequences: 70 switches. Combat recovery delays food-driven natural healing only, not potion/beacon healing; back hits while sprinting stagger; attack on ladders applies a downward/outward impulse; heavy armor increases exertion; actual fall damage can break 1–3 tagged fragile cargo items (Hardcore only by default); hard falls and environmental damage wear armor; selected mobs investigate their last visible target location after losing their vanilla target. Pursuit does not reveal hidden player positions or break doors. Recovery/stagger and pursuit memory are session-local; no new items or recipes. Runtime and balance must be verified.
