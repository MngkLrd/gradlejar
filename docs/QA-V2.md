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
List identical complete release_targets in each port. Extra required dependencies: `qa.dependencies` array of {name, url, sha256}; use exact official HTTPS artifacts. Client-only projects use `mode: client` without a server scenario; static candidates cannot publish automatically. After personally testing every declared target, dispatch **QA v2 manual client release** with the exact completed main Build Hub run/attempt, slug, JSON object mapping every candidate filename to its SHA-256, review notes, and explicit confirmation. The workflow independently rechecks provenance, source tree, static validation, full matrix and all SHA-256 values before publishing a flat JAR-only archive. It records the GitHub reviewer and approval run; QA-5 is human-attested, not an automated claim.

## P1 — real assertions and packages
- Scenario schema: see `tools/qa/scenarios/fragileglass.json`. Steps contain command plus expected/rejected regex, or bounded wait. Mark positive gameplay assertions with assertion=true. Optional loaders filter. Setup commands must also have expectations. Include both positive and negative controls. The runner rejects empty/vacuous suites and executes on a fresh flat world.
- Advanced features may need a GameTest or dedicated harness; extend the runner rather than use dummy success markers. Current generic scenario runner supports RCON commands; there is no simulated player yet, so it cannot validate actual left-click packets.
- `tools/plan_builds.py` uses the complete push range; shared scenario edits select all referencing profiles. Dispatch common Build Hub with project path or `@slug` for every enrolled port of that slug. Unrelated infrastructure changes no longer default to FieldKit.
- `tools/qa/bundle.py` only bundles a cohort from one run/commit when all expected targets passed QA-4, filenames and hashes match the installed JARs, no duplicate targets, and JAR ZIPs are intact. Package order Forge → NeoForge → Fabric. ZIP contains only production JARs. Separate bundle-report.json records incomplete cohorts. Partial builds may produce individual passed JARs but not a full bundle.
- Each common CI job creates a machine-readable QA report, and failed tests upload logs and classified diagnostics. Classifiers are heuristics, not automatic code fixes.

## P2 — evidence and client experimentation
- The **QA v2 infrastructure and dashboard** workflow runs unit tests on infrastructure changes, supports manual dispatch, and refreshes daily. Download MineMods-QA-dashboard and open index.html offline. It inventories all available profiles and reads a bounded recent Actions artifact history. Unknown/stale/legacy are distinct states; expired or unavailable artifacts are never treated as passes. A new source tree invalidates old evidence. No public Pages deployment or repository settings change.
- **QA v2 experimental client** starts Gradle runClient under Xvfb/Mesa, checks for a visible stable window and captures screenshot/logs. It is development-mode only, not a final-JAR test and not visual gameplay QA. It can fail because of runner software graphics, missing tasks, resource downloads, or client problems. Inspect screenshot manually.
- QA levels: 0 no successful evidence; 1 compiled; 2 static; 3 production server boot; 4 production server assertions plus clean shutdown; 5 manually verified client (assigned only after the explicit human approval workflow; not server QA-4). Legacy QA-2 is the old static validator only; it does not imply the stricter v2 checks.
- Run unit tests locally: `python3 -m unittest discover -s tools/qa/tests -v`.

## Migration
The first cohort is Fragile Glass 26.1.2/26.2/26.3 on NeoForge/Fabric. Existing profiles are not mass-edited. Migrate one mod at a time: adopt exact names, QA profile, pinned dependencies, scenario, then remove dedicated packaging and legacy exception only after successful v2 runs. New profiles must not add themselves to the legacy list. Separate historical workflows require explicit migration; this is not a branch-protection policy for the whole repository. The fixed historic Harvest Glow 1.1.0 packaging workflow is manual/idempotent to avoid re-publishing its already existing tag on unrelated Build Hub completions.

## P3 — modpack compatibility
The **QA v2 modpack compatibility** workflow accepts a manifest ID from `tools/qa/packs/`. Each target pins Minecraft, loader, loader version, member projects, a gameplay scenario per member, and optional external dependencies with exact HTTPS URLs and SHA-256. Member JARs are built independently using their own Gradle/Java profiles; this initial workflow supports ordinary standalone Gradle projects, not legacy source-restore steps. All members must come from one commit/run/attempt and be installed byte-for-byte in one isolated server. Every member's scenario must pass on the same world. Reports are separate from individual release QA and cannot publish JARs or certify arbitrary modpacks. First manifest: physics (Fragile Glass + Natural Buoyancy, Fabric and NeoForge 26.3). To add a pack, declare its actual tested configuration; do not claim compatibility with all popular modpacks without an exact pack and version. Client graphics failure is classified separately from a mod gameplay failure. Verified release ZIPs remain downloadable from Actions even if Releases permission is denied.

The pilot pack automatically reruns on changes to either member's 26.3 source tree. Other pack manifests can be dispatched by ID; add matching source watches when enrolling them. The dashboard lists exact combinations separately and invalidates prior evidence when the manifest, a member tree, or a scenario hash changes. Compact compatibility reports are retained separately from full server logs.

## P4 — engineering diagnostics
- **API diff:** compares selected public/protected methods and fields in official Mojang named 26.x server JARs, verifies published SHA-1, reports added/removed classes and changed JVM descriptors. Older obfuscated releases require mapping normalization; client-only classes and loader APIs are not covered. It does not rewrite source.
- **Port audit:** `python3 -m tools.qa.port_audit projects/<port> 26.3 --out audit.json` scans source against evidence-linked advisory rules. Findings are hints, not proof of incompatibility. `migration-rules.json` records the exact scope of known changes.
- **Crash database:** known incidents enrich failure classification with evidence and scoped remedies; no automatic fixes.
- **Assets:** enrolled production JARs are checked for local missing asset references, model UV and PNG headers, while unresolved external references are warnings. This is not visual or texture-quality certification.
- **Performance:** the P4 pilot records idle and fixed 100-entity workload samples, gametime-derived effective TPS, tick query MSPT when present, and Linux process-tree RSS. No unmodded baseline, no universal overhead claim, no strict noisy-host performance threshold yet.
- **Virtual player:** actual offline Minecraft 1.21.1 Fabric localhost connection using Mineflayer. Tests survival, stone negative control, right-click lever, inventory, drop packet and one START_DESTROY_BLOCK for glass. No finished-dig packet is sent for that test. Server-side observations are authoritative. No GUI/rendering, no modded-protocol or 26.3 claim. P4 reports are diagnostics only, not release approvals. The pilot intentionally tests an existing older JAR without silently patching it.
- Workflow **QA v2 P4 engineering diagnostics** is manually runnable. Reports retain exact source/production JAR provenance. The direct npm client package is pinned; transitive npm lock is captured with the run, pending repository lockfile enrollment.

## P5 — project factory and release preparation
- Generate all eleven **draft** Gradle projects: `python3 -m tools.qa.scaffold tools/qa/examples/scaffold.json --out drafts/my-mod`. Templates are computed from the pinned registry. Each has loader metadata, Java entrypoint, Gradle files, pack.mcmeta, exact filename and draft QA profile. The empty entrypoint is not implemented gameplay. The common gate rejects draft profiles. Do not install in projects or remove draft until mechanics, side-specific registration and real scenarios are implemented.
- Port plan: `python3 -m tools.qa.port_plan plan projects/fragileglass-fabric-1.21.1 --out plan.json`. Includes every target, existing paths, source hashes and advisory migration findings; never overwrites sources. Use duplicates subcommand to identify byte-identical Java across ports, not semantic clones.
- Scenario generator: `python3 -m tools.qa.scenario_gen tools/qa/examples/scenario.json --out scenario.json`. Only explicit world fixtures and user-authored assertions; no automatic success markers. Review negative controls and verify real behavior.
- Performance comparison: same runner and Java, three fresh servers (official vanilla, loader only, production mod), same fixed seed and 100-item load. Delta is measured, not a causal or statistically definitive attribution. No hard gate yet. The baseline path cannot certify a release.
- Release preparation: `python3 -m tools.qa.release_prep <spec.json> <JAR-only.zip> <qa-reports-dir> --out upload-draft`. Requires exact complete eligible hashes, source commit and explicit changes. Outputs CHANGELOG.md and upload-plan.json for manual CurseForge/Modrinth preparation. Platform-specific IDs/availability must be verified before publishing; no API writes, tokens or automatic publication.
- Workflow QA v2 P5 project factory generates and compiles the eleven empty scaffolds without publishing them. It also runs comparative performance and a release-prep pilot from the retained Fragile Glass QA artifacts. The historical artifact-dependent pilot may fail after expiration; supply current real evidence rather than fabricate replacements.
- Open regression: the P4 player proved that Fabric 1.21.1 glass did not instamine after one start-dig packet (run 45750325141). Do not claim that old port fixed before its own regression rerun.

## P6 — shared sources and incremental QA
- Shared source roots live under `shared/<mod>/<scope>/src/main`. Add them to Gradle sourceSets and declare `shared_sources` in every consuming profile. Only identical bytes were moved in the Fragile Glass 26.x pilot; loader-specific adapters remain separate. Each target still compiles and tests separately. Build from the complete checkout.
- Build selection follows project changes, declared shared roots, scenario/watch paths and core QA tool changes. It never reuses a previous release JAR merely because an input matches. The report tree field for enrolled projects is now a composite tracked-input fingerprint; old single-tree evidence becomes stale intentionally. Client approval and compatibility reports use the same fingerprint.
- Official Mojang downloads are cached across selected engineering jobs with SHA-1 verification on every reuse. Optional external SHA-256 pinned dependencies can use MINEMODS_DEP_CACHE. Gradle already uses setup-gradle cache. Never cache test worlds or use a cached mod binary as proof of new QA. No full build-output reuse or measured speedup yet.
- The daily dashboard artifact now includes catalog/quality-debt.json, catalog/migration-queue.json and catalog/release-history.json. Recent GitHub evidence and release history are bounded; unavailable/expired evidence is not a failure or a pass. Release existence is not proof of QA. Open confirmed issues have explicit records.
- `python3 -m tools.qa.migrate <project> --mode server --scenario <path> --targets <targets...> --out proposal.json` generates an explicit reviewed profile proposal without editing the project. Natural Buoyancy 26.3 Fabric/NeoForge is the first two-port migration beyond Fragile Glass: pack range corrected, separate water regression added, legacy exception removed. Older ports remain unchanged. No mass migration or automatic release approval.
