# Sleep Debt 3.1.0

Minecraft 1.21.1, NeoForge 21.1.x, Java 21. Realistic survival: Sleep, Body and Stress; shelter, camp rest, sleep quality, coffee farming and coffee maker. No active Sleepless or hallucinations. Legacy entity and egg IDs are inert for save migration. 

Coffee maker uses base liquid/drink, ingredient, and coal/charcoal. Eleven recipes; two energy drink upgrade paths. JEI/EMI reflect the same shared recipe list. NeoForge native config screen is available. Phantom integration can be disabled; spawn counts are not changed.

Artwork authored in native pixel grids using the Blockbench Minecraft Style Guide: limited hue-shifted ramps, deliberate clusters, top-left light, no random noise or AA. GUI uses native Minecraft fonts. Live visual quality, multiplayer/Comforts/Soaring Phantoms compatibility require game testing. See QA_ALPHA18.md.


## Alpha 19
Cumulative activity-based fatigue, separate acute caffeine and tolerance, calm recovery, actual lit campfire heat, visible threats, incense cooldown, recipe-bound progress. Inventory journal tab opens five large vanilla-style pages, server-generated snapshots, bounded 48-point time series and cumulative activity histogram. Existing book remains an alternative shortcut. Graph strokes are half a logical GUI pixel. Minecraft 1.21.1 NeoForge, Java 21.


## 3.1.0
Restored detailed copper HUD with pixel-authored bevels, corner fittings and framed penalty strip. Journal uses a single unblurred background pass, followed by data and then widgets; native tiled border and fine chart strokes. Five pages and server history preserved. This is a focused visual/UI correction, not a physiology rebalance. Client and server protocol remains 5. Live graphical QA is required.


## 3.1.2
Independent material-first pixel artwork (24 items, 15 material maps, 8 growth stages), textured HUD meter atlas, no centered text shadow on journal chart labels, explicit particle texture, nonintersecting awning and bedroll geometry, espresso machine with open brewing bay and nozzle. No gameplay changes. Static asset and compilation tests are not graphical client tests.


## 3.1.3
Fresh native artwork set, separate material ramps, directional surface shading, shaped item outlines, HUD relief and distinct meter artwork. Integer geometry and 1:1 UVs for lantern, incense burner and dreamcatcher. Gameplay and journal logic unchanged. See ART_STANDARD.md. Visual QA in Minecraft remains required.


## 3.1.5
New static PNG artwork for items, crops, materials, effects and GUI. Approved coffee, energy can, coffee berries and pillow retained at pixel level. Gradle copies fixed PNG bytes from the checked-in archive without drawing or recoloring. Gameplay and model geometry unchanged; in-game visual QA required.
