# Machine-readable mod QA assignment

The assignment is the executable test suite consumed by GitHub Actions, not a prose document for a human or another chat agent. No separate Markdown or natural-language technical specification is required.

## Required executable files

- `tools/qa/scenarios/<slug>.json`: schema-1 server fixture and gameplay assertions, referenced by `qa.scenario` in each enrolled port's `build-profile.json`.
- `tools/qa/scenarios/<slug>-player.json`: schema-1 network-player actions and independent server assertions, referenced by `qa.player_scenario` wherever the protocol backend is supported.
- Additional GameTests or automated client tests when necessary, provided they are actually integrated into the GitHub Actions test path. Do not assume a new test file runs by itself.

Use the actual supported schemas and actions in [server runner](../tools/qa/server.py), [player suite](../tools/qa/player_suite.py) and [Minecraft bot](../tools/qa/bot/player.cjs). Existing executable examples: [Fragile Glass server scenario](../tools/qa/scenarios/fragileglass.json) and [Fragile Glass player scenario](../tools/qa/scenarios/fragileglass-player.json). For a new mod, create tests for its own behavior, not copies of unrelated assertions.

## Coverage requirements

Reconcile the requested feature list against the actual registered items, blocks, entities, recipes, effects, menus and other relevant content. Assign stable IDs using assertion names, exercise every added or changed variant, and test applicable acquisition, use, crafting, placement, durability, consumption, drops, persistence, interactions, boundaries and negative controls. Use actual network actions for player interactions where supported; commands may prepare fixtures and observe independent outcomes, but must not perform the player action under test.

If coverage mappings or exceptions need extra structure, keep them in an optional JSON manifest (for example `tools/qa/assignments/<slug>.json`) containing feature/content IDs, target applicability, criticality, executable test paths and assertion names. This is a machine-readable inventory, not an automatically executed or validated schema today. Never substitute it for the runnable scenarios. No Markdown checklist or duplicated prose assignment is mandatory.

## Execution and evidence

Commit scenarios and their profile references, and use the existing Minecraft Mod Build Hub on GitHub Actions. Inspect actual QA reports and logs for every declared target and exact production JAR SHA-256. Fix failures within scope and rerun. Record unsupported or unexecuted required checks explicitly; do not claim they passed. Existing release gates and client approval requirements remain unchanged. A server assertion is not evidence of GUI/rendering correctness. No human operation of the test bot is required.

Keep shared infrastructure stable. A missing backend or action is a limitation to report, not permission to bypass the gate or silently invent a supported action. Only make shared tooling changes under the repository's existing scope rules.

## Coverage array (P9.0)

For new or changed enrolled server project trees, add the following structure at the top level of the existing executable server scenario, next to its `schema` and `steps`. Do not create a prose assignment. Example structure (replace with actual mechanics and matching named assertions):

```json
{
  "coverage": [
    {
      "id": "glass-break",
      "kind": "mechanic",
      "subject": "Arrow breaks full glass",
      "checks": [
        {"backend": "server", "name": "Arrow breaks full glass"},
        {"backend": "server", "name": "Arrow does not break stone"}
      ]
    }
  ]
}
```

Allowed kinds: mechanic, item, block, entity, recipe, effect, menu, other. Optional `targets` lists applicable loader-mcVERSION IDs. Every active reference must match exactly one named assertion with `assertion: true` and a nonempty expectation in the actual active server/player scenario. A player reference requires an enrolled player scenario. The existing runner executes the steps, then the coverage gate maps actual independent assertion evidence to PASS/FAIL/NOT_RUN. Missing or ambiguous references and incomplete declared coverage fail the gate. Supported protocol targets require a player scenario when using coverage. The GitHub job summary shows counts; the JSON report contains individual entries. No automatic discovery of all Java registrations or semantic completeness is claimed. Developers must still reconcile the inventory with actual content. Previously enrolled unchanged project trees without declarations are identified as not-declared, never coverage certified; legacy profiles retain their prior policy.
