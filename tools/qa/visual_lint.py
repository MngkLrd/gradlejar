"""Fast advisory vanilla-style measurements. No client rendering or art certification."""
import argparse
import colorsys
import hashlib
import io
import json
import math
import re
import statistics
import subprocess
import urllib.request
import zipfile
from collections import Counter
from pathlib import Path
from PIL import Image, ImageFile

ROOT = Path(__file__).resolve().parents[2]
Image.MAX_IMAGE_PIXELS = 4_194_304
ImageFile.LOAD_TRUNCATED_IMAGES = False
TEXTURE = re.compile(r"(?:^|/)assets/[^/]+/textures/(item|block|entity)s?/.+\.png$")
MODEL = re.compile(r"(?:^|/)assets/[^/]+/models/.+\.json$")
MAX_PREVIEWS = 32
PILOT = [
    "projects/bearback-1.20.1/src/main/resources/assets/bearback/textures/item/saddlebags.png",
    "projects/creeper-armor-1.20.1/src/main/resources/assets/creeperarmor/textures/item/creeper_powder.png",
    "projects/lurelantern-1.3.7/src/main/resources/assets/lurelantern/textures/block/lure_lantern.png",
    "projects/bearback-1.20.1/src/main/resources/assets/bearback/textures/entity/bear_saddle.png",
]
VANILLA = {
    "item": ["iron_ingot", "iron_sword", "stick", "leather", "apple", "brick", "diamond", "paper", "gold_ingot"],
    "block": ["oak_planks", "stone", "bricks", "iron_block", "glass", "white_wool", "dirt"],
    "entity": ["cow/cow", "pig/pig", "zombie/zombie", "creeper/creeper", "skeleton/skeleton"],
}

def texture_kind(path):
    m = TEXTURE.search(path)
    return m.group(1) if m else None

def decode(data):
    with Image.open(io.BytesIO(data)) as im:
        if im.format != "PNG":
            raise ValueError("Expected PNG")
        if im.width * im.height > Image.MAX_IMAGE_PIXELS:
            raise ValueError("Texture exceeds analysis pixel budget")
        im.load()
        return im.convert("RGBA")

def stats(im):
    w, h = im.size
    px = list(im.getdata())
    colors = [p[:3] if p[3] else None for p in px]
    visible = sum(p is not None for p in colors)
    if not visible:
        return {"width": w, "height": h, "visible_pixels": 0}
    palette = Counter(p for p in colors if p is not None)
    visited = bytearray(len(px))
    sizes = []
    def adjacent(i):
        x, y = i % w, i // w
        if x: yield i - 1
        if x + 1 < w: yield i + 1
        if y: yield i - w
        if y + 1 < h: yield i + w
    for i, c in enumerate(colors):
        if c is None or visited[i]:
            continue
        visited[i] = 1
        pending = [i]
        count = 0
        while pending:
            j = pending.pop()
            count += 1
            for k in adjacent(j):
                if not visited[k] and colors[k] == c:
                    visited[k] = 1
                    pending.append(k)
        sizes.append(count)
    edges = Counter()
    for i, c in enumerate(colors):
        if c is None:
            continue
        for j in adjacent(i):
            other = colors[j]
            if j > i and other is not None:
                edges[tuple(sorted((c, other)))] += 1
    total_edges = sum(edges.values())
    hsv = {c: colorsys.rgb_to_hsv(*(v / 255 for v in c)) for c in palette}
    deltas = []
    for (a, b), n in edges.items():
        if a == b:
            continue
        ah, ass, av = hsv[a]
        bh, bs, bv = hsv[b]
        hue = min(abs(ah - bh), 1 - abs(ah - bh)) * 360
        # Hue is unstable for near-neutral pixels. Do not treat it as meaningful there.
        deltas.append((hue if min(ass, bs) >= .08 else None, abs(ass-bs), abs(av-bv), n))
    def weighted_mean(idx):
        values = [(d[idx], d[3]) for d in deltas if d[idx] is not None]
        return round(sum(a*n for a,n in values)/sum(n for _,n in values), 4) if values else None
    checker = quads = 0
    for y in range(h - 1):
        for x in range(w - 1):
            i = y*w+x
            a,b,c,d = colors[i],colors[i+1],colors[i+w],colors[i+w+1]
            if None in (a,b,c,d):
                continue
            quads += 1
            checker += a == d and b == c and a != b
    return {
        "width":w,"height":h,"visible_pixels":visible,"palette_colors":len(palette),
        "partial_alpha_pixels":sum(0<p[3]<255 for p in px),
        "clusters_4_connected":len(sizes),"median_cluster_area":statistics.median(sizes),
        "largest_cluster_area":max(sizes),
        "singleton_pixel_fraction":round(sizes.count(1)/visible,4),
        "small_cluster_pixel_fraction":round(sum(s for s in sizes if s<=3)/visible,4),
        "same_color_edge_fraction":round(sum(n for (a,b),n in edges.items() if a==b)/max(1,total_edges),4),
        "checkerboard_quad_fraction":round(checker/max(1,quads),4),
        "adjacent_mean_hue_delta_deg":weighted_mean(0),
        "adjacent_mean_saturation_delta":weighted_mean(1),
        "adjacent_mean_value_delta":weighted_mean(2),
    }

def style_notes(kind, m, animated=False):
    if not m["visible_pixels"]:
        return ["Fully transparent: review whether intentional."]
    if animated:
        return ["Animation atlas: aggregate pixel metrics only; inspect individual frames separately."]
    notes = []
    if kind == "item" and (m["width"],m["height"]) != (16,16):
        notes.append("Non-16x16 item texture: review icon/model scale; larger atlases may be intentional.")
    if m["partial_alpha_pixels"]:
        notes.append("Partial alpha: review intended translucency versus accidental smoothing.")
    if m["visible_pixels"] >= 32 and m["singleton_pixel_fraction"] > .45:
        notes.append("Many isolated exact-color pixels: inspect noise and material structure.")
    if m["checkerboard_quad_fraction"] > .15:
        notes.append("Frequent alternating 2x2 colors: inspect unnecessary dithering.")
    return notes

def previews(im, kind, output, animated=False):
    output.mkdir(parents=True, exist_ok=True)
    im.save(output/"native.png")
    if max(im.size) <= 128:
        im.resize((im.width*4, im.height*4),Image.Resampling.NEAREST).save(output/"nearest-4x.png")
    if kind == "block" and not animated and max(im.size) <= 128:
        wall=Image.new("RGBA",(im.width*3,im.height*3))
        for x in range(3):
            for y in range(3):
                wall.paste(im,(x*im.width,y*im.height))
        wall.save(output/"tiling-3x3.png")

def model_metrics(data, name, read):
    doc=json.loads(data)
    if not isinstance(doc,dict):
        raise ValueError("Expected model JSON object")
    errors=[];notes=[]
    elems=doc.get("elements",[])
    if not isinstance(elems,list):
        raise ValueError("Model elements must be a list")
    tex=doc.get("textures",{})
    root,ns=name.rsplit("assets/",1)
    ns=ns.split("/",1)[0]
    def texture_size(ref):
        seen=set()
        while isinstance(ref,str) and ref.startswith("#"):
            key=ref[1:]
            if key in seen: return None
            seen.add(key)
            ref=tex.get(key) if isinstance(tex,dict) else None
        if not isinstance(ref,str): return None
        space,path=ref.split(":",1) if ":" in ref else ("minecraft",ref)
        if not re.fullmatch(r"[a-z0-9_.-]+",space) or not re.fullmatch(r"[a-z0-9_./-]+",path) or ".." in path.split("/"):
            return None
        resource=root+"assets/"+space+"/textures/"+path+".png"
        try:
            if read(resource+".mcmeta") is not None: return None
            b=read(resource)
            return decode(b).size if b is not None else None
        except (OSError,ValueError,Image.DecompressionBombError): return None
    stretched=thin=0
    size_cache={}
    for e in elems:
        if not isinstance(e,dict): continue
        frm,to=e.get("from"),e.get("to")
        def numeric(v,n):
            return isinstance(v,list) and len(v)==n and all(type(x) in (float,int) and math.isfinite(x) for x in v)
        dims=[abs(b-a) for a,b in zip(frm,to)] if numeric(frm,3) and numeric(to,3) else None
        if dims and any(0<d<1 for d in dims):
            thin+=1
        faces=e.get("faces",{})
        if not isinstance(faces,dict): continue
        for direction,face in faces.items():
            if not isinstance(face,dict): continue
            uv=face.get("uv")
            if uv is None: continue
            if not numeric(uv,4):
                errors.append("Invalid explicit UV coordinates")
                continue
            axes={"north":(0,1),"south":(0,1),"east":(2,1),"west":(2,1),"up":(0,2),"down":(0,2)}.get(direction)
            ref=face.get("texture")
            if not dims or axes is None or not isinstance(ref,str): continue
            if ref not in size_cache: size_cache[ref]=texture_size(ref)
            size=size_cache[ref]
            if not size: continue
            a,b=dims[axes[0]],dims[axes[1]]
            u=abs(uv[2]-uv[0])*size[0]/16
            v=abs(uv[3]-uv[1])*size[1]/16
            if face.get("rotation") in (90,270): u,v=v,u
            if min(a,b,u,v)<=0: continue
            ratio=(u/a)/(v/b)
            if ratio>1.25 or ratio<.8: stretched+=1
    if stretched: notes.append(f"{stretched} face(s) with unequal estimated texel density: inspect UV stretch; intentional mappings possible.")
    if thin: notes.append(f"{thin} sub-pixel element(s): inspect model mixels; intentional thin geometry possible.")
    return {"elements":len(elems),"estimated_stretched_faces":stretched,"subpixel_elements":thin,
            "errors":errors,"warnings":notes,"scope":"explicit local JSON UV only; inherited models and code/entity meshes not resolved"}

def fetch(url, max_bytes, sha1=None):
    if not url.startswith("https://"):
        raise ValueError("Expected HTTPS")
    with urllib.request.urlopen(url,timeout=35) as response:
        data=response.read(max_bytes+1)
    if len(data)>max_bytes or (sha1 and hashlib.sha1(data).hexdigest()!=sha1):
        raise ValueError("Vanilla download size/hash mismatch")
    return data

def vanilla_reference(version):
    manifest=json.loads(fetch("https://piston-meta.mojang.com/mc/game/version_manifest_v2.json",4_000_000))
    entry=next(x for x in manifest["versions"] if x["id"]==version and x["type"]=="release")
    meta=json.loads(fetch(entry["url"],4_000_000,entry["sha1"]))
    client=meta["downloads"]["client"]
    data=fetch(client["url"],min(client["size"],100_000_000),client["sha1"])
    if len(data)!=client["size"]:
        raise ValueError("Vanilla client size mismatch")
    rows=[]
    with zipfile.ZipFile(io.BytesIO(data)) as z:
        names=set(z.namelist())
        for kind, samples in VANILLA.items():
            for sample in samples:
                path=f"assets/minecraft/textures/{kind}/{sample}.png"
                if path in names:
                    rows.append({"kind":kind,"name":path,"metrics":stats(decode(z.read(path)))})
    if not all(any(r["kind"]==k for r in rows) for k in VANILLA):
        raise ValueError("Vanilla reference missing an asset category")
    return {"schema":1,"minecraft":version,"client_sha1":client["sha1"],"samples":rows,
            "scope":"selected official vanilla examples, not mandatory thresholds"}

def reference_ranges(ref, kind, dimensions):
    if not ref: return None
    rows=[r["metrics"] for r in ref["samples"] if r["kind"]==kind and
          (r["metrics"]["width"],r["metrics"]["height"])==dimensions]
    if not rows: return None
    keys=["palette_colors","clusters_4_connected","singleton_pixel_fraction","same_color_edge_fraction","adjacent_mean_value_delta"]
    return {"samples":len(rows),"ranges":{k:[min(v),max(v)] for k in keys
            if (v:=[r[k] for r in rows if r.get(k) is not None])}}

def inspect(names, read, out, ref=None):
    result={"schema":1,"scope":"source/final-JAR texture metrics and local model lint; not rendered Minecraft or art certification",
            "reference":({"minecraft":ref["minecraft"],"client_sha1":ref["client_sha1"],"samples":len(ref["samples"])} if ref else None),
            "textures":[],"models":[],"errors":[],"warnings":[]}
    cache={}
    preview_count=0
    for name in sorted(set(names)):
        kind=texture_kind(name)
        try:
            if kind:
                data=read(name)
                if data is None: continue
                animated=read(name+".mcmeta") is not None
                key=(hashlib.sha256(data).hexdigest(),kind,animated)
                if key not in cache:
                    im=decode(data);m=stats(im);notes=style_notes(kind,m,animated)
                    preview=None
                    if preview_count<MAX_PREVIEWS:
                        preview="previews/"+str(preview_count+1).zfill(3)
                        previews(im,kind,out/preview,animated);preview_count+=1
                    cache[key]=(m,notes,preview)
                m,notes,preview=cache[key]
                result["textures"].append({"file":name,"kind":kind,"sha256":key[0],"metrics":m,"preview":preview,
                    "animated_or_metadata":animated,"warnings":notes,
                    "vanilla_context":reference_ranges(ref,kind,(m["width"],m["height"])) if not animated else None})
                result["warnings"].extend({"file":name,"reason":x} for x in notes)
            elif MODEL.search(name):
                data=read(name)
                if data is None: continue
                row=model_metrics(data,name,read)
                result["models"].append(dict(row,file=name))
                result["errors"].extend({"file":name,"reason":x} for x in row["errors"])
                result["warnings"].extend({"file":name,"reason":x} for x in row["warnings"])
        except (OSError,ValueError,TypeError,OverflowError,Image.DecompressionBombError) as e:
            result["errors"].append({"file":name,"reason":str(e)})
    result["counts"]={"textures":len(result["textures"]),"unique_textures":len(cache),"models":len(result["models"]),
                      "errors":len(result["errors"]),"warnings":len(result["warnings"]),"previews":preview_count}
    result["status"]="failed" if result["errors"] else ("review" if result["warnings"] else ("measured" if result["textures"] or result["models"] else "not-applicable"))
    return result

def changed(base):
    if not re.fullmatch(r"[0-9a-f]{40}",base):
        raise ValueError("Expected base commit SHA")
    raw=subprocess.check_output(["git","diff","--name-only","--diff-filter=ACMR","-z",base,"HEAD"],cwd=ROOT)
    paths=raw.decode().split("\0")
    selected=[p[:-7] if p.endswith(".png.mcmeta") else p for p in paths]
    if any(p.startswith("tools/qa/visual_lint") or p.startswith("tools/qa/tests/test_visual_lint") or p==".github/workflows/qa-visual.yml" for p in paths):
        selected+=PILOT
    return selected

def main():
    ap=argparse.ArgumentParser()
    ap.add_argument("--out",type=Path,required=True)
    ap.add_argument("--base")
    ap.add_argument("--jar",type=Path)
    ap.add_argument("--paths",nargs="*",default=[])
    ap.add_argument("--reference",type=Path)
    ap.add_argument("--make-vanilla-reference")
    a=ap.parse_args()
    if a.make_vanilla_reference:
        a.out.parent.mkdir(parents=True,exist_ok=True)
        a.out.write_text(json.dumps(vanilla_reference(a.make_vanilla_reference),indent=2)+"\n")
        return 0
    ref=json.loads(a.reference.read_text()) if a.reference and a.reference.is_file() else None
    a.out.mkdir(parents=True,exist_ok=True)
    if a.jar:
        with zipfile.ZipFile(a.jar) as z:
            names=set(z.namelist())
            r=inspect(names,lambda n:z.read(n) if n in names else None,a.out,ref)
        r["jar_sha256"]=hashlib.file_digest(a.jar.open("rb"),"sha256").hexdigest()
    else:
        def read(n):
            p=(ROOT/n).resolve()
            if not p.is_relative_to(ROOT) or not p.is_file(): return None
            return p.read_bytes()
        names=a.paths+(changed(a.base) if a.base else [])
        r=inspect(names,read,a.out,ref)
    (a.out/"visual-report.json").write_text(json.dumps(r,indent=2,ensure_ascii=False)+"\n")
    c=r["counts"]
    summary=f"Visual QA: {r['status']} | {c['textures']} textures, {c['models']} models, {c['errors']} errors, {c['warnings']} advisory warnings. Native PNG metrics, no in-game rendering."
    print(summary)
    import os
    if os.getenv("GITHUB_STEP_SUMMARY"):
        with open(os.environ["GITHUB_STEP_SUMMARY"],"a") as f: f.write(summary+"\n")
    return int(bool(r["errors"]))

if __name__=="__main__":
    raise SystemExit(main())
