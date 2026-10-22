# LeashWrap 1.0.0
Vanilla lead attachment to tree logs and other solid blocks. Server-authoritative, no items or recipes.

## Usage
1. Put a lead on a cow, wolf, horse, or other leashable entity.
2. Hold a lead and right-click a log, plank, stone, or other sturdy block within reach.
3. The led entities are attached to a vanilla leash knot bound to that block.
4. To untie, hold a lead and **Shift + right-click** the same block.
5. Break the supporting block to remove the knot. Existing Minecraft leash-drop rules apply.

No lead is consumed when transferring an existing leash.
Supports multiplayer. Install the same mod on server and client.
The basic visual uses Minecraft's vanilla leash knot at the center of the block.
Decorative loose rope around an empty trunk is not implemented.

## Build
Use Java 21 on Minecraft 1.21.1; Java 25 on 26.2 and 26.3.
Run `gradle clean build` within a project; find JAR in `build/libs`.
