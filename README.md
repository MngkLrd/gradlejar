# Minecraft Mod Build Hub — MineMods QA v2

Shared Gradle build infrastructure for real Minecraft mods. The bootstrap project (FieldKit) lives at root; independent ports live in `projects/<project>`.

**Development contract:** [AGENTS.md](AGENTS.md) · **QA guide:** [docs/QA-V2.md](docs/QA-V2.md) · **Compatibility registry:** [tools/qa/compatibility.json](tools/qa/compatibility.json)

## Build
Open [Actions](../../actions/workflows/build.yml) → **Minecraft Mod Build Hub** → **Run workflow**. Enter a project directory, `.` for FieldKit, or `@slug` to run every enrolled QA port of a mod. Main pushes build affected projects across the complete commit range. Pull requests do not publish.

Common CI compiles genuine Gradle projects using the profile's pinned Java/Gradle, verifies the JAR, evaluates the QA policy and uploads evidence. New gameplay projects require production-server runtime assertions; new client-only projects remain manual-validation candidates. Legacy projects in the explicit snapshot continue with the old static-only pipeline. Independent historical workflows remain outside the v2 gate until migrated.

## Artifacts
- `jar-*`: eligible individual JAR and SHA-256.
- `qa-report-*`: per-project machine-readable evidence.
- `qa-candidate-NOT-RELEASE-*`: debugging candidate, not a published release.
- `qa-diagnostics-*`: failure logs.
- `qa-v2-bundles-*`: complete verified JAR-only cohorts plus a separate package report.
- **QA v2 infrastructure and dashboard**: downloadable HTML/JSON status dashboard.
- **QA v2 experimental client**: development-client screenshot and logs, not production-client certification.
- **QA v2 manual client release**: explicit human approval for a complete client-only cohort, matching every exact candidate SHA-256. See the QA guide.

The standard targets use Java 17/21/25. Loader-specific Gradle files and metadata remain mandatory. The compatibility registry is not a claim that any arbitrary combination works.

## Important
Successful compilation is not gameplay validation. QA-4 is awarded only to the exact production JAR tested on the dedicated server, with positive assertions and clean shutdown. Manual client testing is still needed. Legacy static-only releases must never be described as runtime tested. No fake Minecraft APIs or manual bytecode name remapping.

Run tooling tests: `python3 -m unittest discover -s tools/qa/tests -v`.
