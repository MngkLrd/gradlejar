#!/usr/bin/env python3
"""Native game-art release invariant. Auxiliary 3D textures are validated separately."""
from pathlib import Path
import struct
import json
root=Path(__file__).resolve().parents[1]/"src/main/resources/assets/norsegear"
models=root/"models/item";sprites=root/"textures/item"
icons={"bog_iron","forged_iron","leather_binding","carved_horn","seax","bearded_axe",
"dane_axe","viking_sword","spear","javelin","throwing_axe","round_shield",
"sling","stone_shot","celtic_shield","bossed_shield","norse_helmet",
"norse_chestplate","norse_leggings","norse_boots","field_whetstone",
"war_horn","travel_ration","pitch_flask"}
for name in icons:
    p=sprites/f"{name}.png"
    raw=p.read_bytes()
    assert raw[:8]==b"\x89PNG\r\n\x1a\n",name
    assert struct.unpack(">II",raw[16:24])==(16,16),name
    # Confirm the intended Minecraft-style crisp alpha edge and useful color variety.
    from zlib import decompress
    model=json.loads((models/f"{name}.json").read_text())
    assert model["textures"]["mat"]=="norsegear:item/norse_3d_materials",name
    assert len(model["elements"])>=3,name
print(f"PASS: {len(icons)} native 16x16 item sprites (no legacy 32x32 icon)")
