"""Measure an isolated exact-toolchain cache experiment; never release evidence."""
import argparse
import json
import os
import re
import time
from pathlib import Path

OUT=Path("cache-pilot")
BASELINE={"minecraft":"1.21.1","loader":"neoforge","java":"21","gradle":"9.2.1",
          "compile_loader_version":"21.1.262"}
PLUGIN="2.0.148"

def check(project):
    project=Path(project)
    p=json.loads((project/"build-profile.json").read_text())
    for k,v in BASELINE.items():
        if p.get(k)!=v: raise ValueError(f"Pilot toolchain changed: {k}={p.get(k)!r}; expected {v}")
    props=(project/"gradle.properties").read_text()
    if re.findall(r"(?m)^\s*neo_version\s*=\s*(\S+)\s*$",props)!=[BASELINE["compile_loader_version"]]:
        raise ValueError("Pilot NeoForge properties changed")
    gradle=(project/"build.gradle").read_text()
    if not re.search(r"id\s+['\"]net\.neoforged\.moddev['\"]\s+version\s+['\"]"+re.escape(PLUGIN)+r"['\"]",gradle):
        raise ValueError("Pilot ModDev plugin changed")
    if not re.search(r"version\s*=\s*project\.neo_version\b",gradle):
        raise ValueError("Pilot loader is not using the checked property")
    print("Exact pilot toolchain verified:",BASELINE, "ModDev",PLUGIN)

def mark(name):
    if not re.fullmatch(r"[a-z-]+",name): raise ValueError("Invalid mark")
    OUT.mkdir(exist_ok=True)
    (OUT/(name+".json")).write_text(json.dumps(time.monotonic())+"\n")

def elapsed(start,end):
    a=json.loads((OUT/(start+".json")).read_text())
    b=json.loads((OUT/(end+".json")).read_text())
    if not 0<=b-a<=3600: raise ValueError("Invalid timing interval")
    return round(b-a,3)

def report(mode):
    prefix="seed" if mode=="seed" else "compare"
    result={"schema":1,"mode":mode,"toolchain":BASELINE,"moddev":PLUGIN,
            "commit":os.getenv("GITHUB_SHA",""),"run":os.getenv("GITHUB_RUN_ID",""),
            "scope":"Compile and verify fresh output only; no runtime/release certification.",
            "setup_seconds":elapsed(prefix+"-start",prefix+"-build-start"),
            "compile_and_verify_seconds":elapsed(prefix+"-build-start",prefix+"-build-end"),
            "through_verification_seconds":elapsed(prefix+"-start",prefix+"-build-end")}
    if mode=="seed": result["cache_save_seconds"]=elapsed("seed-build-end","seed-end")
    text=json.dumps(result,indent=2)
    (OUT/(mode+"-report.json")).write_text(text+"\n")
    print(text)
    if path:=os.getenv("GITHUB_STEP_SUMMARY"):
        with open(path,"a") as f: f.write("\n### Cache pilot: "+mode+"\n\n"+" | ".join(f"{k}: {v}s" for k,v in result.items() if k.endswith("_seconds"))+"\n")

def main():
    parser=argparse.ArgumentParser(description=__doc__)
    sub=parser.add_subparsers(dest="command",required=True)
    sub.add_parser("check").add_argument("project")
    sub.add_parser("mark").add_argument("name")
    sub.add_parser("report").add_argument("mode",choices=["seed","broad","scoped"])
    a=parser.parse_args()
    if a.command=="check": check(a.project)
    elif a.command=="mark": mark(a.name)
    else: report(a.mode)

if __name__=="__main__": main()
