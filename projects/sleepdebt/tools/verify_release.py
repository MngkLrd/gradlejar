#!/usr/bin/env python3
"""Fail the CI release for missing item models, PNGs, guide entries, broken recipes
or missing optional/required module declarations. Requires only Python stdlib."""
from __future__ import annotations
import json
import struct
import sys
import zipfile
import tomllib
import zlib
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
    if not {"neoforge","minecraft"}.issubset(dep_names):
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
    for img in ("sleep_journal.png","sleep_hud.png","coffee_maker.png"):
        data("assets/sleepdebt/textures/gui/"+img)
    for name in names:
        if (name.startswith("assets/sleepdebt/") or name.startswith("data/sleepdebt/")) and name.endswith(".json"):
            js(name)
    pngs=[n for n in names if n.startswith("assets/sleepdebt/textures/") and n.endswith(".png")]
    if len(pngs)<17:raise ValueError(f"Expected >17 textures, found {len(pngs)}")
    for n in pngs:
        raw=data(n)
        if raw[:8]!=b"\x89PNG\r\n\x1a\n":raise ValueError(f"Invalid PNG magic: {n}")
        width,height=struct.unpack(">II",raw[16:24])
        target=((256,128) if "entity/sleepless.png" in n else
                (176,166) if "textures/gui/coffee_maker.png" in n else
                (228,188) if "textures/gui/sleep_journal.png" in n else
                (158,68) if "textures/gui/sleep_hud.png" in n else (16,16))
        if (width,height)!=target:raise ValueError(f"{n}: size {(width,height)} not {target}")

        # Parse PNG chunks rather than trusting the ZIP CRC or just the PNG header.
        cursor=8
        image_data=bytearray()
        while cursor<len(raw):
            length=struct.unpack(">I",raw[cursor:cursor+4])[0]
            typ=raw[cursor+4:cursor+8]
            payload=raw[cursor+8:cursor+8+length]
            stored=struct.unpack(">I",raw[cursor+8+length:cursor+12+length])[0]
            if zlib.crc32(typ+payload)&0xffffffff!=stored:
                raise ValueError(f"PNG chunk CRC mismatch: {n}")
            if typ==b"IDAT":image_data.extend(payload)
            cursor+=12+length
            if typ==b"IEND":break
        if cursor!=len(raw):raise ValueError(f"Invalid trailing bytes in PNG: {n}")
        if n.endswith("entity/sleepless.png"):
            rawpixels=zlib.decompress(image_data)
            stride=256*4
            if len(rawpixels)!=(stride+1)*128:
                raise ValueError("Sleepless atlas has invalid raw RGBA payload")
            # Each UV box from SleeplessModel, including front/back, top/bottom and sides.
            # Distinct real UVs in the new anatomically distorted 256x128 mesh.
            parts=[(0,0,10,15,10),(112,0,9,7,2),
                   (142,0,3,8,2),(158,0,2,8,2),(173,0,2,10,2),
                   (0,50,7,16,5),(58,0,4,20,4),(82,0,4,18,4),
                   (48,52,3,14,3),(70,52,3,14,3),
                   (70,73,2,11,3),(82,73,2,10,3)]
            for u,v,w,h,d in parts:
                faces=[(u,v+d,d,h),(u+d,v+d,w,h),(u+d+w,v+d,d,h),
                       (u+2*d+w,v+d,w,h),(u+d,v,w,d),(u+d+w,v,w,d)]
                for x,y,fw,fh in faces:
                    if x<0 or y<0 or x+fw>256 or y+fh>128:
                        raise ValueError("Sleepless UV outside atlas")
                    for py in range(y,y+fh):
                        if rawpixels[py*(stride+1)]!=0:
                            raise ValueError("Sleepless UV audit expects PNG filter type 0")
                        for px in range(x,x+fw):
                            if rawpixels[py*(stride+1)+1+px*4+3]<240:
                                raise ValueError(f"Transparent Sleepless UV at {px},{py}")

    sounds=js("assets/sleepdebt/sounds.json")
    for cue in ("breath","whisper","growl","strike","heartbeat","rattle","screech","skitter"):
        key="sleepless."+cue
        if key not in sounds:raise ValueError("Missing custom Sleepless sound "+key)
        ogg=data("assets/sleepdebt/sounds/sleepless/"+cue+".ogg")
        if not ogg.startswith(b"OggS") or len(ogg)<1000:
            raise ValueError("Invalid Vorbis OGG "+key)
    # Validate all block model geometry and references to the source texture atlas.
    for model_path in sorted(n for n in names
                             if n.startswith("assets/sleepdebt/models/block/")
                             and n.endswith(".json")):
        item=js(model_path)
        textures=item.get("textures",{})
        for alias,ref in textures.items():
            if ref.startswith("sleepdebt:"):
                data("assets/sleepdebt/textures/"+ref.split(":",1)[1]+".png")
        for piece in item.get("elements",[]):
            frm=piece["from"];to=piece["to"]
            if any(not (0<=a<b<=16) for a,b in zip(frm,to)):
                raise ValueError(f"Invalid cuboid dimensions: {model_path}")
            for side,face in piece["faces"].items():
                token=face.get("texture","")
                if token.startswith("#") and token[1:] not in textures:
                    raise ValueError(f"Unknown texture alias {token} in {model_path}")
                uv=face.get("uv")
                if uv is not None and (len(uv)!=4 or
                       any(not 0<=float(value)<=16 for value in uv)):
                    raise ValueError(f"Invalid UV range in {model_path}/{side}")

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

# Additional authored pixel-art quality gate, independently decodes PNG pixels.
import subprocess
subprocess.run([sys.executable,str(Path(__file__).resolve().parent/"verify_pixel_art.py"),str(jar)],check=True)
