# World Logic: Trauma public API v1

Target: Minecraft 1.21.1 NeoForge; mod ID `worldlogic_trauma`, Java package `dev.minemods.traumacraft.api.TraumaApi`. Registry IDs retain the historical `traumacraft` namespace.

Add the Trauma JAR as compileOnly (runtime optional only if the caller guards presence before loading its integration class). Check `API_VERSION == 1`. Call on the logical server thread, not from a render callback. No direct access to private NBT or internal classes is required.

```java
Optional<TraumaApi.Status> status = TraumaApi.status(serverPlayer);
```

Status contains bloodMl, maximumBloodMl, pain (0..100), shock (0..3), concussionTicks, analgesiaTicks and an immutable list of seven Region records. Each Region exposes zone, health, maximum, function (0..1), bleeding (0..2), fractured, stabilized, and an immutable set of Injury classifications. Zones: HEAD, CHEST, ABDOMEN, LEFT_ARM, RIGHT_ARM, LEFT_LEG, RIGHT_LEG. Injury: CONTUSION, CUT, PUNCTURE, LACERATION, BURN, FROSTBITE. Empty Optional when injuries are disabled, the player is inactive, or the call is not on the server thread.

```java
boolean accepted = TraumaApi.reportInjury(serverPlayer,
    TraumaApi.Zone.LEFT_ARM, TraumaApi.Injury.FROSTBITE, 4f, 0);
```

Reports NEW localized tissue damage; anatomical HP points must be finite, positive and at most the target zone maximum, bleeding 0..2. Does not subtract vanilla hearts itself. Rejects invalid/inactive calls; accepted calls cancel treatment and synchronize the body. Vital destruction is resolved by normal Trauma tick. Never call for damage already handled by Minecraft's damage event; that would count an injury twice. Thermal can use this for additional tissue-specific complications only if the corresponding wound is not already produced by vanilla damage. No core modification required. The API does not expose a public healing or direct NBT mutation method.

This documents the RC12 source contract; combined third-party runtime verification is separate.

### Ownership arbitration (0.5.4)
Trauma is the owner of medical injuries while installed. Core's `ruleEnabled("injuries")` / `ruleEnabled("shoulder_impact")` may be false specifically because Core delegates these legacy mechanics to Trauma. Do not use these switches to gate Trauma API calls. Living server-side survival/adventure players remain eligible independently of Core's legacy injury switches; creative/spectator players remain excluded.

### Thermal ownership (0.5.5)
Thermal >= 0.8.0-rc1 reports local FROSTBITE through the unchanged v1 API. HP does not run its legacy peripheral exposure adapter when this provider is installed. Thermal 0.7.x and older do not have a local injury sender; HP's compatibility adapter consumes existing felt/wet temperature data in that case only. Systemic hypothermia and local frostbite remain distinct.


### Hydration 0.5.0 (API v1 revision 2)

```java
TraumaApi.reportWaterIntake(ServerPlayer player, int volumeMb, double contaminationRisk, double salinityGkg);
double additionalPointsPerSecond = TraumaApi.fluidLossPointsPerSecond(player);
Optional<TraumaApi.IllnessStatus> status = TraumaApi.illnessStatus(player);
```

- Report the **actually consumed** batch exactly once, on logical server thread after Hydration accepts drinking. Volume in mB, risk in [0,1], salinity in g/kg; invalid arguments and inactive players are ignored. Medical dose per call capped at 1000 mB like Hydration's balance model. Clean saline water does not create infection. Repeated contaminated intake can worsen a disease or switch the syndrome to enterocolitis; incubation is not restarted.
- Fluid loss is a pure, repeatable read of the current symptoms in Hydration points/second; zero during incubation, after recovery or for inactive players. No blood, sweat, basal or salinity-loss component. Diarrheal loss is severity-dependent, vomiting adds 0.55 points/sec for five active seconds. Maximum 0.91. Hydration should use this finite result instead of its fallback exposure loss, never add both.
- Trauma reads optional `dev.minemods.hydration.api.HydrationApi.physiology(ServerPlayer)` revision >=2 through a read-only adapter (hydrationPercent 0..100, electrolyteIndex 0..100 with 50 neutral, dehydrationStage NORMAL/MILD/MODERATE/SEVERE/CRITICAL). Missing/empty/incompatible provider uses neutral recovery only. No Hydration NBT reads or writes, no second dehydration simulation.
- Game-calibrated syndromes: dose >=0.35 starts incubation (90–164 active seconds); dose >=1.55 is enterocolitis, otherwise gastroenteritis; symptom severity <0.5 mild, >=0.5 moderate, >=0.8 severe. Natural recovery is gradual; medkit on abdomen (or RMB auto-target) grants 150 active seconds of supportive recovery, one course at a time; rest and healthy hydration/electrolytes improve recovery. No instantaneous cure. Vomiting interrupts ongoing climbing/treatment and applies nausea. State persists in Trauma's separate NBT, resets on death.
- Optional illnessStatus exposes immutable medical state for integrations, including incubation. Client sync intentionally hides asymptomatic incubation; clinical name, stage and treatment shown in abdomen tooltip. Network protocol 10, both sides must update. No required Hydration dependency or cross-loader changes.
