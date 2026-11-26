"""One upload manifest for every complete QA-4 cohort in a run; no external API writes."""
import argparse
import hashlib
import json
import re
import zipfile
from pathlib import Path
from .common import require,sha256,write_json

def prepare(bundles,reports,out):
    bundles=Path(bundles);out=Path(out);out.mkdir(parents=True,exist_ok=True)
    index=json.loads((bundles/"bundle-report.json").read_text())
    evidence={}
    for f in Path(reports).rglob("*.json"):
        r=json.loads(f.read_text())
        if not isinstance(r,dict) or not r.get("jar"):continue
        key=r["jar"];require(key not in evidence,"Duplicate JAR evidence")
        evidence[key]=r
    files=[];blocked=[];archive_names=set()
    for cohort in index:
        if cohort["status"]!="passed":
            blocked.append(cohort);continue
        name=cohort["archive"]
        require(re.fullmatch(r"[A-Za-z0-9_.-]+\.zip",name) and name not in archive_names,"Unsafe/duplicate cohort archive")
        archive_names.add(name);archive=bundles/name
        require(sha256(archive)==cohort["sha256"],"Archive hash mismatch")
        with zipfile.ZipFile(archive) as z:
            require(z.testzip() is None and len(z.namelist())==len(cohort["targets"]),"Archive incomplete/corrupt")
            seen=set()
            for jar in z.namelist():
                require(re.fullmatch(r"[A-Za-z0-9_.-]+\.jar",jar) and jar in evidence,"Unexpected release file")
                r=evidence[jar]
                require(r.get("eligible") is True and r.get("qa_level")==4 and r.get("runtime")=="production-jar" and r.get("status")=="passed","Unverified JAR")
                require(r["slug"]==cohort["slug"] and r["version"]==cohort["version"] and r["target"] in cohort["targets"] and r["target"] not in seen,"Cohort mismatch")
                seen.add(r["target"]);h=hashlib.sha256(z.read(jar)).hexdigest()
                require(h==r["sha256"]==r["installed_sha256"],"Runtime hash mismatch")
                if "virtual_player" in r:require(r["virtual_player"]["status"]=="passed","Player verification failed")
                files.append({"slug":r["slug"],"version":r["version"],"target":r["target"],"minecraft":r["minecraft"],"loader":r["loader"],"archive":name,"file":jar,"sha256":h,"run_id":r["run_id"],"qa":4,"player_status":r.get("virtual_player",{}).get("status","not-run"),"curseforge":{"game_version_labels":[r["minecraft"],r["loader"]],"project_id":None},"modrinth":{"game_versions":[r["minecraft"]],"loaders":[r["loader"]],"project_id":None}})
            require(seen==set(cohort["targets"]),"Missing target")
    write_json(out/"bulk-upload-plan.json",{"schema":1,"status":"prepared-not-published","files":files,"blocked_cohorts":blocked,"scope":"Verified JARs only. Resolve actual platform project/version IDs and approve publication separately."})
    print("Bulk upload plan:",len(files),"eligible files,",len(blocked),"blocked cohorts")
    return files
if __name__=="__main__":
    p=argparse.ArgumentParser();p.add_argument("bundles");p.add_argument("reports");p.add_argument("--out",required=True);a=p.parse_args();prepare(a.bundles,a.reports,a.out)
