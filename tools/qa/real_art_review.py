"""R7 real-asset review packet and scoring. No automated art verdict."""
import argparse
import base64
import hashlib
import io
import json
import os
import random
import re
import secrets
import subprocess
import urllib.parse
import urllib.request
import zipfile
from collections import Counter
from pathlib import Path
from PIL import Image, ImageDraw
from .art_compare import official_client, reference_path
from .vanilla_corpus import git_blob
from .visual_lint import decode

ROOT=Path(__file__).resolve().parents[2]
MANIFEST=ROOT/"tools/qa/research/real-art-r7.json"
HEX40=re.compile(r"[0-9a-f]{40}")
HEX64=re.compile(r"[0-9a-f]{64}")
CRITERIA=("silhouette_or_surface_structure","material_identity","cluster_structure",
          "light_and_volume","edge_and_transparency","tile_or_icon_readability","vanilla_comparison")
VERDICTS=("ACCEPT","REJECT","UNSURE")

def digest(data):
    return hashlib.sha256(data).hexdigest()

def write_json(path,value):
    path.write_text(json.dumps(value,indent=2,ensure_ascii=False)+"\n",encoding="utf-8")

def source_bytes(repo,c):
    commit,path,expected=c["commit"],c["path"],c["blob_sha"]
    if not HEX40.fullmatch(commit) or not HEX40.fullmatch(expected) or not re.fullmatch(r"projects/[a-zA-Z0-9_./-]+\.png",path) or ".." in path.split("/"):
        raise ValueError("Invalid pinned source locator")
    if repo!="MngkLrd/gradlejar":raise ValueError("Unexpected repository")
    local=subprocess.run(["git","show",commit+":"+path],cwd=ROOT,capture_output=True,check=False)
    if local.returncode==0:
        data=local.stdout
    else:
        url="https://api.github.com/repos/"+repo+"/contents/"+urllib.parse.quote(path,safe="/")+"?ref="+commit
        headers={"Accept":"application/vnd.github+json","User-Agent":"MineMods-real-art-research"}
        token=os.getenv("GH_TOKEN")
        if token:headers["Authorization"]="Bearer "+token
        with urllib.request.urlopen(urllib.request.Request(url,headers=headers),timeout=30) as resp:
            body=resp.read(2_000_001)
        if len(body)>2_000_000:raise ValueError("GitHub source response too large")
        row=json.loads(body)
        if row.get("sha")!=expected or row.get("encoding")!="base64":
            raise ValueError("Pinned GitHub blob mismatch")
        data=base64.b64decode("".join(row["content"].split()),validate=True)
    if git_blob(data)!=expected:
        raise ValueError("Actual Git blob mismatch: "+path)
    return data

def sheet(images,family,out):
    # At native 16x and 8x; blocks additionally get a same-scale 3x3 tiled preview.
    width=216*len(images)
    height=342 if family=="soil" else 190
    canvas=Image.new("RGB",(width,height),(177,177,177))
    d=ImageDraw.Draw(canvas)
    for i,im in enumerate(images):
        if im.size!=(16,16):raise ValueError("R7 pilot expects 16x16")
        x=i*216+10
        d.text((x,8),"AB"[i],fill="black")
        canvas.paste(im,(x,29),im)
        big=im.resize((128,128),Image.Resampling.NEAREST)
        canvas.paste(big,(x,51),big)
        if family=="soil":
            tile=Image.new("RGBA",(48,48))
            for y in range(3):
                for xx in range(3):tile.paste(im,(xx*16,y*16))
            big=tile.resize((144,144),Image.Resampling.NEAREST)
            canvas.paste(big,(x,190),big)
    canvas.save(out)

def build(manifest,read_source,read_reference,out,seed):
    if out.exists():raise ValueError("Output directory must not already exist")
    public=out/"reviewer";private=out/"private";public.mkdir(parents=True);private.mkdir()
    rng=random.Random(seed)
    cases=list(manifest["cases"]);rng.shuffle(cases)
    deck=[];key=[];seen=set()
    for i,c in enumerate(cases,1):
        case_id=f"R7-{i:03d}"
        sources=list(c["candidates"]);rng.shuffle(sources)
        if len(sources) not in (1,2):raise ValueError("Invalid candidate count")
        images=[];sources_key=[]
        for side,source in zip("AB",sources):
            raw=read_source(manifest["repository"],source)
            if git_blob(raw)!=source["blob_sha"]:raise ValueError("Source reader returned wrong bytes")
            h=digest(raw)
            if h in seen:raise ValueError("Duplicate candidate image")
            seen.add(h);images.append(decode(raw))
            sources_key.append({**source,"side":side,"sha256":h})
        refs=[]
        for name in c["references"]:
            data=read_reference(reference_path(name))
            decode(data)
            refs.append({"id":name,"sha256":digest(data)})
        filename=case_id+".png"
        sheet(images,c["family"],public/filename)
        deck.append({"case":case_id,"family":c["family"],"intent":c["intent"],
                     "sheet":filename,"sheet_sha256":digest((public/filename).read_bytes()),
                     "sides":[x["side"] for x in sources_key],"references":refs})
        key.append({"case":case_id,"family":c["family"],"sources":sources_key})
    deck_doc={"schema":1,"minecraft":manifest["minecraft"],"criteria":list(CRITERIA),"cases":deck}
    write_json(public/"deck.json",deck_doc)
    deck_sha=digest((public/"deck.json").read_bytes())
    write_json(private/"key.json",{"schema":1,"deck_sha256":deck_sha,"seed":seed,"cases":key})
    (public/"INSTRUCTIONS.txt").write_text(
        "Independent exploratory review. Do not look up candidate provenance or private/key.json. "
        "Open the actual images of the listed vanilla comparators from verified Minecraft 1.21.1, "
        "using the listed hashes. For each side, judge each criterion independently with a concrete "
        "region observation; use ACCEPT, REJECT or UNSURE. For paired cases also choose A/B/TIE/UNSURE. "
        "Do not infer quality from revision age, pixel counts or preference alone. Mark UNSURE if a "
        "required image or visual context is unavailable. Rendered client evidence is NOT_RUN. "
        "Complete review-template.json in a fresh context. Freeze the response before the coordinator opens the key.\n",encoding="utf-8")
    template={"schema":1,"deck_sha256":deck_sha,"reviewer":"","review_context":"",
              "independence_declared":False,"cases":[{"case":c["case"],"preference":"UNSURE" if len(c["sides"])==2 else "N/A",
               "images":[{"side":s,"verdict":"UNSURE","findings":{k:"" for k in CRITERIA},
                          "reference_sha256":[],"correction":""} for s in c["sides"]]} for c in deck]}
    write_json(public/"review-template.json",template)
    return {"schema":1,"cases":len(deck),"pairs":sum(len(c["sides"])==2 for c in deck),
            "candidate_images":len(seen),"family_counts":dict(Counter(c["family"] for c in deck)),
            "verified_reference_bindings":sum(len(c["references"]) for c in deck),
            "reviewer_status":"NOT_RUN","owner_labels":0,"deck_sha256":deck_sha}

def evaluate(deck_path,key_path,response_path,labels_path=None):
    deck_bytes=deck_path.read_bytes()
    deck=json.loads(deck_bytes);key=json.loads(key_path.read_text(encoding="utf-8"))
    response_bytes=response_path.read_bytes();response=json.loads(response_bytes)
    h=digest(deck_bytes)
    if h!=key["deck_sha256"] or h!=response.get("deck_sha256"):raise ValueError("Deck identity mismatch")
    if not response.get("reviewer","").strip() or not response.get("review_context","").strip():
        raise ValueError("Missing reviewer/context identity")
    if response.get("independence_declared") is not True:raise ValueError("Reviewer independence not declared")
    if len(response["cases"])!=len(deck["cases"]):raise ValueError("Incomplete case set")
    by={x["case"]:x for x in response["cases"]}
    if len(by)!=len(response["cases"]):raise ValueError("Duplicate case")
    key_by={x["case"]:x for x in key["cases"]}
    labels={} if labels_path is None else json.loads(labels_path.read_text(encoding="utf-8"))["labels"]
    for sha,label in labels.items():
        if not HEX64.fullmatch(sha) or label.get("verdict") not in ("ACCEPT","REJECT") or not label.get("owner_evidence","").strip():
            raise ValueError("Invalid owner label; require exact hash and explicit evidence")
    counts=Counter();preferences=Counter();confusion=Counter();unmatched=0
    used_labels=set()
    for c in deck["cases"]:
        if digest((deck_path.parent/c["sheet"]).read_bytes())!=c["sheet_sha256"]:raise ValueError("Sheet changed")
        row=by[c["case"]];answers={x["side"]:x for x in row["images"]}
        if len(answers)!=len(row["images"]) or set(answers)!=set(c["sides"]):raise ValueError("Incomplete/duplicate image answers")
        expected_refs={x["sha256"] for x in c["references"]}
        for k in key_by[c["case"]]["sources"]:
            answer=answers[k["side"]];verdict=answer.get("verdict")
            if verdict not in VERDICTS:raise ValueError("Invalid verdict")
            findings=answer.get("findings",{})
            if set(findings)!=set(CRITERIA) or any(not isinstance(v,str) or len(v.strip())<12 for v in findings.values()):
                raise ValueError("Require specific observation for every criterion")
            if verdict!="UNSURE" and set(answer.get("reference_sha256",[]))!=expected_refs:
                raise ValueError("Missing reference inspection attestation")
            if verdict=="REJECT" and len(answer.get("correction","").strip())<12:
                raise ValueError("Rejection needs actionable correction")
            counts[verdict]+=1
            if k["sha256"] in labels:
                used_labels.add(k["sha256"])
                truth=labels[k["sha256"]]["verdict"]
                confusion[truth+"_"+verdict]+=1
            else:unmatched+=1
        pref=row.get("preference")
        if len(c["sides"])==2:
            if pref not in ("A","B","TIE","UNSURE"):raise ValueError("Invalid pair preference")
            revisions={k["side"]:k["revision"] for k in key_by[c["case"]]["sources"]}
            preferences[revisions.get(pref,pref)]+=1
        elif pref!="N/A":raise ValueError("Single-image preference must be N/A")
    if set(labels)!=used_labels:raise ValueError("Owner label not in deck")
    return {"schema":1,"reviewer":response["reviewer"],"review_context":response["review_context"],
            "reviewer_status":"SELF_ATTESTED_INDEPENDENCE","response_sha256":digest(response_bytes),
            "deck_sha256":h,"verdicts":dict(counts),"revision_preferences":dict(preferences),
            "owner_labeled_images":len(used_labels),"unlabeled_images":unmatched,
            "owner_label_confusion":dict(confusion),
            "scope":"Actual image judgments only if performed; completeness/hashes do not prove independent inspection. Revision preference is not accuracy. No client rendering or release certification."}

def main():
    p=argparse.ArgumentParser(description=__doc__);sub=p.add_subparsers(dest="cmd",required=True)
    b=sub.add_parser("build");b.add_argument("--out",type=Path,required=True);b.add_argument("--seed",type=int)
    b.add_argument("--client",type=Path);b.add_argument("--manifest",type=Path,default=MANIFEST)
    e=sub.add_parser("evaluate");e.add_argument("--deck",type=Path,required=True);e.add_argument("--key",type=Path,required=True)
    e.add_argument("--response",type=Path,required=True);e.add_argument("--labels",type=Path);e.add_argument("--out",type=Path,required=True)
    a=p.parse_args()
    if a.cmd=="build":
        manifest=json.loads(a.manifest.read_text(encoding="utf-8"))
        client,sha=official_client(manifest["minecraft"],a.client)
        with zipfile.ZipFile(io.BytesIO(client)) as z:
            report=build(manifest,source_bytes,z.read,a.out,a.seed if a.seed is not None else secrets.randbits(64))
        report["official_client_sha1"]=sha
        write_json(a.out/"summary.json",report)
        print("REAL_ART_R7",json.dumps(report,sort_keys=True))
    else:
        result=evaluate(a.deck,a.key,a.response,a.labels);write_json(a.out,result)
        print("REAL_ART_REVIEW",json.dumps(result,sort_keys=True))
if __name__=="__main__":main()
