# Lure Lantern 1.3.9 | NeoForge 1.21.1

Glass transparency fix requested after testing 1.3.8.

- Advanced blue and basic violet glass use 30% per-pixel **opacity** (alpha 77/255, 70% transparent), in active and inactive states.
- Keep original RGB colors, metal texture, item scale, captive preview, game logic and saved-mob NBT untouched.
- Avoid cumulative extra glass layers by omitting horizontal up/down faces from the glass cuboid in all eight block models.
- The custom Advanced held-item renderer also renders only four vertical sides of glass, after the metal frame and the captured entity.

## Build
Minecraft 1.21.1 / NeoForge 21.1.262 / Java 21 / Gradle 9.2.1.

`gradle clean build`
Output: `build/libs/lurelantern-1.3.9.jar`
Optional server tests: `gradle runGameTestServer`.

## Client testing
Check first-person right/left hands, empty/filled Advanced, normal violet, active/inactive, placed/hanging, with and without shaders. GameTest does not verify translucent client rendering.
