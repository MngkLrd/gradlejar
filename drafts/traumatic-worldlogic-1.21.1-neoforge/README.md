# World Logic: Trauma 0.5.0-rc2 — NeoForge development draft

Active target: Minecraft 1.21.1, NeoForge 21.1.262 compile/QA baseline, Java 21, Gradle 9.2.1, Mojang mappings. Runtime loader range preserves the donor project's 21.1.0 minimum as an unverified claim pending lower-version validation. Previous Forge development remains archived on branch traumatic-worldlogic-rc16.

Core is mandatory. As of inspected main d2a992aa5e519ec6eccd178fb80d6df91ab94c90, no NeoForge core artifact or project was verified. Do not use the former RC16 Forge JAR. Build is deliberately blocked until the actual NeoForge core JAR, exact mod version, SHA-256 and bridge signatures are verified. Put the JAR outside the repository or in ignored libs/; then:

`gradle --no-daemon clean build -PworldlogicJar=/path/to/worldlogic-neoforge.jar -PworldlogicVersion=ACTUAL_VERSION -PworldlogicSha256=ACTUAL_SHA256`

Expected candidate: build/libs/traumacraft-neoforge-mc1.21.1-0.5.0-rc2.jar. No release without actual dependency lock in QA profile, successful compile, server assertions, required coverage and client evidence.

Ported source: NeoForge event bus, LivingDamageEvent.Pre, EntityTickEvent.Post, DeferredHolder registries, payload handlers with per-zone stabilization flags, client GUI layers (no vanilla Gui mixin). Existing anatomical atlas, overlays, icons, optional closed-by-default Trauma inventory tab, medicines, recipes, JEI descriptions and HUD selection logic preserved. World Logic bridge retains provisional RC16-style hooks and must be checked against the new core before enrollment. No new art in this port. No changes to the old published standalone mod or shared infrastructure.

QA: component scenario tools/qa/scenarios/traumatic-worldlogic-neoforge.json and coverage inventory tools/qa/assignments/traumatic-worldlogic-neoforge.json. Includes initial healthy body, independent limbs and treatment stabilization/recovery. All Minecraft runtime and client checks remain NOT_RUN; no binary has been built or published.
