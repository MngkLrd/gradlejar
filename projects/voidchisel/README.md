# Void Pickaxe 1.1.0

Minecraft 1.21.1 · NeoForge 21.1+ · Java 21

This mod upgrades the original Void Chisel item into **Void Pickaxe**. Its registry ID stays `voidchisel:void_chisel` to avoid breaking existing save inventories.

- A **real PickaxeItem** with Netherite-tier ordinary block mining.
- Bedrock breaks after holding the attack key for 8 server ticks (about 0.4 s).
- Mining cracks appear while holding. Releasing, switching targets, or losing line of sight cancels progress.
- Each successfully destroyed bedrock block consumes one of five durability points. Bedrock never drops.
- The durability bar is shown even when unused, and the tooltip shows the exact remaining uses.
- The item is not repairable; ordinary block mining also uses durability.
- Server checks held tool, permissions, reach, targeting and protection cancellation.

Recipe unchanged:
```text
 . W .
 N S N
 . S .
```
W = Nether Star, N = Netherite Ingot, S = Stick.

### Build

`gradle --no-daemon clean build`

JAR: `build/libs/voidchisel-1.21.1-1.1.0.jar`.

### Manual testing checklist

- Survival: hold attack on bedrock for ~0.4s; cracks, then block disappears.
- Release early, change the target, or swap the item: mining stops and cracks clear.
- After 5 successful bedrock blocks the pickaxe breaks; durability bar is visible on fresh items.
- Bedrock yields no items or XP. Bedrock is not removed when unheld or out of range.
- Ordinary stone and ores mine as expected for a Netherite-tier pickaxe (durability also decreases).
- Try on a dedicated server, near claims, with high latency, and across dimension changes.

Compilation and JAR checks run in GitHub Actions. This is **not** a substitute for running the game.
