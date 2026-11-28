# Reference mod analysis and adaptation

Mandatory under [AGENTS.md](../../AGENTS.md) for all MineMods projects when the owner references an existing mod, modpack, game, mechanic, architecture or asset. The goal is to understand why another implementation works, extract the useful idea and implement an improved, native solution for our product. A reference is not automatically a request to reproduce the whole mod, port its code, install it as a dependency or copy its appearance.

## 1. Interpret the owner's intention

Determine the actual product, target Minecraft/loader/Java/mappings, intended gameplay, constraints and task type. Read the current project instructions, source, API, registries, data definitions, known bugs and existing tests before planning. Use the already established target; do not port to additional versions unless authorized.

Classify the reference: player-facing behavior, algorithm/system architecture, content/progression, visual language, compatibility or explicitly authorized code reuse/fork. Identify the particular feature the owner wants, rather than automatically absorbing every adjacent system. If the owner broadly asks to study a reference mod, map its important systems at a high level, then investigate relevant subsystems in depth. Distinguish reference requirements, our existing behavior, user constraints, proposed improvements and out-of-scope ideas. Make reasonable documented assumptions when the task can proceed; don't turn ordinary design decisions into questions.

## 2. Research actual evidence

- Identify the canonical upstream project, exact Minecraft/loader version, release/tag/commit and relevant add-ons. A modpack can change base-mod behavior via configs, datapacks, scripts and add-on integrations: identify which component actually provides the feature. Do not attribute TFG pack behavior to TFC by default.
- Prefer actual version-pinned upstream documentation, source files, data definitions, tests and reproducible gameplay observations. Read the relevant paths/functions and trace call sites, inputs, outputs, state ownership, units, lifecycle and failure paths. A README, video or wiki may explain intent but does not establish exact implementation. Pin source URLs and paths, record inspected evidence versus inferred behavior, and explicitly mark inaccessible code or untested cases.
- Trace a system end to end: trigger -> inputs/environment -> rules/algorithm -> persistent state -> effects/rendering -> outputs. Identify dependencies, registries, recipes, tags, networking, data formats, performance constraints and edge cases that matter. Follow call paths rather than searching for a single suggestive class name.
- Separate a design principle from its particular representation. Explain why the approach works, what limitations it has, what can be improved and which parts do not belong in our product. Consider realism, player experience, performance, compatibility, maintainability and existing architecture. For scientific or physical mechanics, verify material assumptions against relevant reliable domain sources as needed; another mod's approximation is not scientific ground truth.
- Do not copy source code or assets simply to understand them. Read legitimately accessible sources; do not bypass access restrictions, obfuscation protections or private repositories. Do not execute arbitrary upstream build scripts solely to inspect an idea.

## 3. Decide what to adopt

For each relevant mechanic, record one decision: ADAPT (same useful principle, own design), IMPROVE (address a specific limitation), KEEP_EXISTING (our system already solves it), DEFER (out of scope), REJECT (conflicts with design/constraints), INTEGRATE (real interoperability via supported APIs), or LICENSED_REUSE (explicitly requested/authorized and permitted). State the reason and exact desired behavior. Avoid inventing complexity solely to differ from upstream. Preserve good established ideas when they fit.

The minimum useful research result is not a class-by-class inventory: it is a clear mapping from observed source behavior to our own concrete implementation plan. Examples: retain environmental relationships while replacing a fixed formula with our data-driven model; retain a progression concept while adapting resources/recipes and balance to our existing ecology; preserve an algorithmic invariant while designing our own storage, scheduling and integration.

## 4. Provenance and independent implementation

Default: implement from a behavioral/technical specification, not a line-by-line translation of the upstream code. Describe the mechanics as inputs, rules, invariants, outputs and test cases; then create our own packages, data structures, registries, resources, logic and balancing appropriate to the target. Avoid carrying over distinctive source structure, comments, names, constants, assets, GUI layouts or data tables merely through renaming. Using a general algorithm or standard API is different from copying a specific expressive implementation; select algorithms because they fit the problem.

Inspect the actual license(s), notices and per-file exceptions at the referenced revision before any copying, modification or redistribution of third-party code or assets. Publicly viewable does not mean unrestricted. If licensing is unclear or incompatible, do not transplant code/assets; use the independently specified idea, authorized API or defer the reuse. For permitted, explicitly selected reuse, record exactly what was used, applicable license, notices/attribution, source availability or other required obligations, and compatibility with our distribution. Do not imply that changing names removes obligations. Do not claim a formal clean-room process unless roles and evidence actually establish it; independent reimplementation in one agent is not clean-room certification. For restrictive or uncertain cases keep behavioral research and implementation separate where practical and seek clarification on reuse permission if needed. No legal assurance from this checklist.

Create original textures, models, audio and UI assets under [Visual QA](../art/VISUAL-QA.md) and its [vanilla atlas](../art/VANILLA-ART-ATLAS.md). A screenshot or inspiration reference is not permission to redistribute upstream art. Do not copy Minecraft or third-party assets into the repository merely for comparisons.

## 5. Integrate with our architecture

Use current project-local architecture/API documents and real code, not a stale API example. The owner-approved subsystem boundaries take precedence over a referenced mod's internal division. Prefer tags and data-driven rules, optional integrations, version-appropriate public APIs, server-authoritative gameplay, bounded simulation and separate client rendering. Preserve registry identities and existing saves; design migration only when required. Reuse shared systems rather than adding parallel stamina, climate, water or other state stores.

WorldLogic-specific: Core owns shared world/environment systems and physical simulation; addons depend on Core, not the reverse. Terrarian owns plant ecology and vegetation, Temperature owns player thermoregulation, Hydration owns player water balance, Trauma owns medical consequences, Metabolism owns digestion/nutrition where implemented. Other responsibilities follow the current project contracts. Do not infer implemented API availability from this ownership list. Inspect exact public methods on the active core revision, including units, coordinate/side conventions, ranges, fallback and serialization. Missing shared data: record the required method, inputs, outputs, units and semantics for the responsible developer; use a documented bounded fallback only when meaningful. Don't edit another addon's private implementation or bundle its classes without authorization. Coordinate API changes and regression checks against actual pinned JARs.

For worldgen adaptation, preserve approved base relief, old save IDs, seed determinism, coordinate scale and compatibility contracts unless the owner explicitly requests a generation redesign. Test natural chunk generation, not only synthetic samples. Evaluate cross-region/chunk seams, continuity, distribution, limits, reload and cost. Do not transplant an incompatible noise/router or generator just because the reference's screenshots look good.

## 6. Implement and verify

- Write the selected independent design and executable test IDs before implementation; implement the useful mechanic within the current product module. Start with a working vertical slice, then expand only what is required. Do not stop after documenting ideas if the owner asked for implementation.
- Test intended normal behavior, forbidden behavior, boundary conditions, save/reload, multiplayer/side issues, actual inputs and outcomes, and existing mechanics that must not regress. For adaptations, compare meaningful outcomes with the desired reference principle, not exact source parity unless explicitly requested. Test compatibility only where actual integration is part of the task. Use [QA assignment](../qa/QA-ASSIGNMENT-TEMPLATE.md), [QA guide](../qa/QA-V2.md) and existing final-JAR verification. If reference behavior is not verified, label it accordingly instead of inventing a comparison PASS.
- For large simulations, profile representative workloads and bound per-tick/per-chunk cost. For content, test real acquisition, recipes, tool/food progression, drops and interactions. For UI/assets, apply current Visual QA and separate actual client rendering evidence. A successful compile, copied upstream tests, source inspection or a plausible PNG does not prove gameplay or aesthetics.
- Keep useful improvements, explain intentional differences, preserve actual test results and distinguish NOT_RUN/UNSUPPORTED from PASS. No forced ports, global refactors or unrelated product changes. Use [naming policy](MOD-NAMING.md) for versioned deliveries.

## 7. Compact adaptation record

For nontrivial reference-driven changes, retain a short project-local Markdown note (for example REFERENCE-ANALYSIS.md, or an existing architecture/design section) alongside the code, and link it from the PR. One record per substantive feature or related family, not a new file for every small idea. Required fields:

| Field | What to record |
| --- | --- |
| Request and target | Owner's actual goal, Minecraft, loader, Java, scope |
| Upstream evidence | Project/version/commit, exact files/links and verified observations; uncertainty |
| Source principle | What the mechanic does and why, input/state/output, relevant limits |
| Decision | ADAPT / IMPROVE / KEEP_EXISTING / DEFER / REJECT / INTEGRATE / LICENSED_REUSE, with reason |
| Our design | Concrete implementation, owner module/API, balance and differences |
| Provenance | Independent implementation or specific permitted reuse, license/notice obligations and asset origin |
| Verification | Executable test IDs, run results, regression constraints and remaining gaps |

No duplicate prose QA report is required; executable scenarios and actual CI evidence remain the authority for tests. The note is a traceable design decision, not a new automatic CI gate or proof that the upstream analysis is correct.

## Worked example — TFC/TFG as references for WorldLogic

Illustrative workflow, not an audit of current TFC/TFG source. Suppose the owner asks for more convincing rivers. Locate the exact reference version and the module responsible for its drainage, distinguish pack changes from the base mod, trace how drainage paths and terrain relate, then extract the desired properties (for example coherent downstream flow, confluence, terminal behavior and plausible valleys). Compare those properties with the actual current WorldLogic generator. Choose our own algorithm and data layout that satisfy the requested properties while preserving approved base terrain and Core ownership. Test actual multi-seed terrain, tributary connectivity, coastal outlets, elevation/flow constraints, chunk boundaries, old-world invariants and generation cost. Terrarian consumes environmental APIs; it does not create a second river generator.

An internal precedent is [WorldLogic RC38 PR #229](https://github.com/MngkLrd/gradlejar/pull/229): its description reports an independently designed ocean-rooted regional drainage graph, spaced upstream sources, shared downstream paths and bounded actual base-height checks, with RC22 density preserved. The PR reports no borrowed code/assets and acknowledges missing full-length hydrological evidence. This is an example of a documented adaptation direction, not independent confirmation of upstream license, source behavior, final runtime success or completion of all river mechanics.

For a TFC-inspired soil or crop feature, separate natural soil characteristics (Core data/API) from plant responses (Terrarian). For a reference nutrition or injury system, map effects to Metabolism or Trauma and use shared APIs instead of duplicating state. These examples express our ownership and design process; they do not establish which exact features TFC implements.
