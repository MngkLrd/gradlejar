# Norse Gear 0.3.1: physically modeled five-piece showcase

The following are the **actual Minecraft asset files**, not pictures produced by a generative image model:

- `models/item/bearded_axe.json`: beard, sharp edge, iron eye, fasteners and leather wrap.
- `models/item/dane_axe.json`: long haft and broad swept head (no isolated downward spike).
- `models/item/spear.json` and `javelin.json`: forged head, raised center ridge and shaft ferrule.
- `models/item/round_shield.json`: individual plank cuboids, embossed central boss and wooden grip.
- `textures/item/norse_showcase_materials.png`: authored 256×256 material atlas including 64×64 painted shield faces.
- `client/model/NorseArmorModel.java`: actual wearable armor geometry, including helmet ridge and moving pauldrons.
- `textures/models/armor/ironweave_layer_{1,2}.png`: 256×256 hand-shaped textures mapped to the Java mesh.

## Non-AI preview

`PREVIEW_3D_ACTUAL_031.png` was rasterized by `tools/render_showcase031.py` from the corresponding `models/item/*.json` using that exact texture atlas. It is **not** a Minecraft screenshot; it is a preview renderer with simplified face lighting.

`PREVIEW_ARMOR_ACTUAL_031.png` uses the ModelPart cubes parsed directly from Java and the armor texture layer, with a static dark mannequin to show uncovered regions. Minecraft lighting, animation, and UV orientation may vary: test inside the game.

To regenerate game assets run `python3 tools/showcase031.py`; to regenerate armor UVs run `python3 tools/armor_uv031.py`; to render source-based previews run `python3 tools/render_showcase031.py`. All require Pillow; previews also require numpy and OpenCV. **Do not run** the legacy `tools/build_3d.py`, which would overwrite the redesigned five assets.

Better Combat is still optional. The seven profile JSON files remain unchanged.
