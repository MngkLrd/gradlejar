# Last Witness 0.4.2 – Guard bow animation hotfix

- Bow-armed guards use vanilla Minecraft 1.21.1 RangedBowAttackGoal instead of the generic ranged-attack AI.
- Bow drawing animation only appears while actually charging a bow.
- Arrows are created as projectiles by the server and no longer displayed in the guard's offhand.
- Legacy archers automatically discard the obsolete 64-arrow offhand stack after upgrading.
- Existing guard gear, patrols, wanted behavior, village discovery and themed skins remain unchanged.

**Status:** source changes under CI build; Minecraft runtime test still required.
