"""Prepare upload manifests and changelog from explicit evidence; never contact publishing APIs."""
import argparse
import json
import re
import zipfile
from pathlib import Path
from .common import require,sha256,write_json,SLUG,SAFE_VERSION

def prepare(spec,archive,reports,out):
    slug=spec["slug"];version=spec["version"]
    require(SLUG.fullmatch(slug) and SAFE_VERSION.fullmatch(version),"Invalid slug/version")
    require(isinstance(spec.get("summary"),str) and spec["summary"].strip(),"Summary required")
    require(isinstance(spec.get("changes"),list) and spec["changes"] and all(isinstance(x,str) and x.strip() for x in spec["changes"]),"Human-written changes required")
    evidence={}
    for f in Path(reports).rglob("*.json"):
        r=json.loads(f.read_text("utf-8"))
        if not isinstance(r,dict) or r.get("slug")!=slug or r.get("version")!=version:continue
        if r.get("kind") in ("compatibility","p4-diagnostic","p5-benchmark"):continue
        if not r.get("jar"):continue
        require(r["jar"] not in evidence,"Duplicate release evidence")
        evidence[r["jar"]]=r
    expected=set(spec["targets"]);require(expected and len(expected)==len(spec["targets"]),"Duplicate or missing targets")
    rows=[]
    with zipfile.ZipFile(archive) as z:
        names=z.namelist();require(z.testzip() is None and len(names)==len(set(names)) and all(re.fullmatch(r"[A-Za-z0-9_.-]+\.jar",n) for n in names),"Archive must contain flat JARs only")
        import hashlib,io
        for n in names:
            require(n in evidence,"No QA evidence for "+n);r=evidence[n]
            require(r.get("eligible") is True and ((r.get("qa_level")==4 and r.get("status")=="passed" and r.get("runtime")=="production-jar") or (r.get("qa_level")==5 and r.get("status")=="manual-client-approved" and r.get("manual_client_verified") is True)),"Unapproved candidate: "+n)
            require(set(r.get("release_targets",[]))==expected,"Release matrix differs")
            require(r.get("target") in expected and n==f"{slug}-{r['target']}-{version}.jar","Filename/target mismatch")
            b=z.read(n);h=hashlib.sha256(b).hexdigest();require(h==r.get("sha256"),"JAR hash mismatch")
            if r["qa_level"]==4:require(h==r.get("installed_sha256"),"Production runtime hash mismatch")
            with zipfile.ZipFile(io.BytesIO(b)) as jar:require(jar.testzip() is None,"Invalid nested JAR")
            rows.append({"file":n,"sha256":h,"minecraft":r["minecraft"],"loader":r["loader"],"target":r["target"],"qa_level":r["qa_level"],"source_commit":r.get("commit"),"run_id":r.get("run_id")})
    require(len(rows)==len(expected) and {r["target"] for r in rows}==expected,"Incomplete release")
    require(len({r["source_commit"] for r in rows})==1 and rows[0]["source_commit"],"Mixed source revisions")
    out=Path(out);require(not out.exists(),"Output exists");out.mkdir(parents=True)
    change="# "+spec.get("name",slug)+" "+version+"\n\n"+spec["summary"]+"\n\n## Changes\n"+"\n".join("- "+x for x in spec["changes"])+"\n\n## Verification\n"+"\n".join("- "+r["target"]+": QA-"+str(r["qa_level"]) for r in rows)+"\n\nClient visuals are only verified where explicitly marked QA-5.\n"
    (out/"CHANGELOG.md").write_text(change,encoding="utf-8")
    manifests=[]
    for r in rows:
        manifests.append(dict(r,curseforge={"displayName":f"{spec.get('name',slug)} {version} | {r['loader']} {r['minecraft']}","releaseType":spec.get("release_type","beta"),"gameVersionLabels":[r["minecraft"],r["loader"]],"requires_platform_version_ids":True},modrinth={"name":f"{spec.get('name',slug)} {version}","version_number":version+"-"+r["target"],"game_versions":[r["minecraft"]],"loaders":[r["loader"]],"version_type":spec.get("release_type","beta")}))
    write_json(out/"upload-plan.json",{"schema":1,"status":"draft-manual-upload","archive":Path(archive).name,"archive_sha256":sha256(archive),"files":manifests,"notes":"Resolve actual platform project/game-version IDs and verify availability before upload. No API calls or publication performed."})
    return rows

if __name__=="__main__":
    p=argparse.ArgumentParser();p.add_argument("spec");p.add_argument("archive");p.add_argument("reports");p.add_argument("--out",required=True);a=p.parse_args()
    print("Prepared",len(prepare(json.loads(Path(a.spec).read_text("utf-8")),a.archive,a.reports,a.out)),"upload entries; not published")
