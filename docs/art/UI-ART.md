# HUD and GUI art

Mandatory when changing screens, interfaces, icons or HUD. Follow [common Visual QA](VISUAL-QA.md); this is not a complete client test backend.

## HUD and GUI

- Use the existing vanilla UI language for the specific target: font, icon grid, panel borders, spacing, slot geometry, hover/disabled state and readable contrast. Do not paste a generic web dashboard into Minecraft. Use the actual intended screen layout, not an image-generation mockup as evidence.
- Check normal and extreme states (empty/full, injured/healed, heat/cold, disabled, stacked effects, long tooltips). Check supported GUI scales, small/large window and relevant inventory/chat overlays; labels must not clip, overlap or cover vanilla HUD controls. Verify independent left/right limb markers and status/tooltip correspondence for anatomy UI, and synchronized values for climate/fatigue HUD when those features are affected.
- A vanilla texture reference is a visual comparator, not permission to override an unrelated interface. Keep state and display logic separate. Do not claim client UX verification from Java layout math or a successful server test alone.

