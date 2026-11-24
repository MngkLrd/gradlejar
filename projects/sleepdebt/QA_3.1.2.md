# 3.1.2 art QA
- Compare 24 native item sprites at inventory scale. Check dark outline, top-left light, coherent clusters and hue shift; no noise or smoothing.
- HUD: all three meters at 0/25/50/100 percent; cropped native atlas, not scaled; inspect wood relief.
- Journal Activity: labels without drawCenteredString shadow, inspect EN/RU at GUI scale 2–4.
- Break dreamcatcher, awning, bedroll, coffee machine; particle sprites must exist. Inspect cutout from both sides.
- Awning roof and legs do not intersect; rotate coffee machine and inspect tray/nozzle; confirm hitboxes and operation.
- CI checks files, not runtime client visuals. Mechanics unchanged.
