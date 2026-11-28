"""Local/private source-art comparison against exact official vanilla PNGs.

This creates visual evidence to inspect, not an automatic aesthetic verdict.
"""
import argparse
import hashlib
import io
import json
import re
import zipfile
from collections import Counter
from pathlib import Path
from PIL import Image, ImageDraw
from .visual_lint import decode, fetch

REFERENCE = re.compile(r"(item|block|entity|models/armor)/[a-z0-9_./-]+")
MAX_SIDE = 128

def reference_path(value):
    if not REFERENCE.fullmatch(value) or any(p in ("",".","..") for p in value.split("/")):
        raise ValueError("Expected vanilla texture ID such as item/glass_bottle")
    return "assets/minecraft/textures/"+value+".png"

def official_client(version, local=None):
    manifest=json.loads(fetch("https://piston-meta.mojang.com/mc/game/version_manifest_v2.json",4_000_000))
    entry=next((x for x in manifest["versions"] if x["id"]==version and x["type"]=="release"),None)
    if entry is None:
        raise ValueError("Unknown official release: "+version)
    meta=json.loads(fetch(entry["url"],4_000_000,entry["sha1"]))
    client=meta["downloads"]["client"]
    if local is None:
        data=fetch(client["url"],min(client["size"],100_000_000),client["sha1"])
    else:
        if local.stat().st_size!=client["size"]:
            raise ValueError("Local vanilla client size mismatch")
        data=local.read_bytes()
    if len(data)!=client["size"] or hashlib.sha1(data).hexdigest()!=client["sha1"]:
        raise ValueError("Vanilla client hash/size mismatch")
    return data,client["sha1"]

def sprite(data):
    im=decode(data)
    if max(im.size)>MAX_SIDE:
        raise ValueError("Comparison supports static textures up to 128 pixels per side")
    return im

def palette(im):
    w,h=im.size
    colors=[p[:3] if p[3] else None for p in im.getdata()]
    counts=Counter(p for p in colors if p is not None)
    if not counts:
        return []
    remaining={i for i,p in enumerate(colors) if p is not None}
    components={c:[] for c in counts}
    while remaining:
        start=remaining.pop()
        color=colors[start]
        stack=[start]; size=0
        while stack:
            pos=stack.pop();size+=1
            x,y=pos%w,pos//w
            neighbors=[]
            if x: neighbors.append(pos-1)
            if x+1<w: neighbors.append(pos+1)
            if y: neighbors.append(pos-w)
            if y+1<h: neighbors.append(pos+w)
            for n in neighbors:
                if n in remaining and colors[n]==color:
                    remaining.remove(n);stack.append(n)
        components[color].append(size)
    return [{"rgb":"#%02x%02x%02x"%c,"pixels":n,
             "visible_fraction":round(n/sum(counts.values()),4),
             "components":len(components[c]),"largest_component":max(components[c])}
            for c,n in sorted(counts.items(),key=lambda x:(-x[1],x[0]))]

def make_comparison(candidate_name, candidate_data, references, version, client_sha1, out):
    if not 1<=len(references)<=4:
        raise ValueError("Select one to four actual vanilla references")
    rows=[("candidate",candidate_name,candidate_data)]+[("vanilla",name,data) for name,data in references]
    images=[sprite(data) for _,_,data in rows]
    scale=max(1,min(8,192//max(max(im.size) for im in images)))
    cell_w=max(240,max(im.width*scale for im in images)+24)
    cell_h=max(im.height*scale for im in images)+max(im.height for im in images)+112
    sheet=Image.new("RGB",(cell_w*len(rows),cell_h),(184,184,184))
    draw=ImageDraw.Draw(sheet)
    records=[]
    for i,((role,name,data),im) in enumerate(zip(rows,images)):
        x=i*cell_w
        colors=palette(im)
        # Separate native and enlarged views, using the same scale for all compared assets.
        draw.text((x+8,8),role.upper(),fill=(20,20,20))
        display=Path(name).name
        draw.text((x+8,24),display[:38],fill=(20,20,20))
        draw.text((x+8,40),f"{im.width}x{im.height} | {len(colors)} RGB colors",fill=(20,20,20))
        sheet.paste(im,(x+8,58),im)
        large=im.resize((im.width*scale,im.height*scale),Image.Resampling.NEAREST)
        top=62+max(z.height for z in images)
        sheet.paste(large,(x+8,top),large)
        y=top+max(z.height*scale for z in images)+8
        for j,c in enumerate(colors[:24]):
            sx=x+8+(j%12)*18;sy=y+(j//12)*18
            draw.rectangle((sx,sy,sx+15,sy+15),fill=c["rgb"])
        records.append({"role":role,"path":name,"sha256":hashlib.sha256(data).hexdigest(),
                        "width":im.width,"height":im.height,"palette":colors})
    out.mkdir(parents=True,exist_ok=True)
    sheet.save(out/"comparison.png")
    report={"schema":1,"minecraft":version,"official_client_sha1":client_sha1,
            "aesthetic_review":"NOT_RUN","client_rendering":"NOT_RUN","scale":scale,
            "scope":"Private static image comparison. Counts are evidence, not art grades. Inspect actual shape, material and cluster transitions.",
            "assets":records}
    (out/"comparison.json").write_text(json.dumps(report,indent=2)+"\n",encoding="utf-8")
    return report

def main():
    ap=argparse.ArgumentParser(description=__doc__)
    ap.add_argument("--minecraft",required=True)
    ap.add_argument("--candidate",required=True,type=Path)
    ap.add_argument("--reference",required=True,action="append")
    ap.add_argument("--client",type=Path)
    ap.add_argument("--out",required=True,type=Path)
    a=ap.parse_args()
    if not 1<=len(a.reference)<=4 or len(set(a.reference))!=len(a.reference):
        raise ValueError("Select one to four distinct vanilla references")
    paths=[reference_path(x) for x in a.reference]
    client,sha=official_client(a.minecraft,a.client)
    with zipfile.ZipFile(io.BytesIO(client)) as z:
        refs=[]
        for path in paths:
            if path+".mcmeta" in z.namelist():
                raise ValueError("Animated/metadata texture not supported: "+path)
            refs.append((path,z.read(path)))
    if a.candidate.with_name(a.candidate.name+".mcmeta").is_file():
        raise ValueError("Animated/metadata candidate not supported")
    make_comparison(a.candidate.as_posix(),a.candidate.read_bytes(),refs,a.minecraft,sha,a.out)
    print("Private comparison ready; aesthetic review NOT_RUN, client rendering NOT_RUN.")

if __name__=="__main__":
    main()
