# Minecraft QA player action library

Actions are JSON objects with action and args fields. Existing schema-1 player scenarios continue to work. All runtime checks need independent named server assertions. No graphical rendering, custom modded protocol or custom GUI support is implied. Vanilla inventory/crafting protocol actions are not graphical UI tests. See [Visual QA](../art/VISUAL-QA.md) for client evidence.

| Action | Arguments |
|---|---|
| position | x,y,z (observe fixture teleport, not movement) |
| startDig / abortDig / dig / activate | x,y,z,block; activate optional face 0..5 |
| inventory / drop | item; inventory also count |
| equip | item, optional destination (hand/off-hand/head/torso/legs/feet) |
| move / jump | ms 100..4000, yaw degrees in Mineflayer convention, direction, forward (jump), sprint, sneak |
| craft | output item, count of recipe executions 1..16, optional table {x,y,z,block} |
| place | x,y,z,block (support block), item, face 0..5 |
| attack / interact | nearby entity name |
| use | item, ms 100..4000 |

Fixtures can arrange blocks, inventory and player position, but cannot perform the action under test. Use the bot and then independent server evidence. A successful packet/API callback is not a gameplay assertion. Controls run against the official Fabric loader without any mods on 1.20.1, 1.21.1 and 1.21.4; they are diagnostic, not release evidence. Inspect actual CI results before treating a new action as verified. Existing mod-specific release tests remain mandatory. The standalone control workflow runs only on bot changes or explicit dispatch. New action APIs not exercised by the control suite (interact/use and non-default options) remain experimental. No new supported protocol targets.

The pinned Mineflayer 4.26.0 client sends `player_loaded` on 1.21.4 spawn (upstream fix #3986); inventory actions wait for slot synchronization, rather than assuming an RCON reply arrived after the client update.

### Explicit crafting grid and discovery

`craftGrid` accepts item, count, a 2x2 or 3x3 matrix of vanilla item names/null, and a table fixture for 3x3. It clicks actual inventory/grid slots and reads the server output slot rather than synthesizing a local recipe result. Independent server assertions remain required. See [recipe QA](RECIPE-QA.md). No modded registry IDs or custom menus are supported by this protocol backend.
