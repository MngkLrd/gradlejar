# Rope Fences (Forge 1.21.1)

Connect two vanilla fence posts using a vanilla lead. A span has three visible pixel-art ropes and a thin, solid 1.5-block-high collision plane.

## How to use

1. Put two vanilla wooden/nether brick fence posts at the same height along X or Z, between 2 and 12 blocks apart. The gap must be air.
2. Hold a vanilla lead, sneak + right-click the first post.
3. Sneak + right-click the second post: one lead is consumed unless in Creative.
4. Break any section or either support fence to remove the entire connected segment and refund one lead in survival.
5. Sneak + right-click the same post twice to cancel. First selection expires after 60 seconds.

Ordinary right-click with a lead retains Minecraft's vanilla mob leash behavior. Multiple spans can meet at one shared post.

## Build

- Minecraft 1.21.1, Forge >= 52.1.0, Java 21, Gradle 8.10.2
- `gradle --no-daemon clean build`
- Output: `build/libs/ropefences-1.0.0.jar`
- GitHub Actions: Minecraft Mod Build Hub at MngkLrd/gradlejar, project_dir `projects/ropefences`

## Technical notes

The pixel-art texture is stored as a base64 source file and decoded into the standard PNG resource path during processResources. No dependencies, networking, block entities, or custom items.

Straight horizontal spans only, no diagonal or sloped runs. Collision deliberately forms a continuous plane, rather than three independently traversable thin strings.

## Testing checklist

- Survival: craft/place across 2-12 blocks, verify 1 lead consumed
- Jump/walk against the rope; ensure no traversal
- Break center rope and break either post separately; exactly 1 lead returned per link
- Two ropes sharing one post; break shared post -> both ropes clear
- Obstruction, wrong height, diagonal, same post
- Creative break no lead drops; reload world; multiplayer/dedicated server
