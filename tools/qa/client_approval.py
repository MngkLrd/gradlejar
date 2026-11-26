"""Manually approve a complete client-only cohort, matching each tested candidate SHA."""
import argparse
import json
import os
import re
import shutil
import subprocess
import zipfile
from pathlib import Path
from .common import ROOT, SLUG, SAFE_VERSION, profile, qa_config, project_tree, sha256, require, write_json
from .static import validate_jar
from .bundle import ORDER

def gh(*args,timeout=120):
    return subprocess.run(["gh",*args],check=True,capture_output=True,timeout=timeout).stdout

def approved_candidates(reports, jars, slug, approved, run_id, attempt, commit):
    require(isinstance(approved,dict) and approved,"Approval must map every exact JAR filename to a SHA-256")
    require(all(isinstance(k,str) and isinstance(v,str) and re.fullmatch(r"[a-f0-9]{64}",v) for k,v in approved.items()),"Invalid approval SHA-256")
    rows=[]
    for file in Path(reports).rglob("*.json"):
        r=json.loads(file.read_text("utf-8"))
        if not isinstance(r,dict) or r.get("slug")!=slug:continue
        if r.get("mode")!="client" or r.get("status")!="client-candidate-awaiting-validation":continue
        require(r.get("schema")==1 and r.get("qa_level")==2 and r.get("eligible") is False,"Not a static client candidate")
        require(str(r.get("run_id"))==str(run_id) and str(r.get("run_attempt"))==str(attempt) and r.get("commit")==commit,"Candidate provenance mismatch")
        rows.append(r)
    require(rows,"No client candidate evidence")
    expected=set(rows[0].get("release_targets",[]))
    require(expected and all(set(r.get("release_targets",[]))==expected for r in rows),"Inconsistent release target matrix")
    require(len(rows)==len(expected) and {r.get("target") for r in rows}==expected,"Incomplete or duplicate client cohort")
    require(len({r["version"] for r in rows})==1 and SAFE_VERSION.fullmatch(rows[0]["version"]),"Mixed/invalid version")
    require(set(approved)=={r["jar"] for r in rows},"Approval must cover exactly every JAR, no omissions or extras")
    byname={}
    for f in Path(jars).rglob("*.jar"):
        byname.setdefault(f.name,[]).append(f)
    result=[]
    for r in rows:
        name=r["jar"]
        require(len(byname.get(name,[]))==1,"Missing/duplicate candidate: "+name)
        jar=byname[name][0]
        require(sha256(jar)==r["sha256"]==approved[name],"Manual approval hash mismatch: "+name)
        result.append((r,jar))
    return sorted(result,key=lambda x:(ORDER[x[0]["loader"]],tuple(map(int,x[0]["minecraft"].split(".")))))

def main():
    ap=argparse.ArgumentParser()
    ap.add_argument("--out",required=True)
    a=ap.parse_args()
    repo=os.environ["GITHUB_REPOSITORY"];run_id=os.environ["SOURCE_RUN"];attempt=os.environ["SOURCE_ATTEMPT"]
    slug=os.environ["MOD_SLUG"];actor=os.environ["GITHUB_ACTOR"];note=os.environ["REVIEW_NOTE"].strip()
    require(os.environ.get("CONFIRM_CLIENT_TESTED")=="true","Explicit manual verification is required")
    require(run_id.isdigit() and attempt.isdigit() and SLUG.fullmatch(slug),"Invalid source run, attempt or slug")
    require(10<=len(note)<=2000,"Describe manual verification (10–2000 characters)")
    approved=json.loads(os.environ["APPROVAL_JSON"])
    meta=json.loads(gh("api",f"repos/{repo}/actions/runs/{run_id}"))
    require(meta.get("name")=="Minecraft Mod Build Hub" and meta.get("head_branch")=="main" and meta.get("head_repository",{}).get("full_name")==repo and meta.get("event") in ("push","workflow_dispatch") and meta.get("status")=="completed","Only completed trusted main Build Hub runs may be approved")
    commit=meta.get("head_sha","")
    require(re.fullmatch(r"[a-f0-9]{40}",commit),"Invalid source commit")
    out=Path(a.out).resolve();out.mkdir(parents=True,exist_ok=True)
    inp=out/"input";inp.mkdir()
    gh("run","download",run_id,"--repo",repo,"--pattern",f"qa-report-*-{attempt}","--dir",str(inp/"reports"))
    gh("run","download",run_id,"--repo",repo,"--pattern",f"qa-candidate-NOT-RELEASE-*-{attempt}","--dir",str(inp/"candidates"))
    files=approved_candidates(inp/"reports",inp/"candidates",slug,approved,run_id,attempt,commit)
    reportdir=out/"reports";reportdir.mkdir()
    version=files[0][0]["version"]
    archive=out/f"{slug}-{version}-ALL-{len(files)}-JARs.zip"
    # Revalidate every exact artifact against the current enrolled source profile before release.
    for r,jar in files:
        p=profile(r["project"]);q=qa_config(p)
        require(q["mode"]=="client" and p["version"]==r["version"] and p["slug"]==slug,"Current client policy differs")
        require(r.get("tree") and r["tree"]==project_tree(r["project"]),"Source changed since candidate was built: "+r["project"])
        require(set(q["release_targets"])==set(r["release_targets"]),"Release matrix changed")
        validate_jar(jar,p)
    with zipfile.ZipFile(archive,"w",zipfile.ZIP_DEFLATED) as z:
        for r,jar in files:
            z.write(jar,jar.name)
            r.update(status="manual-client-approved",qa_level=5,eligible=True,manual_client_verified=True,approved_by=actor,approval_run_id=os.environ["GITHUB_RUN_ID"],review_note=note,runtime="human-reviewed-client")
            write_json(reportdir/(r["target"]+".json"),r)
    write_json(out/"approval.json",{"source_run":run_id,"source_attempt":attempt,"source_commit":commit,"approved_by":actor,"review_note":note,"archive":archive.name,"archive_sha256":sha256(archive),"jars":approved})
    (out/"notes.md").write_text(f"Manually client-verified cohort by GitHub actor {actor}.\nSource Build Hub run {run_id}, attempt {attempt}.\nExact candidate SHA-256 verified before packaging.\n\nReview notes:\n{note}\n",encoding="utf-8")
    shutil.rmtree(inp)
    with open(os.environ["GITHUB_OUTPUT"],"a",encoding="utf-8") as f:
        f.write(f"archive={archive}\ncommit={commit}\nslug={slug}\nversion={version}\n")
    print("Manual QA-5 cohort verified:",archive.name)

if __name__=="__main__":main()
