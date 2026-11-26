"""Build flat JAR-only ZIPs from exact, production-tested reports of one CI run."""
import argparse
import json
import os
import re
import zipfile
from collections import defaultdict
from pathlib import Path
from .common import require, sha256, write_json, SLUG, SAFE_VERSION

ORDER={"forge":0,"neoforge":1,"fabric":2}

def build(reports_dir,jars_dir,out,run_id):
    groups=defaultdict(list)
    for path in Path(reports_dir).rglob("*.json"):
        try: r=json.loads(path.read_text("utf-8"))
        except (OSError,ValueError): continue
        if not isinstance(r,dict) or r.get("schema")!=1 or r.get("mode")!="server": continue
        if str(r.get("run_id"))!=str(run_id): continue
        groups[(r.get("slug"),r.get("version"))].append(r)
    found=defaultdict(list)
    for path in Path(jars_dir).rglob("*.jar"): found[path.name].append(path)
    out=Path(out);out.mkdir(parents=True,exist_ok=True)
    summary=[]
    for (slug,version),rs in sorted(groups.items()):
        row={"slug":slug,"version":version,"status":"incomplete"}
        summary.append(row)
        try:
            require(isinstance(slug,str) and SLUG.fullmatch(slug) and isinstance(version,str) and SAFE_VERSION.fullmatch(version),"Unsafe bundle identity")
            expected=set(rs[0].get("release_targets",[]))
            require(expected and all(set(r.get("release_targets",[]))==expected for r in rs),"Inconsistent release target matrix")
            require(len(rs)==len(expected) and {r.get("target") for r in rs}==expected,"Incomplete or duplicate release matrix")
            require(all(r.get("eligible") is True and r.get("qa_level")==4 and r.get("status")=="passed" and r.get("runtime")=="production-jar" for r in rs),"Not every target passed production gameplay tests")
            require(len({r.get("commit") for r in rs})==1 and bool(rs[0].get("commit")),"Mixed source commits")
            files=[]
            for r in rs:
                target=r["target"]
                require(re.fullmatch(r"(forge|neoforge|fabric)-mc\d+\.\d+(?:\.\d+)?",target),"Unsafe target")
                name=f"{slug}-{target}-{version}.jar"
                require(r.get("jar")==name and len(found[name])==1,"Missing or duplicate final JAR "+name)
                f=found[name][0]
                require(sha256(f)==r.get("sha256")==r.get("installed_sha256"),"Tested/packaged SHA-256 differs: "+name)
                with zipfile.ZipFile(f) as z: require(z.testzip() is None,"Corrupt JAR "+name)
                files.append((r,f))
            files.sort(key=lambda x:(ORDER[x[0]["loader"]],tuple(map(int,x[0]["minecraft"].split(".")))))
            name=f"{slug}-{version}-ALL-{len(files)}-JARs.zip"
            with zipfile.ZipFile(out/name,"w",zipfile.ZIP_DEFLATED) as z:
                for r,f in files: z.write(f,f.name)
            row.update(status="passed",archive=name,sha256=sha256(out/name),targets=[r["target"] for r,_ in files])
        except (ValueError,OSError,KeyError,zipfile.BadZipFile,TypeError) as e:
            row["reason"]=str(e)
            print("Bundle not published:",slug,version,e)
    write_json(out/"bundle-report.json",summary)
    return summary

if __name__=="__main__":
    ap=argparse.ArgumentParser()
    ap.add_argument("reports");ap.add_argument("jars");ap.add_argument("out")
    ap.add_argument("--run-id",default=os.getenv("GITHUB_RUN_ID",""))
    a=ap.parse_args()
    build(a.reports,a.jars,a.out,a.run_id)
