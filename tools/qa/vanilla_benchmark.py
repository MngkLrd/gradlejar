"""Private perturbation benchmark for visual reviewers. Not an automated aesthetic oracle."""
import argparse
import hashlib
import io
import json
import random
import zipfile
from collections import Counter
from pathlib import Path
from PIL import Image,ImageDraw
from .art_compare import official_client
from .vanilla_corpus import PREFIX, extra_metrics
from .visual_lint import decode, stats

# Not part of the 19 visually inspected pilot samples. Hold out entire source
# images and their mutations from future fitting; same-family knowledge is
# intentional in this pilot, not a held-out-family generalization claim.
HOLDOUT = {
    "bark":["block/cherry_log","block/mangrove_log"],
    "log_end":["block/acacia_log_top","block/birch_log_top"],
    "foliage":["block/azalea_leaves","block/mangrove_leaves"],
    "sapling":["block/dark_oak_sapling","block/oak_sapling"],
    "soil":["block/coarse_dirt","block/rooted_dirt"],
    "ore":["block/redstone_ore","block/gold_ore"],
    "armor":["item/iron_chestplate","item/chainmail_chestplate"],
    "vessel":["item/bowl","item/glass_bottle"],
}
METHODS=("shuffle_visible","flatten_ramp","shuffle_alpha")
def pixels(im):
    return list(im.getdata())
def visible_hist(im):
    return Counter(p for p in pixels(im) if p[3])
def mutate(im,mode,rng):
    px=pixels(im); result=im.copy()
    if mode=="shuffle_visible":
        ids=[i for i,p in enumerate(px) if p[3]]
        colors=[px[i] for i in ids]; rng.shuffle(colors)
        for i,p in zip(ids,colors):px[i]=p
    elif mode=="flatten_ramp":
        counts=visible_hist(im)
        if len(counts)<3:return None
        keep=[p for p,n in counts.most_common(max(2,len(counts)//2))]
        px=[min(keep,key=lambda q:sum((p[j]-q[j])**2 for j in range(3))) if p[3] else p for p in px]
    elif mode=="shuffle_alpha":
        if not any(p[3]==0 for p in px) or not any(p[3]>0 for p in px):return None
        rng.shuffle(px)
    else:raise ValueError("Unknown perturbation")
    result.putdata(px)
    return result if pixels(result)!=pixels(im) else None

def measure(im):
    s=dict(stats(im),**extra_metrics(im))
    return {k:s.get(k) for k in ("palette_colors","visible_pixels","clusters_4_connected","singleton_pixel_fraction","largest_cluster_area","same_color_edge_fraction","adjacent_mean_value_delta","same_color_x","same_color_y","layout_sha256")}

def sha(im):
    return hashlib.sha256(im.tobytes()).hexdigest()

def build(z,seed):
    rng=random.Random(seed);cases=[];assets=[]
    for family,refs in HOLDOUT.items():
        for ref in refs:
            path=PREFIX+ref+".png"
            if path+".mcmeta" in z.namelist():raise ValueError("Animated holdout "+path)
            data=z.read(path);source=decode(data)
            assets.append({"family":family,"reference":ref,"source_png_sha256":hashlib.sha256(data).hexdigest(),
                           "layout_sha256":extra_metrics(source)["layout_sha256"]})
            for mode in METHODS:
                altered=mutate(source,mode,rng)
                if altered is None:continue
                if mode=="shuffle_visible":
                    if visible_hist(source)!=visible_hist(altered) or [p[3] for p in pixels(source)]!=[p[3] for p in pixels(altered)]:
                        raise ValueError("Control must preserve palette frequencies and alpha mask")
                if mode=="shuffle_alpha" and Counter(pixels(source))!=Counter(pixels(altered)):
                    raise ValueError("Control must preserve RGBA histogram")
                order=[source,altered]
                answer=rng.choice(("A","B"))
                if answer=="B":order.reverse()
                cases.append({"family":family,"reference":ref,"mode":mode,"source_side":answer,
                              "source_metrics":measure(source),"perturbed_metrics":measure(altered),
                              "images":order,"rgba_sha256":[sha(i) for i in order]})
    rng.shuffle(cases)
    for i,case in enumerate(cases,1):case["id"]=f"case-{i:03d}"
    return cases,assets

# Fixed, unfitted diagnostic baselines. Directional choices are hypotheses,
# not art rules; publish misses and abstentions as well as successes.
BASELINES={
    "more_palette_colors":("palette_colors",1),
    "less_isolated_pixels":("singleton_pixel_fraction",-1),
    "more_same_color_edges":("same_color_edge_fraction",1),
    "less_adjacent_value_delta":("adjacent_mean_value_delta",-1),
    "larger_component":("largest_cluster_area",1),
}
def baseline_results(cases):
    results={}
    for rule,(key,direction) in BASELINES.items():
        total=Counter(correct=0,wrong=0,unsure=0,total=0)
        by_mode={}
        for case in cases:
            source=case["source_metrics"].get(key)
            altered=case["perturbed_metrics"].get(key)
            delta=None if source is None or altered is None else direction*(source-altered)
            decision="unsure" if delta is None or abs(delta)<1e-10 else ("correct" if delta>0 else "wrong")
            mode=by_mode.setdefault(case["mode"],Counter(correct=0,wrong=0,unsure=0,total=0))
            for counts in (total,mode):
                counts[decision]+=1;counts["total"]+=1
        results[rule]={"metric":key,"direction":direction,"total":dict(total),
                       "by_mode":{m:dict(c) for m,c in sorted(by_mode.items())},
                       "coverage":(total["correct"]+total["wrong"])/total["total"] if total["total"] else None,
                       "source_choice_accuracy":total["correct"]/total["total"] if total["total"] else None}
    return results

def report(cases,assets,version,client_sha,seed):
    by=Counter(c["mode"] for c in cases)
    unchanged_palette=sum(c["source_metrics"]["palette_colors"]==c["perturbed_metrics"]["palette_colors"] for c in cases)
    unchanged_visible=sum(c["source_metrics"]["visible_pixels"]==c["perturbed_metrics"]["visible_pixels"] for c in cases)
    return {"schema":1,"minecraft":version,"official_client_sha1":client_sha,"seed":seed,
            "source_count":len(assets),"pair_count":len(cases),"controls":dict(by),
            "equal_palette_count_pairs":unchanged_palette,"equal_visible_area_pairs":unchanged_visible,
            "source_assets":assets,"unfitted_metric_baselines":baseline_results(cases),
            "reviewer_status":"NOT_RUN","scope":"Perturbed vs source pairs, not human-validated bad art; no reviewer accuracy yet."}

def private_deck(out,cases):
    out.mkdir(parents=True,exist_ok=True)
    # No names, modes, metrics or labels in reviewer-facing sheets.
    for c in cases:
        sheet=Image.new("RGB",(352,202),(184,184,184));d=ImageDraw.Draw(sheet)
        d.text((8,5),c["id"],fill="black")
        for i,im in enumerate(c["images"]):
            x=8+i*176;d.text((x,22),"AB"[i],fill="black")
            large=im.resize((im.width*8,im.height*8),Image.Resampling.NEAREST)
            sheet.paste(large,(x,40),large)
            sheet.paste(im,(x,178),im)
        sheet.save(out/(c["id"]+".png"))
    key=[{k:v for k,v in c.items() if k!="images"} for c in cases]
    (out/"PRIVATE-key.json").write_text(json.dumps(key,indent=2)+"\n")
    (out/"reviewer-instructions.txt").write_text(
        "Evaluate both images before choosing A/B/UNSURE as the better vanilla-style example. "
        "Describe each image independently: specific silhouette, light, cluster, material and edge observations. "
        "Record a separate aesthetic accept/reject/unsure decision for each. Do not read PRIVATE-key.json until responses are frozen. "
        "A perturbed image is not automatically bad; identify actual visible defects. No renderer evidence implied.\n")
def score(key,answers):
    """Report source-choice accuracy separately from subjective artistic verdicts."""
    ids={c["id"] for c in key}
    if set(answers)!=ids or any(v not in ("A","B","UNSURE") for v in answers.values()):
        raise ValueError("Require one A/B/UNSURE response per case")
    groups={}
    for c in key:
        result=groups.setdefault(c["family"],Counter(total=0,correct=0,wrong=0,unsure=0))
        result["total"]+=1
        choice=answers[c["id"]]
        result["unsure" if choice=="UNSURE" else "correct" if choice==c["source_side"] else "wrong"]+=1
    totals=sum((Counter(g) for g in groups.values()),Counter())
    return {"total":dict(totals),"families":{k:dict(v) for k,v in groups.items()},
            "source_choice_accuracy":totals["correct"]/totals["total"] if totals["total"] else None,
            "scope":"Source-side recognition only, not artistic acceptance accuracy or independence certification."}

def main():
    p=argparse.ArgumentParser(description=__doc__)
    p.add_argument("--minecraft",default="1.21.1");p.add_argument("--client",type=Path)
    p.add_argument("--seed",type=int,default=206);p.add_argument("--out",required=True,type=Path)
    p.add_argument("--private-deck",action="store_true",help="Local only. Never upload vanilla images or answer key publicly.")
    a=p.parse_args()
    client,client_sha=official_client(a.minecraft,a.client)
    with zipfile.ZipFile(io.BytesIO(client)) as z:cases,assets=build(z,a.seed)
    result=report(cases,assets,a.minecraft,client_sha,a.seed)
    a.out.mkdir(parents=True,exist_ok=True)
    (a.out/"benchmark-summary.json").write_text(json.dumps(result,indent=2)+"\n")
    if a.private_deck:private_deck(a.out/"private-deck",cases)
    print("BLIND_BENCHMARK",json.dumps({k:v for k,v in result.items() if k not in ("source_assets","unfitted_metric_baselines")},sort_keys=True))
    for rule,row in result["unfitted_metric_baselines"].items():
        print("METRIC_BASELINE",rule,json.dumps(row,sort_keys=True))
if __name__=="__main__":main()
