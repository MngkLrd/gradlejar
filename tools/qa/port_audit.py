"""Read-only migration guidance: no automatic Java edits."""
import argparse
import json
import re
from pathlib import Path
from .common import ROOT, profile, relative_project, REGISTRY, require, write_json

def audit(project,target_mc):
    p=profile(project);base=relative_project(project)
    require(target_mc in REGISTRY["minecraft"],"Unknown target")
    rules=json.loads((ROOT/"tools/qa/migration-rules.json").read_text())["rules"]
    hits=[]
    for f in (base/"src/main").rglob("*"):
        if not f.is_file() or f.suffix not in (".java",".json",".mcmeta"):continue
        rel=f.relative_to(base).as_posix()
        try:text=f.read_text("utf-8")
        except UnicodeError:continue
        for rule in rules:
            if target_mc not in rule["versions"]:continue
            for i,line in enumerate(text.splitlines(),1):
                if re.search(rule["pattern"],line) or (i==1 and re.search(rule["pattern"],rel)):
                    hits.append({"rule":rule["id"],"file":rel,"line":i,"evidence":line.strip()[:220],"advice":rule["advice"]})
                    break
    return {"schema":1,"project":project,"source":p["minecraft"],"target":target_mc,"loader":p["loader"],"target_baseline":REGISTRY["minecraft"][target_mc]["loaders"].get(p["loader"]),"findings":hits,"automatic_changes":False}
if __name__=="__main__":
    a=argparse.ArgumentParser();a.add_argument("project");a.add_argument("target");a.add_argument("--out",required=True);x=a.parse_args()
    r=audit(x.project,x.target);write_json(x.out,r);print("Migration hints:",len(r["findings"]))
