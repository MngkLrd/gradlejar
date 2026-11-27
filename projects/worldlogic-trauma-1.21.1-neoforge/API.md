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
