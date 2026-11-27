"""Diagnostic registry export using a separate test-only mod; never release approval."""
import argparse
import json
import os
import re
from pathlib import Path
from .common import profile, qa_config, require, sha256, write_json
from .server import run
from .content_discovery import reconcile

PILOT="projects/fragileglass-fabric-1.21.1"
PROBE="minemods_registry_probe"
CONTROLS={"item":"minecraft:stone","block":"minecraft:stone","entity":"minecraft:pig",
          "effect":"minecraft:speed","menu":"minecraft:generic_9x1","block_entity":"minecraft:chest",
          "fluid":"minecraft:water","potion":"minecraft:water","sound":"minecraft:block.stone.break",
          "particle":"minecraft:flame","recipe":"minecraft:crafting_table"}
RID=re.compile(r"^[a-z0-9_.-]+:[a-z0-9_./-]+$")

def validate(doc, fixture=False):
    require(isinstance(doc,dict) and doc.get("schema")==1,"Bad runtime registry schema")
    rows=doc.get("registries")
    require(isinstance(rows,dict) and set(rows)==set(CONTROLS),"Missing or unknown runtime registry category")
    for kind, known in CONTROLS.items():
        ids=rows[kind]
        require(isinstance(ids,list) and 1<=len(ids)<=100000 and
                all(isinstance(i,str) and RID.fullmatch(i) for i in ids),"Invalid runtime IDs: "+kind)
        require(len(set(ids))==len(ids),"Duplicate runtime ID: "+kind)
        require(known in ids,"Missing independent vanilla control: "+known)
        require(PROBE+":nonexistent" not in ids,"Negative registry control failed")
    require((PROBE+":code_only" in rows["item"])==fixture,"Code-only registry control mismatch")
    return rows

def compare(static, live, namespace):
    hints={(r["kind"],r["id"]) for r in static["content"]}
    actual={(kind,ident) for kind,ids in live.items() for ident in ids if ident.split(":",1)[0]==namespace}
    candidates={key for key in hints if key[1].split(":",1)[0]==namespace and key[0] in live}
    return {"runtime_confirmed":[{"kind":k,"id":i,"resource_hint":(k,i) in hints} for k,i in sorted(actual)],
            "runtime_only":[{"kind":k,"id":i} for k,i in sorted(actual-hints)],
            "resource_hint_not_active":[{"kind":k,"id":i} for k,i in sorted(candidates-actual)]}

def main():
    parser=argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--jar",required=True,type=Path)
    parser.add_argument("--probe",required=True,type=Path)
    parser.add_argument("--out",required=True,type=Path)
    a=parser.parse_args()
    a.out.mkdir(parents=True,exist_ok=True)
    jar,probe=a.jar.resolve(),a.probe.resolve()
    p=profile(PILOT);q=qa_config(p)
    report={"schema":1,"kind":"runtime-registry-diagnostic","eligible":False,"status":"FAIL",
            "checks":[],"qa_level":0,"target":p["minecraft"]+"/"+p["loader"],
            "scope":"Separate diagnostic server with QA probe; never release evidence."}
    previous=os.environ.get("MINEMODS_REGISTRY_FIXTURE")
    try:
        require(jar.is_file() and probe.is_file() and jar.name!=probe.name,"Missing/distinct diagnostic JARs required")
        report.update(jar=jar.name,sha256=sha256(jar),probe_sha256=sha256(probe))
        static=reconcile(jar,p["slug"])
        probe_static=reconcile(probe,PROBE)
        write_json(a.out/"packaged-content.json",static)
        os.environ["MINEMODS_REGISTRY_FIXTURE"]="1"
        def collect(conn,port,proc,rep,work):
            raw=(work/"server"/"minemods-live-registries.json")
            require(raw.is_file() and raw.stat().st_size<=8*1024*1024,"Missing/oversize live export")
            live=validate(json.loads(raw.read_text()),fixture=True)
            result=compare(static,live,p["slug"])
            control=compare(probe_static,live,PROBE)
            require({"kind":"item","id":PROBE+":code_only"} in control["runtime_only"],
                    "Code-only item was not discovered independently of packaged resource hints")
            require(rep["installed_sha256"]==report["sha256"],"Target JAR provenance mismatch")
            require(rep["installed_jars"].get(probe.name)==report["probe_sha256"],"Probe JAR provenance mismatch")
            write_json(a.out/"live-registries.json",live)
            rep.update(inventory=result,registry_counts={k:len(v) for k,v in live.items()},
                       control="code-only item discovered",fixture_inventory=control)
        run(p,q,jar,a.out/"runtime",report,extra_jars=(probe,),after_suites=collect)
        report["status"]="PASS"
        return 0
    except Exception as e:
        report["error"]=f"{type(e).__name__}: {e}"
        print(report["error"],flush=True)
        return 1
    finally:
        if previous is None: os.environ.pop("MINEMODS_REGISTRY_FIXTURE",None)
        else: os.environ["MINEMODS_REGISTRY_FIXTURE"]=previous
        report["diagnostic_server_level"]=report.pop("qa_level",0)
        report["eligible"]=False
        write_json(a.out/"report.json",report)

if __name__=="__main__": raise SystemExit(main())
