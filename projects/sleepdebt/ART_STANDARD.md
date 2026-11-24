# Sleep Debt art standard (Blockbench Minecraft Style Guide)
Source: https://blockbench.net/wiki/guides/minecraft-style-guide/

- Readable shape before detail. Use minimal recognizable geometry; use planes and transparency for small detail instead of numerous cubes. Avoid stair-step approximation of slopes or curves. Respect proportions and integer model scale.
- Match UV length to model face dimensions. Avoid texture/model mixels. Review the actual visible face region, not only the atlas.
- Choose the midtone, one shadow and highlight first. Expand the ramp only if the material needs it. Balance hue, saturation and value steps. Use top-left light for item sprites and a substantially darker but shaded outline.
- Distinct material properties govern the shape and placement of connected color clusters. Avoid random noise, needless dithering, banding, pillow shading and pancake shading. Polish diagonal/curve stepping. No scaled-up detail in native textures.
- Test block tiling at 3x3 before and during shading. Review at 16x16 and enlarged nearest-neighbor scale, and inspect models from all sides in game. The GUI uses native-pixel artwork; chart geometry is a deliberate separate readability requirement.
- Entity shading: top/front brighter than bottom/back, relative to the actual box UV. Not applicable to the retired Sleepless migration ID.

Technical checks are not proof of artistic quality. No live client rendering is claimed by CI.

Release review: inspect item contact sheet at 1:1 and enlarged scale, check 3x3 material swatches, check model UV ratios mechanically. In-game inspection is a separate required step.
