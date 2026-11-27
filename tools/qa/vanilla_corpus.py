"""Inventory official Java client PNGs; measure supported static images, not art grades."""
import argparse
import hashlib
import io
import json
import statistics
import zipfile
from collections import Counter, defaultdict
from pathlib import Path
from .art_compare import official_client
from .visual_lint import decode, stats

PREFIX="assets/minecraft/textures/"
PILOT={
"block/spruce_log":"b95fe0dc8f1760d26dc6cbaa2241d2e0eb85296c",
"block/spruce_log_top":"6b4908530fb29e8396626e8337e2580e81004552",
"block/spruce_leaves":"a243dac85c0f3eff5460aa9ecec03d1a2879a413",
"block/oak_leaves":"0f1793d68df14a8551ca59d4c428bba24acb9c50",
"block/oak_log":"9a6c5fdd0e973e90ad766b4633e4ef4e0dabaf61",
"block/oak_log_top":"74530e611040746765a12803a2925d4fe02c0cf6",
"block/dirt":"88525fa26791a5d2446373a38ec8a63d8053ed91",
"block/stone":"de078145f71d63196633e6061e7d007b5b7a79e4",
"block/iron_ore":"3d75fcd6c136aa75b4b9e381afeb0776feca0a0a",
"item/leather_chestplate":"104c1feb3e77641096783d2a52538a0a86c45f6f",
"block/spruce_sapling":"fdaa1493a8fef508e4ed70968c0e0bef4a9c0231",
"block/birch_log":"9b9c2df4639489d58b7b4fcf7c469ee998cb4359",
"block/jungle_log":"e1e5e12ae50cac33a2597a882d5e634d4c705a22",
"block/acacia_log":"012ee84830b305003b9318e98570a3b1f6f83d11",
"block/dark_oak_log":"8562ff7226479812ef22ddf43f3eb7e1d59e6c31",
"block/birch_leaves":"b29f8c01e47ce5db20fcb7f226b920ea9b057ed1",
"block/acacia_leaves":"81c370267c6bcff3f458d612ea4ef72c0838e5e3",
"block/jungle_sapling":"8632359457d3a245220bee351716a7b1c9267bfc",
"block/birch_sapling":"9b8193943a7dfd61a2752c82189e9b0d02f95497",
}

def git_blob(data):
    return hashlib.sha1(f"blob {len(data)}\0".encode()+data).hexdigest()

def extra_metrics(im):
    w,h=im.size
    px=[p[:3] if p[3] else None for p in im.getdata()]
    labels={}
    pattern=[]
    for c in px:
        if c is None:pattern.append(0)
        else:
            if c not in labels:labels[c]=len(labels)+1
            pattern.append(labels[c])
    # Canonical color labels preserve equality structure, not the colors themselves.
    signature=hashlib.sha256(json.dumps([w,h,pattern],separators=(",",":")).encode()).hexdigest()
    adjacent=Counter()
    directions=Counter()
    for y in range(h):
        for x in range(w):
            i=y*w+x;c=px[i]
            if c is None:continue
            for axis,j in (("x",i+1 if x+1<w else None),("y",i+w if y+1<h else None)):
                if j is None or px[j] is None:continue
                other=px[j]
                directions[axis,c==other]+=1
                if c!=other:adjacent[tuple(sorted((c,other)))]+=1
    result={"layout_sha256":signature,"distinct_neighbor_pairs":len(adjacent)}
    for axis in ("x","y"):
        n=directions[axis,True]+directions[axis,False]
        result["same_color_"+axis]=round(directions[axis,True]/n,4) if n else None
    return result

def family(path):
    name=path.rsplit("/",1)[-1].removesuffix(".png")
    if path.startswith("block/"):
        if name.endswith(("_log_top","_stem_top")):return "log_end_hint"
        if name.endswith(("_log","_stem")) and not name.startswith("stripped_"):return "bark_hint"
        if name.endswith("_leaves"):return "foliage_hint"
        if name.endswith("_sapling"):return "sapling_hint"
        if name.endswith("_ore"):return "ore_hint"
        if name in ("dirt","coarse_dirt","rooted_dirt","mud","grass_block_top"):return "soil_hint"
    if path.startswith("item/") and name.endswith(("_helmet","_chestplate","_leggings","_boots")):return "armor_icon_hint"
    return path.split("/",1)[0]+"_other"

def inventory(z):
    names=set(z.namelist());rows=[]
    for path in sorted(n for n in names if n.startswith(PREFIX) and n.endswith(".png")):
        relative=path[len(PREFIX):]
        data=z.read(path)
        row={"path":relative,"category":relative.split("/",1)[0],"family_hint":family(relative),
             "sha256":hashlib.sha256(data).hexdigest(),"git_blob_sha1":git_blob(data),
             "metadata_present":path+".mcmeta" in names,"visual_review":"NOT_RUN"}
        try:
            im=decode(data);row.update(width=im.width,height=im.height)
            if row["metadata_present"]:
                row["status"]="METADATA_REVIEW_REQUIRED"
            elif max(im.size)>128:
                row["status"]="LARGE_TEXTURE_INVENTORIED"
            else:
                row["status"]="MEASURED"
                row["metrics"]=dict(stats(im),**extra_metrics(im))
        except (ValueError,OSError) as exc:
            row.update(status="DECODE_FAILED",error=str(exc))
        rows.append(row)
    return rows

def summarize(rows):
    groups=defaultdict(list);layouts=defaultdict(list)
    for row in rows:
        groups[row["family_hint"]].append(row)
        if row["status"]=="MEASURED":
            layouts[row["metrics"]["layout_sha256"]].append(row["path"])
    summaries={}
    for name,group in sorted(groups.items()):
        valid=[r["metrics"] for r in group if r["status"]=="MEASURED"]
        summaries[name]={"total":len(group),"measured":len(valid)}
        for key in ("palette_colors","clusters_4_connected","dominant_color_fraction","same_color_x","same_color_y"):
            values=[m[key] for m in valid if m.get(key) is not None]
            if values:summaries[name][key]={"min":min(values),"median":statistics.median(values),"max":max(values)}
    return {"statuses":dict(Counter(r["status"] for r in rows)),"families":summaries,
            "shared_color_layouts":[sorted(paths) for paths in layouts.values() if len(paths)>1]}

def main():
    p=argparse.ArgumentParser(description=__doc__)
    p.add_argument("--minecraft",default="1.21.1")
    p.add_argument("--client",type=Path)
    p.add_argument("--out",type=Path,required=True)
    a=p.parse_args()
    client,sha=official_client(a.minecraft,a.client)
    with zipfile.ZipFile(io.BytesIO(client)) as z:rows=inventory(z)
    by_path={r["path"]:r for r in rows}
    pilot={n:by_path.get(n+".png",{}).get("git_blob_sha1")==expected for n,expected in PILOT.items()} if a.minecraft=="1.21.1" else {}
    summary=summarize(rows)
    report={"schema":1,"minecraft":a.minecraft,"official_client_sha1":sha,
            "scope":"Official PNG inventory; static measurements only. Family hints are not verified material labels. No automatic artistic verdict.",
            "pilot_archive_hash_matches":pilot,"total":len(rows),"summary":summary,"assets":rows}
    a.out.mkdir(parents=True,exist_ok=True)
    (a.out/"corpus.json").write_text(json.dumps(report,indent=2)+"\n")
    print("Official client:",sha,"PNG total:",len(rows),"statuses:",summary["statuses"])
    print("Archive samples confirmed against official client:",sum(pilot.values()),"/",len(pilot))
    for k in ("bark_hint","log_end_hint","foliage_hint","sapling_hint","soil_hint","ore_hint","armor_icon_hint"):
        print("FAMILY",k,json.dumps(summary["families"].get(k,{}),sort_keys=True))
    for group in summary["shared_color_layouts"]:
        if any(n.endswith(("_log.png","_leaves.png","_sapling.png","_log_top.png")) for n in group):
            print("SHARED_LAYOUT",json.dumps(group))
    if not rows or any(r["status"]=="DECODE_FAILED" for r in rows) or (pilot and not all(pilot.values())):
        raise SystemExit("Corpus verification requires investigation")
if __name__=="__main__":main()
