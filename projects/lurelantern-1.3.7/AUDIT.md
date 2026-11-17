# Lure Lantern 1.3.6 audit

Source origin: user-provided June 2026 1.3.2 JAR, SHA-256 36365af25e45282dfa2ef79e69f57ad698ce289e605fd82319a52d46a1ee308f.

Updated from 1.3.5:
- Recompiled `LureLanternBlock` to add a three-stage advanced-only mode selector. The basic lantern remains binary off/on.
- Recompiled `LureLanternBlockEntity` with `LureMode` persisted in NBT. Original `CapturedMob` NBT, public methods, capture, release, item transport and five-tick pathfinding cadence are preserved. Old active advanced lanterns with no `LureMode` default to auto-capture to prevent save regressions.
- Replaced the client preview renderer with smooth rotation, entity-adaptive scaling, stable centering and category-specific pixellated emissive glow. Client-only entities are not spawned into the world.
- Added tests for lure-only vs auto-capture plus original destroy/re-place/release.
- Added translated actionbar messages and a glow texture; all 1.3.4 vanilla-style lantern textures unchanged.

Gradle repackages the unmodified 1.3.2 classes and resources, excluding the two replaced classes; modern classes and assets take priority through duplicate exclusion. GameTest classes and structure excluded from the release JAR.

CI verifies compilation and server-side tests. Client shaders/appearance cannot be verified headlessly.
