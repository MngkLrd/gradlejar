# P9.2a — Content discovery (advisory)

Build Hub extracts a deterministic inventory from the exact packaged JAR for enrolled server/client projects after QA. The report links item definitions, item-model hints, blockstates, block-loot hints, recipes, translation hints (including entities/effects), worldgen and dimension resources to named coverage evidence. The namespace is explicit; overrides and foreign resources are not assumed to be newly registered content. The report is checksum-bound.

For new concrete content, use the exact resource ID (e.g. demo:box) as the coverage subject, with the corresponding kind; use separate entries for block and block item. A prose subject or unregistered model variant may produce NO_EXACT_DECLARATION, an advisory review task, not proof of a missed mechanic. DECLARED_PASS means that the linked named assertions passed, not that every behavior was tested. Missing report or unexecuted entries are not passes. Existing scenarios need no migration.

Run: python3 -m tools.qa.content_discovery <final.jar> --modid <id> --qa-report <qa-report.json> --out content-discovery.json

Live Java registries, code-only registrations, menus, dynamically added content, conditional recipes, and actual client rendering are not discovered by this stage. A later loader-specific runtime probe is needed. The report does not alter release eligibility or certify coverage. Do not equate empty resource inventory with an empty registry.
