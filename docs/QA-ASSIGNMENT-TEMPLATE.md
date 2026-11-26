# Mod QA assignment template

Copy to `tools/qa/assignments/<slug>.md` and populate from the user's requested behavior and the actual mod. This is an internal developer artifact, not a task for the user. Use one assignment for all ports and specify exceptions explicitly. Do not put test-only resources in the release JAR unless an established, reviewed harness requires it.

## Product and scope
- Mod slug, release version, target Minecraft/loader pairs, Java/mappings:
- Requested behavior and explicit non-goals:
- Source/registration/resource inventory (list all additions or changes, including variants):
- Dependencies and compatibility scope:

## Coverage matrix
For every feature and content entry, assign stable test IDs. A row can cover equivalent variants only if it lists them and the executable tests exercise each.

| ID | Feature/content (exact IDs where applicable) | Target(s) | Expected behavior | Backend and executable test reference | Critical? |
|---|---|---|---|---|---|
| EX-01 | Example feature — replace with actual mod content | All declared | Observable intended outcome | scenario path + named assertion | Yes |

## Agent task for the test player
For each test ID, specify:
- Preconditions: clean world, game mode, inventory, blocks/entities, exact configuration and dependencies.
- Player actions: concrete interaction sequence; use real network actions when supported (not a command that directly performs the behavior being tested).
- Independent assertions: expected state before/after; use authoritative server state or appropriate client evidence.
- Negative and edge cases: invalid target, incorrect tool, boundary condition, repeat action, multiplayer/restart/save behavior where relevant.
- Content lifecycle as applicable: acquire/craft/place/use/equip/consume/break/drop/persist; verify all added variants, recipes, tooltips, effects, containers, mobs, worldgen, or other relevant features.
- Backend: network player, server assertions, GameTest, or client test; identify unsupported actions, versions and required visual/manual evidence explicitly.
- Executable test path and exact named assertion(s). Never use this document alone as a test.

## Per-target evidence
| Target | Test IDs executed | PASS | FAIL | NOT_RUN / UNSUPPORTED (reason) | CI run / exact JAR SHA-256 |
|---|---|---|---|---|---|
| | | | | | |

Record failures and reruns, link to regressions, distinguish server-only from actual network-player and client evidence. Missing critical behavior blocks a verified release. A complete tested release requires the applicable tests and existing release gates to pass; explicitly disclose any remaining coverage gap. Do not invent results, weaken checks or certify unsupported backends.

## Final developer review
- [ ] All user-requested mechanics mapped to test IDs.
- [ ] Every registered added/modified content entry reconciled against coverage.
- [ ] All applicable positive, negative and lifecycle assertions implemented.
- [ ] Required network-player tests executed on supported targets; alternatives and limitations recorded.
- [ ] Exact final JAR evidence retained for each advertised target.
- [ ] Failed checks investigated, fixed and rerun; unresolved blockers disclosed.
- [ ] ZIP contains only eligible JARs; user receives concise results rather than technical chores.
