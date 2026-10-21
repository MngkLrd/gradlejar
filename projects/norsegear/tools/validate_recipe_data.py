#!/usr/bin/env python3
"""Reject broken 1.21.1 shaped-ingredient strings before Java compilation."""
from pathlib import Path
import json

root = Path(__file__).resolve().parents[1]
recipes = sorted((root / "src/main/resources/data/norsegear/recipe").glob("*.json"))
assert len(recipes) == 24, f"Expected 24 recipes, got {len(recipes)}"
for path in recipes:
    data = json.loads(path.read_text(encoding="utf-8"))
    if data["type"] == "minecraft:crafting_shaped":
        assert all(isinstance(value, (dict, list)) for value in data["key"].values()), path
        assert all(isinstance(x, dict) and ("item" in x or "tag" in x)
                   for value in data["key"].values()
                   for x in (value if isinstance(value,list) else [value])), path
    elif data["type"] == "minecraft:crafting_shapeless":
        assert all(isinstance(value, (dict, list)) for value in data["ingredients"]), path
print(f"PASS: validated {len(recipes)} Norse Gear recipe JSON files for 1.21.1")
