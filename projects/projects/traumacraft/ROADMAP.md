# Traumatic development roadmap

## Stable mainline

- **Minecraft 1.21.1, NeoForge, Java 21** is the single feature-development target.
- Ports (Minecraft 1.20.1 Forge; 26.3 NeoForge) follow tested feature releases. Do not split core mechanics into independently designed forks.
- Preserve mod namespace and registry IDs `traumacraft:*` for world compatibility. The display name is **Traumatic**.
- Keep the existing 7-part HP, hit location, blood loss, fracture logic and inventory healing architecture stable unless a separately approved bug fix explicitly touches them.

## Next milestones

### 0.4 - Injury feedback and usability (low risk)
- No intrusive commands/chat/action-bar diagnostics in survival.
- Medical HUD pulse briefly when a new injury occurs, highlight damaged body part. Tiny and toggleable.
- Injury summaries in the anatomy screen: light/heavy bleed, fractures, blacked-out parts, and treatment suitability.
- Config: HUD visibility (always/on injury/off), opacity, anchor/offset, and effect strength.

### 0.5 - Medical treatment depth
- Item cooldowns and per-treatment use time; no instant surgery while sprinting.
- Bandage vs heavy bleed restrictions, field surgery time and risks, separate recovery thresholds.
- Keep existing medical items and recipes; balance via config/data files rather than changing core damage code.

### 0.6 - Distinct injuries
- Damage-location consequences (arm accuracy/mining, leg mobility, severe torso trauma) implemented in an isolated server-side modifier layer.
- Temporary pain and shock debuffs; readable vanilla-like screen feedback, no constant blur.

### 0.7 - Survival integration and interoperability
- Damage type tags, modded medical items via item tags, optional JEI/EMI information, config, server-authoritative sync.
- Dedicated server and modpack QA for HUD, respawns, dimensions, deaths and item persistence.

## Porting gate
Each milestone: compile 1.21.1 -> verify JAR -> in-game test -> tag known-good -> port independently to 1.20.1 and 26.3 -> rerun world creation, GUI, crafting and JEI smoke tests.
