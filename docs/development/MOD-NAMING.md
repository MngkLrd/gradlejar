# Mandatory mod naming and versioning

Binding for all MineMods agents, all product series, cores, addons, standalone mods and ports. Apply from the next versioned delivery. This is a naming/version policy, not a request to rename all historic artifacts, change registry identities, or start a multi-target rebuild.

## Product version

Use exactly `MAJOR.MINOR.PATCH`, three nonnegative decimal integers with no leading zeroes except zero itself.

| Component | Meaning | Example transition |
| --- | --- | --- |
| MAJOR (first) | A new major release generation, significant release-level change | 1.4.7 to 2.0.0 |
| MINOR (second) | A substantial feature/content/system update within the same release generation | 1.2.3 to 1.3.0 |
| PATCH (third) | Bugfix, balance, small improvement, polish or incremental update | 1.2.3 to 1.2.4 |

When a higher component increases, reset the lower components to zero. Do not increment the major release generation for every ordinary iteration. Choose the component according to the actual scope of the changes, not their number of commits. Use 0.x.y for initial development before the first release generation if no published version history dictates otherwise. This policy distinguishes release generations, major feature updates and patches; it is not a claim that every increment has formal SemVer API compatibility guarantees.

Never add RC, alpha, beta, dev, snapshot, date or build-count suffixes to new product version strings, and never add a fourth component. Record development/QA status, commit, run, attempt and release channel separately (PR/release notes, QA evidence and platform metadata). A numeric version alone does not mean stable, eligible or ready. Never silently reset or reuse a published version for different bytes. For a rebuild of the same version, retain the exact source/provenance and do not replace a published artifact with different bytes under that name.

For existing RC-based products, including WorldLogic Core, the owner/product agent must establish the next numeric version from the actual previous release history and scope, and document the one-time mapping in the release notes. Do not assume RC33 means 33 patches, or promote a development build to 1.0.0 merely by removing the suffix. Examples in this document are hypothetical, not assigned versions. Use the same numeric version across Gradle, loader metadata, build-profile.json, release notes and affected dependency declarations. Coordinate updated core version constraints with its addon owners and verify the actual compatible JARs before delivery. Do not silently loosen or change dependency compatibility.

## Public JAR filenames

Series addons: `series_addon_MAJOR.MINOR.PATCH_MINECRAFT_LOADER.jar`

Series core: `series_core_MAJOR.MINOR.PATCH_MINECRAFT_LOADER.jar`

Standalone: `modname_MAJOR.MINOR.PATCH_MINECRAFT_LOADER.jar`

Use lowercase ASCII letters/digits for stable public name components; underscores separate fields. Normalize names consistently by agreement; do not invent new spellings on each update. The product version always precedes the Minecraft version. The Minecraft version is the exact target, including two-component release IDs where applicable; never concatenate the two versions into one six-component version. Loader is `forge`, `neoforge` or `fabric`; this suffix distinguishes ports of the same product and Minecraft version. It is not a loader runtime version pin. A port does not itself require changing product version if it is the same product revision. Explicitly version any genuine product changes.

Hypothetical examples:
- `worldlogic_core_1.2.3_1.21.1_neoforge.jar`
- `worldlogic_trauma_1.2.3_1.21.1_neoforge.jar`
- `worldlogic_terrarian_1.2.3_1.21.1_neoforge.jar`
- `worldlogic_thermal_1.2.3_1.21.1_neoforge.jar`
- `worldlogic_hydration_1.2.3_1.21.1_neoforge.jar`
- `fragileglass_1.1.1_1.21.1_fabric.jar`
- `fragileglass_1.1.1_1.21.1_neoforge.jar`

WorldLogic is the shared series prefix for its addons. Keep existing product terminology and the owner-approved public addon name. Public filenames are independent of internal `modId`, registry namespaces, package names, save/NBT identifiers and existing QA `slug`; do not rename those for cosmetic consistency. For example, a core with existing modId worldlogic remains worldlogic, even when the public filename uses worldlogic_core.

## Safe transition in the existing Build Hub

The existing strict [QA contract](../qa/QA-V2.md) currently computes canonical build filenames as `slug-loader-mcMINECRAFT-version.jar` and verifies exact names in profiles, static validation and release packaging. This policy does not silently change that executable contract. For the next product revision, set its real numeric version consistently, retain the exact required internal Gradle archive name and jar_glob for QA, and run all normal checks. Do not just rename an RC JAR to a numeric version while its internal metadata is unchanged.

After an artifact is actually eligible for delivery under the existing QA policy, the new helper can create a byte-identical public-name copy, verified against the source SHA-256 from the actual QA report. Example (substitute real version, source, digest and output directory):
```sh
python3 -m tools.qa.naming --series worldlogic --addon trauma --version 1.2.3 --minecraft 1.21.1 --loader neoforge
python3 -m tools.qa.naming --series worldlogic --addon trauma --version 1.2.3 --minecraft 1.21.1 --loader neoforge --source verified.jar --sha256 ACTUAL_SHA256 --out delivery
```
For a core use `--series worldlogic --addon core`; for a standalone use `--mod fragileglass`. A filename-only invocation prints the exact proposed name; the copy mode checks the input bytes, refuses a conflicting destination and rechecks the copy. Never provide invented digests. The helper does not establish eligibility, read or rewrite JAR metadata, sign JARs, verify that a version is actually appropriate, or publish anything. Verify metadata and eligibility first, and link both original CI evidence and the exact renamed delivery copy where available.

Automatic Build Hub artifacts, full multi-port bundles and release names remain on the existing internal naming contract until a separately tested, backward-compatible canonical naming migration. Do not break active agent branches, rename artifacts in place, rewrite QA evidence, bypass package validation, or mass-rebuild all mods merely to switch filenames. If the renamed copy cannot actually be created/delivered, report the limitation and exact existing verified artifact name rather than invent a link.

## Required agent behavior

Read this policy and current AGENTS.md before a new version. Select the correct component to increment, keep the prefix stable, use exact Minecraft and loader targets, coordinate affected versioned API dependencies, and deliver the actual verified JAR under the consistent public naming convention when the delivery helper is available. Keep old published artifacts and historical tags unchanged. Apply the same rules to ordinary mods, addon series and cores. Do not add independent suffixes such as final, fixed, new, build42, r33 or date to the public product version.
