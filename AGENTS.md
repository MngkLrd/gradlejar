# MineMods development contract (QA v2)

Applies to every agent and contributor using MngkLrd/gradlejar. The repository is shared: inspect the current main head and never force-push, overwrite unrelated changes, or publish untested binaries as verified.

## Mandatory for new projects
1. Define Minecraft, loader, Java, Gradle, mappings and real dependencies. Use `tools/qa/compatibility.json` as the reference matrix; it contains tested/pinned baseline choices, not a universal promise of compatibility.
2. Add `build-profile.json` with `qa.mode`, exact `qa.loader_version`, complete `qa.release_targets`, standard JAR name, and (for gameplay mods) a positive regression scenario under `tools/qa/scenarios/`. Client-only mods use mode client, not a dummy server.
3. Compile the real Gradle project. Validate the actual final JAR, metadata, class target, JSON and data-pack layout.
4. For gameplay mods, boot the official installed loader with the exact release JAR, run assertions against the world and retain logs. No development-server fallback may be called production-tested.
5. Convert each confirmed bug into a repeatable scenario or GameTest. Include positive and negative controls. Test each supported Minecraft/loader pair separately.
6. Never bypass a failed QA gate, replace gameplay assertions with startup markers, fake outputs, or mark a client screenshot as a manual gameplay test.
7. Ship only eligible release JARs. Complete multi-port bundles require every declared target, exact checksums and no duplicate variants; ZIP contains JARs only. Report client status separately.
8. Existing legacy profiles are grandfathered temporarily to avoid interrupting ongoing development; label them legacy/static only and migrate before claiming runtime QA. Never add new exceptions without explicit owner approval.
9. Prefer shared tools over more hardcoded per-mod workflow jobs. Separate client and server code. Do not modify unrelated projects during a port.
10. Report actual CI run, first failure, test scope and limitations. A successful compile is not a successful gameplay test.

See `docs/QA-V2.md`. The experimental client launcher is development-mode only; QA-5 requires manual client validation. Independent legacy workflows remain outside the v2 gate until migrated; do not use them to bypass the standard.
