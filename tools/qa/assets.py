"""Inspect final JAR models, textures, blockstates and sound references without inventing external assets."""
import argparse
import json
import re
import struct
import zipfile
from pathlib import Path
from .common import write_json, require
ID=re.compile(r"^[a-z0-9_.-]+:[a-z0-9_./-]+$")

def inspect(jar):
    errors=[];warnings=[];counts={"models":0,"textures":0,"blockstates":0,"sounds":0}
    with zipfile.ZipFile(jar) as z:
        names=set(z.namelist())
        namespaces={n.split("/")[1] for n in names if n.startswith("assets/") and len(n.split("/"))>2}
        docs={}
        for n in sorted(names):
            if n.startswith("assets/") and n.endswith(".json"):
                try:docs[n]=json.loads(z.read(n))
                except (ValueError,UnicodeError):errors.append({"file":n,"reason":"Malformed JSON"})
            if n.startswith("assets/") and n.endswith(".png"):
                counts["textures"]+=1;b=z.read(n)
                if len(b)<33 or b[:8]!=b"\x89PNG\r\n\x1a\n" or b[12:16]!=b"IHDR":
                    errors.append({"file":n,"reason":"Invalid PNG header"});continue
                w,h=struct.unpack(">II",b[16:24])
                if not w or not h:errors.append({"file":n,"reason":"Zero texture dimensions"})
                if max(w,h)>4096:warnings.append({"file":n,"reason":"Texture exceeds 4096 pixels"})
                if n.endswith("/.png"):errors.append({"file":n,"reason":"Empty texture ID"})
        def ref(n,value,kind,ns):
            if not isinstance(value,str) or value.startswith("#"):return
            rid=value if ":" in value else "minecraft:"+value
            if not ID.fullmatch(rid):
                errors.append({"file":n,"reason":"Invalid "+kind+" ID: "+value});return
            space,path=rid.split(":",1)
            folder,ext={"model":("models",".json"),"texture":("textures",".png"),"sound":("sounds",".ogg")}[kind]
            dest=f"assets/{space}/{folder}/{path}{ext}"
            if dest not in names and space in namespaces and space!="minecraft":
                errors.append({"file":n,"reason":"Missing local "+kind+": "+dest})
            elif dest not in names and space!="minecraft":
                warnings.append({"file":n,"reason":"External dependency not checked: "+dest})
        def variants(n,obj,ns):
            if isinstance(obj,dict):
                if "model" in obj:ref(n,obj["model"],"model",ns)
                for v in obj.values():variants(n,v,ns)
            elif isinstance(obj,list):
                for v in obj:variants(n,v,ns)
        for n,d in docs.items():
            ns=n.split("/")[1]
            if "/models/" in n and isinstance(d,dict):
                counts["models"]+=1
                ref(n,d.get("parent"),"model",ns)
                for v in d.get("textures",{}).values():ref(n,v,"texture",ns)
                for elem in d.get("elements",[]):
                    if not isinstance(elem,dict):continue
                    for face in elem.get("faces",{}).values():
                        if not isinstance(face,dict):continue
                        uv=face.get("uv")
                        if uv is not None:
                            if not isinstance(uv,list) or len(uv)!=4 or any(not isinstance(x,(int,float)) for x in uv):
                                errors.append({"file":n,"reason":"Invalid face UV (requires four numbers)"})
                            elif any(x<0 or x>16 for x in uv):warnings.append({"file":n,"reason":"UV outside 0..16; review intentional tiling"})
                        ref(n,face.get("texture"),"texture",ns)
            elif "/blockstates/" in n:
                counts["blockstates"]+=1;variants(n,d,ns)
            elif "/items/" in n:
                variants(n,d,ns)
            elif n.endswith("/sounds.json") and isinstance(d,dict):
                for event,body in d.items():
                    if not isinstance(body,dict):continue
                    for s in body.get("sounds",[]):
                        counts["sounds"]+=1
                        if isinstance(s,dict) and s.get("type")=="event":continue
                        ref(n,s if isinstance(s,str) else s.get("name") if isinstance(s,dict) else None,"sound",ns)
    return {"schema":1,"jar":Path(jar).name,"counts":counts,"errors":errors,"warnings":warnings,"scope":"local asset links and basic geometry/PNG headers; no visual-quality judgment"}

if __name__=="__main__":
    ap=argparse.ArgumentParser();ap.add_argument("jar");ap.add_argument("--out",required=True);ap.add_argument("--strict",action="store_true")
    a=ap.parse_args();r=inspect(a.jar);write_json(a.out,r);print("Assets:",r["counts"],"errors",len(r["errors"]),"warnings",len(r["warnings"]))
    if a.strict:require(not r["errors"],"Local asset validation failed")
