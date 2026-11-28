# MineMods documentation by task

Current main is canonical. Start with [AGENTS.md](../AGENTS.md), then read only the required branches for your task. Cross-cutting QA requirements are unchanged. Product-specific notes remain inside their project directories; this index reorganizes shared documentation, not product repositories or Git branches.

| Task | Read |
| --- | --- |
| Naming / version / delivery | [Naming](development/MOD-NAMING.md) |
| Study or adapt another mod | [Adaptation](development/MOD-ADAPTATION.md) |
| Build, release and compatibility | [QA v2](qa/QA-V2.md) |
| Executable coverage | [QA assignment](qa/QA-ASSIGNMENT-TEMPLATE.md) |
| Network player actions | [Bot actions](qa/BOT-ACTIONS.md) |
| Recipes | [Recipe QA](qa/RECIPE-QA.md) |
| Any new/changed asset | [Common visual QA](art/VISUAL-QA.md), relevant [atlas](art/VANILLA-ART-ATLAS.md) rule |
| Blocks, trees, vegetation | [Block art](art/BLOCK-ART.md) |
| Items, tools, weapons and armor | [Item and armor art](art/ITEM-ARMOR-ART.md) |
| Animals, living mobs and animation | [Mob art](art/MOB-ART.md) |
| Interfaces and HUD | [Interface art](art/UI-ART.md) |
| Research history or evidence audit | [Vanilla research](research/VANILLA-RESEARCH.md), [real mod review](research/VANILLA-REAL-ART-REVIEW.md) |

Use the common art requirements and relevant guide(s); mixed features may need more than one. Historical research is not an everyday reading assignment. Original flat docs paths are compatibility pointers retaining old heading links; do not use them as a second source of truth. No new CI stage; do not run unrelated tests merely because a document exists.
