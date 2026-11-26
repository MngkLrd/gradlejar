# MineMods QA v2: contributor guide

## P0 — compatibility and release policy
- `tools/qa/compatibility.json` centralizes the eleven standard targets, runtime Java, pinned baseline loaders, Gradle and separate resource/data pack formats. For newer releases verify Mojang/loader metadata and update the registry intentionally. It is a reference, not an automatic dependency updater. Each project's real Gradle build still controls compile dependencies.
- `AGENTS.md` is the standard for all contributors. A new profile in the common Build Hub must define QA; missing QA is a failure. The explicit legacy snapshot prevents suddenly blocking existing work. Existing independent workflows are not yet migrated; their releases are not QA-v2 certified.
- Official server install: Fabric Meta launcher, Forge or NeoForge official Maven installer. No dev-run fallback. Copy exact release JAR, validate its SHA-256, run isolated localhost server with unique RCON password and ports, check real game-state assertions, require clean shutdown.
- A failed gate uploads a clearly labeled NOT-RELEASE candidate (if static compilation produced one), not a normal release artifact.

## Example build-profile.json
```json
{
  "slug": "examplemod",
  "version": "1.0.0",
  "minecraft": "26.3",
  "loader": "fabric",
  "java": "25",
  "gradle": "9.7.1",
  "jar_glob": "examplemod-fabric-mc26.3-1.0.0.jar",
  "qa": {
    "mode": "server",
    "loader_version": "0.19.5",
    "scenario": "tools/qa/scenarios/examplemod.json",
    "release_targets": ["fabric-mc26.3", "neoforge-mc26.3"],
    "watch_paths": ["tools/qa/scenarios/examplemod.json"]
  }
}
```
List identical complete release_targets in each port. Extra required dependencies: `qa.dependencies` array of {name, url, sha256}; use exact official HTTPS artifacts. Client-only projects use `mode: client` without a server scenario; static candidates intentionally cannot publish until a separate client approval process is configured.

## P1 — real assertions and packages
- Scenario schema: see `tools/qa/scenarios/fragileglass.json`. Steps contain command plus expected/rejected regex, or bounded wait. Mark positive gameplay assertions with assertion=true. Optional loaders filter. Setup commands must also have expectations. Include both positive and negative controls. The runner rejects empty/vacuous suites and executes on a fresh flat world.
- Advanced features may need a GameTest or dedicated harness; extend the runner rather than use dummy success markers. Current generic scenario runner supports RCON commands; there is no simulated player yet, so it cannot validate actual left-click packets.
- `tools/plan_builds.py` uses the complete push range; shared scenario edits select all referencing profiles. Dispatch common Build Hub with project path or `@slug` for every enrolled port of that slug. Unrelated infrastructure changes no longer default to FieldKit.
- `tools/qa/bundle.py` only bundles a cohort from one run/commit when all expected targets passed QA-4, filenames and hashes match the installed JARs, no duplicate targets, and JAR ZIPs are intact. Package order Forge → NeoForge → Fabric. ZIP contains only production JARs. Separate bundle-report.json records incomplete cohorts. Partial builds may produce individual passed JARs but not a full bundle.
- Each common CI job creates a machine-readable QA report, and failed tests upload logs and classified diagnostics. Classifiers are heuristics, not automatic code fixes.

## P2 — evidence and client experimentation
- The **QA v2 infrastructure and dashboard** workflow runs unit tests on infrastructure changes, supports manual dispatch, and refreshes daily. Download MineMods-QA-dashboard and open index.html offline. It inventories all available profiles and reads a bounded recent Actions artifact history. Unknown/stale/legacy are distinct states; expired or unavailable artifacts are never treated as passes. A new source tree invalidates old evidence. No public Pages deployment or repository settings change.
- **QA v2 experimental client** starts Gradle runClient under Xvfb/Mesa, checks for a visible stable window and captures screenshot/logs. It is development-mode only, not a final-JAR test and not visual gameplay QA. It can fail because of runner software graphics, missing tasks, resource downloads, or client problems. Inspect screenshot manually.
- QA levels: 0 no successful evidence; 1 compiled; 2 static; 3 production server boot; 4 production server assertions plus clean shutdown; 5 manually verified client (not assigned by automation). Legacy QA-2 is the old static validator only; it does not imply the stricter v2 checks.
- Run unit tests locally: `python3 -m unittest discover -s tools/qa/tests -v`.

## Migration
The first cohort is Fragile Glass 26.1.2/26.2/26.3 on NeoForge/Fabric. Existing profiles are not mass-edited. Migrate one mod at a time: adopt exact names, QA profile, pinned dependencies, scenario, then remove dedicated packaging and legacy exception only after successful v2 runs. New profiles must not add themselves to the legacy list. Separate historical workflows require explicit migration; this is not a branch-protection policy for the whole repository.
