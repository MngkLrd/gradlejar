# Rope Fences 1.1.0 — NeoForge 1.21.1

Use a normal lead in your main hand; sneak-right-click two level, aligned fence posts 2–12 blocks apart. One lead makes two thin, subtly sagging ropes. The fence-height collision is separate from the visual renderer. Break a rope or supporting post to dismantle the span.

## Art direction
The rope is drawn with Minecraft's vanilla leash render type, two crossed ribbons and the leash's brown two-value color ramp. No custom rope texture, UVs, caps, color inserts, or oversized block models. One continuous curve reaches the center of each vanilla fence post. The client-only renderer uses a consistent thickness and a bounded, cached endpoint lookup. The selection outline follows the ropes; the separate collision plane reaches 1.5 blocks.

The legacy rope_span block ID and state properties are retained. New spans use non-ticking block entities. **Existing spans placed before 1.1.0 may need to be dismantled and re-created if the old chunks lack block entity data.** Back up the world before updating.

## Build
Minecraft 1.21.1, NeoForge 21.1.262, Java 21, Gradle 9.2.1.
`gradle --no-daemon clean build`
Output: `build/libs/ropefences-neoforge-1.21.1-1.1.0.jar`.

## QA
- Test one empty block between two posts, and 3, 6 and 12-block spans on both axes.
- Check four spans on a single central post; no colored ends, no detached strands.
- Inspect actual gameplay scale, day and night, normal Minecraft and shaders separately.
- Inspect at least 3 adjacent segments: continuous curve and consistent strand thickness.
- Test survival, creative, multiplayer and reload; lead refund, obstruction, post removal and collision.
- Test new placements and old worlds independently; client and dedicated server.
- Straight and level spans only. No new items, recipes or custom rope texture.
