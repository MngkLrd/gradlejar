#!/usr/bin/env python3
"""Fail the CI release for missing item models, PNGs, guide entries, broken recipes
or missing optional/required module declarations. Requires only Python stdlib."""
from __future__ import annotations
import json
import struct
import sys
import zipfile
import tomllib
from pathlib import Path

jar=Path(sys.argv[1])
with zipfile.ZipFile(jar) as z:
    names=set(z.namelist())
    def data(name):
        if name not in names: raise ValueError(f"MISSING in JAR: {name}")
        return z.read(name)
    def js(name):
        try:return json.loads(data(name).decode("utf-8"))
        except json.JSONDecodeError as e:raise ValueError(f"{name} invalid JSON: {e}") from e
    metadata=tomllib.loads(data("META-INF/neoforge.mods.toml").decode("utf-8"))
    deps=metadata.get("dependencies",{}).get("sleepdebt",[])
    dep_names={x.get("modId") for x in deps if x.get("type")=="required"}
    if not {"neoforge","minecraft","jei","emi","jade","patchouli"}.issubset(dep_names):
        raise ValueError(f"Required dependencies missing: {dep_names}")
    item_ids=[
        "roasted_cocoa_beans","coffee_grounds","dried_herbs","dream_shard",
        "coffee","strong_coffee","long_coffee","energy_drink","warm_milk",
        "herbal_tea","dream_tea","caramel_coffee","bitter_tonic","cocoa_biscuit",
        "pillow","sleep_mask","sleepless_spawn_egg",
        "sleep_journal","herbal_poultice","incense_bundle",
        "coffee_seeds","coffee_berries","roasted_coffee_beans",
        "coffee_machine","camp_bedroll","camp_awning","camp_lantern","dreamcatcher","incense_burner",
    ]
    for lang in ("en_us","ru_ru"):
        l=js(f"assets/sleepdebt/lang/{lang}.json")
        for item in item_ids:
            key=("block" if item in {"coffee_machine","camp_bedroll","camp_awning","camp_lantern","dreamcatcher","incense_burner"} else "item")
            k=f"{key}.sleepdebt.{item}"
            if k not in l:raise ValueError(f"Missing translation {lang}:{k}")
    for item in item_ids:
        m=js(f"assets/sleepdebt/models/item/{item}.json")
        if not m.get("parent"):raise ValueError(f"Item {item} has no parent model")
    block_names=("coffee_machine","camp_bedroll","camp_awning","camp_lantern","dreamcatcher","incense_burner")
    for b in block_names:
        js(f"assets/sleepdebt/blockstates/{b}.json")
        js(f"assets/sleepdebt/models/block/{b}.json")
        js(f"data/sleepdebt/loot_table/blocks/{b}.json")
        js(f"data/sleepdebt/recipe/{b}.json")
    js("assets/sleepdebt/blockstates/coffee_bush.json")
    js("data/sleepdebt/loot_table/blocks/coffee_bush.json")
    for n in range(8):
        js(f"assets/sleepdebt/models/block/coffee_bush_stage_{n}.json")
    for name in names:
        if (name.startswith("assets/sleepdebt/") or name.startswith("data/sleepdebt/")) and name.endswith(".json"):
            js(name)
    pngs=[n for n in names if n.startswith("assets/sleepdebt/textures/") and n.endswith(".png")]
    if len(pngs)<17:raise ValueError(f"Expected >17 textures, found {len(pngs)}")
    for n in pngs:
        raw=data(n)
        if raw[:8]!=b"\x89PNG\r\n\x1a\n":raise ValueError(f"Invalid PNG magic: {n}")
        width,height=struct.unpack(">II",raw[16:24])
        target=((64,64) if "entity/sleepless.png" in n else
                (176,166) if "textures/gui/coffee_maker.png" in n else (16,16))
        if (width,height)!=target:raise ValueError(f"{n}: size {(width,height)} not {target}")
    for locale in ("en_us","ru_ru"):
        entries=[n for n in names if f"/patchouli_books/sleep_guide/{locale}/entries/" in n and n.endswith(".json")]
        if len(entries)<6:raise ValueError(f"{locale}: insufficient Patchouli content")
        for entry in entries:
            j=js(entry)
            for page in j.get("pages",[]):
                if "recipe" in page:
                    recipe=page["recipe"].split(":",1)[-1]
                    if f"data/sleepdebt/recipe/{recipe}.json" not in names and not page["recipe"].startswith("minecraft:"):
                        raise ValueError(f"{entry} points to missing recipe {page['recipe']}")
    print("Sleep Debt production assets OK:",len(item_ids),"items,",len(block_names),"blocks,",len(pngs),"PNG textures")



