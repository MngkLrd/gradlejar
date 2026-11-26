"""Produce a reviewed QA profile proposal; no source overwrite or automatic enrollment."""
import argparse
import json
from pathlib import Path
from .common import ROOT,profile,compatibility,filename,target,safe_path,require,write_json
from .server import scenario_steps

def proposal(project,mode,scenario,targets,loader_version=None,dependencies=None):
    p=profile(project);mc,l=compatibility(p)
    require(mode in ("server","client"),"Invalid QA mode")
    require(p["jar_glob"]==filename(p),"Rename real production artifact before migration")
    require(target(p) in targets and len(set(targets))==len(targets),"Current target missing or duplicate")
    q={"mode":mode,"loader_version":loader_version or l["version"],"release_targets":targets}
    if mode=="server":
        require(scenario,"Provide a reviewed real scenario")
        scenario_steps(json.loads(safe_path(ROOT,scenario).read_text()),p["loader"])
        q["scenario"]=scenario
    if dependencies:q["dependencies"]=dependencies
    p.pop("project_dir",None);p["qa"]=q
    from .common import qa_config
    qa_config(dict(p,project_dir=project))
    return {"schema":1,"status":"proposal-not-approved","project":project,"profile":p,"review":["Verify actual loader dependency and runtime side","Validate pack.mcmeta and required mods","Run independent production tests","Remove legacy exception only in reviewed migration commit"]}

if __name__=="__main__":
    a=argparse.ArgumentParser();a.add_argument("project");a.add_argument("--mode",choices=["server","client"],required=True);a.add_argument("--scenario");a.add_argument("--targets",nargs="+",required=True);a.add_argument("--loader-version");a.add_argument("--out",required=True);x=a.parse_args()
    write_json(x.out,proposal(x.project,x.mode,x.scenario,x.targets,x.loader_version))
