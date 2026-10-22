# Void Pickaxe 1.1.1

Minecraft 1.21.1 / NeoForge / Java 21.

**Changes from 1.1.0:**
- Replaced a corrupt PNG with a validated 16x16 RGBA pixel-art pickaxe texture (obsidian, netherite, diamond).
- Removed unnecessary descriptive text and uses-remaining tooltip.
- Restored **vanilla durability display**: no bar when new, bar only after first use.
- Strengthened GitHub Actions to fully decode and CRC-validate the PNG from the built JAR.

Gameplay is unchanged: progressive eight-tick bedrock break with cracks, no drops, five durability uses. The original item registry ID `voidchisel:void_chisel` is retained so saves are compatible.

Recipe:
```text
 . W .
 N S N
 . S .
```
W = Nether Star; N = Netherite Ingot; S = Stick.

**Build:** `gradle --no-daemon clean build`.

**JAR:** `build/libs/voidchisel-1.21.1-1.1.1.jar`

**Test in-game:** confirm full texture (no purple/black squares), no instruction tooltip, no new-item durability bar, bar after first successful break, no bedrock drops, fifth break consumes tool, mining progress cancels on release, client/dedicated server.
