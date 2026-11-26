"""Plan ports without copying incompatible Java or overwriting existing projects."""
import argparse
import hashlib
import json
from collections import defaultdict
from pathlib import Path
from .common import ROOT,profile,relative_project,REGISTRY,write_json,require,shared_paths,safe_path
from .scaffold import select
from .port_audit import audit

def plan(project,targets):
    p=profile(project);src=relative_project(project)
    files=[]
    for root in [src,*[safe_path(ROOT,x) for x in shared_paths(p)]]:
        for f in sorted((root/"src/main").rglob("*")):
            if f.is_file():files.append({"path":f.relative_to(ROOT).as_posix(),"sha256":hashlib.sha256(f.read_bytes()).hexdigest()})
    rows=[]
    for mc,l in select(targets):
        dest="projects/"+p["slug"]+"-"+l+"-"+mc
        rows.append({"target":l+"-mc"+mc,"destination":dest,"exists":(ROOT/dest).exists(),"baseline":REGISTRY["minecraft"][mc]["loaders"][l],"source_audit":audit(project,mc)["findings"],"required_review":["loader hooks and mappings","runtime descriptors","data/resource formats","production JAR regression"]})
    return {"schema":1,"source":project,"status":"plan-only","source_files":files,"targets":rows}

def duplicates(projects):
    byhash=defaultdict(list)
    for project in projects:
        src=relative_project(project)
        for f in sorted((src/"src/main/java").rglob("*.java")):
            # Only byte-identical source is counted. Do not infer semantic equivalence.
            b=f.read_bytes()
            if len(b)<100:continue
            byhash[hashlib.sha256(b).hexdigest()].append({"project":project,"file":f.relative_to(src).as_posix(),"bytes":len(b)})
    groups=[{"sha256":h,"copies":rows,"duplicated_bytes":sum(r["bytes"] for r in rows[1:])} for h,rows in byhash.items() if len({r["project"] for r in rows})>1]
    return {"schema":1,"scope":"byte-identical Java files across selected ports","groups":sorted(groups,key=lambda x:-x["duplicated_bytes"]),"duplicated_bytes":sum(g["duplicated_bytes"] for g in groups)}

if __name__=="__main__":
    ap=argparse.ArgumentParser();sub=ap.add_subparsers(dest="cmd",required=True)
    a=sub.add_parser("plan");a.add_argument("project");a.add_argument("--targets",nargs="+",default=["all"]);a.add_argument("--out",required=True)
    a=sub.add_parser("duplicates");a.add_argument("projects",nargs="+");a.add_argument("--out",required=True)
    a=ap.parse_args();r=plan(a.project,a.targets) if a.cmd=="plan" else duplicates(a.projects);write_json(a.out,r)
    print(a.cmd, "report written",a.out)
