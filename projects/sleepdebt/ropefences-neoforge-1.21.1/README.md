# Rope Fences - NeoForge 1.21.1

This is a **NeoForge** port of the Forge Rope Fences mod with the same mod ID (`ropefences`) and mechanics.

## Use
- Place two vanilla fence posts in line, on the same level, with 1-11 air blocks between them (2-12 blocks apart).
- With a vanilla lead in your main hand, sneak and right-click the first post; do the same on the second.
- One lead is consumed in Survival. The span has two rendered neutral-brown rope strands and a solid, thin 1.5-block collision plane.
- Break a rope segment or a supporting post to dismantle the entire line and recover one lead in Survival.
- Selecting the same post twice cancels the action. Selection expires after 60 seconds.
- Connecting several separate spans to one fence is possible.

## Build
- Minecraft: **1.21.1**
- Loader: **NeoForge 21.1.262** (Java FML)
- Java: **21**
- Gradle: **9.2.1**
- Command: `gradle --no-daemon clean build`
- JAR: `build/libs/ropefences-neoforge-1.21.1-1.0.1.jar`
- Build Hub project: `projects/ropefences-neoforge-1.21.1`

## Implementation
V1.0.1 removes the bright red/green texture artifacts from the old resource, keeps short one-air-block spans, supports multiple links meeting at the same post, and draws a slim selection outline around the strands instead of the invisible collision wall. The legacy end blockstates remain for existing world compatibility, but the end geometry is just a continuation of the rope and has no decorative caps.\n\nThe original Forge project remains at `projects/ropefences`. This port uses NeoForge's mod event bus, gameplay event bus and deferred block registry. No custom items, networking or libraries. The block model and localization resources match the Forge edition.

## Test plan
1. Client and dedicated server launch without errors.
2. Place, reload, and break 2-, 4- and 12-block spans in survival.
3. Attempt to walk and jump across a span, including between ropes.
4. Test breaking one section, the first fence, the second fence, a shared fence supporting two spans, and creative destruction.
5. Confirm exactly one lead consumed and returned per span.
6. Verify that non-collinear, different-elevation and obstructed placements do not consume a lead.
7. Test in multiplayer and with a second mod affecting vanilla leads.

Known limitation: diagonal and sloped spans are not currently supported. This port does not make vanilla leads attachable to non-fence blocks.

## 1.0.1 visual regression checks
- Every direction and each shared support post uses only muted vanilla-like rope colors (no colored bands).
- The one-empty-block span between two fence posts shows two continuous cables on both sides.
- Cross-shaped arrangements with one central support show clean connections.
- Pointing at ropes selects only the strands, while the collision plane remains impassable.
