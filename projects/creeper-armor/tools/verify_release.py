#!/usr/bin/env python3
"""Optional per-project assertions invoked by the repository build hub."""
import json
import struct
import sys
from zipfile import ZipFile

JAR = sys.argv[1]
PREFIX = "assets/creeperarmor/"
ARMOR = [f"{PREFIX}textures/models/armor/creeper_layer_{i}.png" for i in (1, 2)]
REQUIRED = {
    "dev/creeperarmor/registry/ModCreativeTabs.class",
    "dev/creeperarmor/integration/jei/CreeperJeiPlugin.class",
    "dev/creeperarmor/gameplay/CreeperGameplayEvents.class",
    "data/creeperarmor/recipe/creeper_chestplate.json",
    f"{PREFIX}models/item/creeper_shield.json",
    f"{PREFIX}lang/en_us.json",
    f"{PREFIX}lang/ru_ru.json",
    *ARMOR,
}
with ZipFile(JAR) as jar:
    names = set(jar.namelist())
    missing = REQUIRED - names
    if missing:
        raise ValueError(f"Creeper Armor JAR missing {sorted(missing)}")
    for name in ARMOR:
        png = jar.read(name)
        if png[:8] != b"\x89PNG\r\n\x1a\n" or struct.unpack(">II", png[16:24]) != (128, 64):
            raise ValueError(f"Expected 128x64 pixel armor texture: {name}")
    model = json.loads(jar.read(f"{PREFIX}models/item/creeper_shield.json"))
    assert model["display"]["thirdperson_lefthand"]["rotation"][1] == 0
    assert model["display"]["thirdperson_righthand"]["rotation"][1] == 0
print("PASS: 128x64 armor, correct shield yaw, JEI, creative tab")
