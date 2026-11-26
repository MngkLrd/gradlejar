"""Static verification of exact production JAR contents for enrolled projects."""
import json
import re
import struct
import tomllib
import zipfile
from pathlib import Path
from .common import compatibility, require, sha256, filename

def pair(x):
    if isinstance(x,int): return (x,0)
    require(isinstance(x,list) and len(x)==2 and all(isinstance(i,int) for i in x),"Invalid format number")
    return tuple(x)

def pack_check(pack, mc, names):
    info=pack.get("pack",{})
    has_data=any(n.startswith("data/") and not n.endswith("/") for n in names)
    has_assets=any(n.startswith("assets/") and not n.endswith("/") for n in names)
    needed=[tuple(mc["resource"])] if has_assets or not has_data else []
    if has_data: needed.append(tuple(mc["data"]))
    if "min_format" in info and "max_format" in info:
        lo,hi=pair(info["min_format"]),pair(info["max_format"])
        require(lo<=hi and all(lo<=x<=hi for x in needed),"pack.mcmeta excludes required resource/data pack version(s)")
    else:
        require(mc["java"]<25,"26.x requires explicit min_format/max_format")
        # Legacy mod loaders often provide separate virtual pack metadata.
        require(info.get("pack_format") in (mc["resource"][0],mc["data"][0]),"Unexpected legacy pack format")

def validate_jar(jar,p):
    jar=Path(jar)
    mc,_=compatibility(p)
    require(jar.name==filename(p),"Incorrect standard JAR filename")
    with zipfile.ZipFile(jar) as z:
        require(z.testzip() is None,"Corrupt JAR")
        names=z.namelist()
        require(len(names)==len(set(names)),"Duplicate JAR entry")
        require(all(not n.startswith("/") and ".." not in n.split("/") for n in names),"Unsafe JAR entry")
        require(not any(n.startswith(("net/minecraft/","net/neoforged/","net/fabricmc/","net/minecraftforge/")) and n.endswith(".class") for n in names),"Bundled game/loader classes")
        classes=[n for n in names if n.endswith(".class")]
        require(classes,"No compiled classes")
        for n in classes:
            b=z.read(n)[:8]
            require(len(b)==8 and b[:4]==b"\xca\xfe\xba\xbe","Invalid class header: "+n)
            require(struct.unpack(">H",b[6:8])[0] <= mc["java"]+44,"Class newer than runtime Java: "+n)
        docs={}
        for n in names:
            if n.endswith((".json",".mcmeta")):
                try: docs[n]=json.loads(z.read(n).decode("utf-8-sig"))
                except (UnicodeDecodeError,ValueError) as e: raise ValueError("Malformed JSON "+n+": "+str(e)) from e
        require("pack.mcmeta" in docs,"Missing pack.mcmeta")
        pack_check(docs["pack.mcmeta"],mc,names)
        if p["loader"]=="fabric":
            meta=docs.get("fabric.mod.json",{})
            require(meta.get("id")==p["slug"] and meta.get("version")==p["version"],"Fabric metadata ID/version mismatch")
            require("minecraft" in meta.get("depends",{}) and "fabricloader" in meta.get("depends",{}),"Missing Fabric loader/Minecraft dependency")
            require(not (p.get("qa",{}).get("mode")=="server" and meta.get("environment")=="client"),"Client-only mod cannot use server QA")
            for mix in meta.get("mixins",[]):
                path=mix if isinstance(mix,str) else mix.get("config","")
                require(path in docs,"Missing mixin configuration "+path)
        else:
            key="META-INF/mods.toml" if p["loader"]=="forge" or p["minecraft"]=="1.20.1" else "META-INF/neoforge.mods.toml"
            require(key in names,"Missing "+key)
            meta=tomllib.loads(z.read(key).decode("utf-8-sig"))
            require(any(m.get("modId")==p["slug"] and m.get("version")==p["version"] for m in meta.get("mods",[])),"Forge/NeoForge metadata ID/version mismatch")
        singular={"recipe","loot_table","advancement","structure","function","predicate","item_modifier","block","item","entity_type","fluid","game_event","dimension","dimension_type"}
        plural={"recipes","loot_tables","advancements","structures","functions","predicates","item_modifiers","blocks","items","entity_types","fluids","game_events","dimensions","dimension_types"}
        for n in names:
            parts=n.split("/")
            if len(parts)<4 or parts[0]!="data" or n.endswith("/"): continue
            folder=parts[3] if parts[2]=="tags" and len(parts)>=5 else parts[2]
            wrong=plural if mc["data_layout"]=="singular" else singular
            # dimension and dimension_type have always been singular; no generic mod registry guesses.
            if parts[2]=="tags":
                wrong=({"blocks","items","entity_types","fluids","game_events"} if mc["data_layout"]=="singular" else {"block","item","entity_type","fluid","game_event"})
            else: wrong=wrong-{"block","item","entity_type","fluid","game_event","dimension","dimension_type","dimensions","dimension_types","blocks","items","entity_types","fluids","game_events"}
            require(folder not in wrong,"Wrong version-specific data directory: "+n)
            if parts[2] in ("recipe","recipes") and n.endswith(".json"):
                require(isinstance(docs[n],dict) and isinstance(docs[n].get("type"),str),"Recipe missing type: "+n)
            if parts[2]=="tags" and n.endswith(".json"):
                require(isinstance(docs[n],dict) and isinstance(docs[n].get("values"),list),"Invalid tag: "+n)
    return {"classes":len(classes),"json":len(docs),"sha256":sha256(jar)}
