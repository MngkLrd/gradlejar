"""P3: build, stage and jointly test exact same-commit mod JARs; never release by implication."""
import argparse
import json
import os
import re
import shutil
import zipfile
from pathlib import Path
from .common import ROOT, REGISTRY, SLUG, VERSION, require, profile, safe_path, target, sha256, write_json, project_tree
from .server import scenario_steps, run
from .crash import analyze

def load_pack(name):
    require(isinstance(name,str) and SLUG.fullmatch(name),"Invalid pack ID")
    pack=json.loads((ROOT/"tools/qa/packs"/(name+".json")).read_text("utf-8"))
    require(pack.get("schema")==1 and pack.get("id")==name,"Invalid pack manifest")
    targets=pack.get("targets")
    require(isinstance(targets,list) and 1<=len(targets)<=20,"Pack needs 1..20 targets")
    seen=set();result=[]
    for t in targets:
        mc,l=t.get("minecraft"),t.get("loader")
        key=l+"-mc"+mc if isinstance(l,str) and isinstance(mc,str) else ""
        require(t.get("id")==key and key not in seen,"Invalid or duplicate target")
        seen.add(key)
        reg=REGISTRY["minecraft"].get(mc,{})
        require(l in reg.get("loaders",{}),"Unsupported Minecraft/loader pair: "+key)
        require(isinstance(t.get("loader_version"),str) and VERSION.fullmatch(t["loader_version"]),"Exact loader version required")
        members=t.get("members")
        require(isinstance(members,list) and 2<=len(members)<=30,"Pack must contain 2..30 mod projects")
        rows=[];slugs=set();paths=set()
        for m in members:
            p=profile(m["project"])
            require(target(p)==key,"Mixed Minecraft/loader versions: "+m["project"])
            require(p["slug"] not in slugs and m["project"] not in paths,"Duplicate mod or project")
            slugs.add(p["slug"]);paths.add(m["project"])
            require((p.get("qa") or {}).get("mode")!="client","Client-only mod cannot be tested as a server pack")
            scenario=m.get("scenario")
            require(isinstance(scenario,str),"Each member needs its own gameplay scenario")
            scenario_steps(json.loads(safe_path(ROOT,scenario).read_text("utf-8")),l)
            rows.append({"project":m["project"],"slug":p["slug"],"version":p["version"],"scenario":scenario,
                         "key":key+"--"+p["slug"],"target":key,"java":p["java"],"gradle":p["gradle"]})
        deps=t.get("dependencies",[])
        require(isinstance(deps,list),"Invalid dependency array")
        names=set()
        for d in deps:
            require(isinstance(d,dict) and re.fullmatch(r"[A-Za-z0-9_.-]+\.jar",d.get("name","")),"Invalid dependency name")
            require(d["name"] not in names,"Duplicate dependency filename")
            names.add(d["name"])
            require(re.fullmatch(r"[a-f0-9]{64}",d.get("sha256","")) and d.get("url","").startswith("https://"),"External dependency must pin HTTPS artifact and SHA-256")
        result.append(dict(t,members=rows,java=str(reg["java"]),dependencies=deps))
    require(sum(len(t["members"]) for t in result)<=256,"Member matrix exceeds GitHub Actions limit")
    return result

def plan(name):
    targets=load_pack(name)
    members=[m for t in targets for m in t["members"]]
    with open(os.environ["GITHUB_OUTPUT"],"a",encoding="utf-8") as f:
        f.write("members="+json.dumps(members,separators=(",",":"))+"\n")
        f.write("targets="+json.dumps([{"id":t["id"],"java":t["java"]} for t in targets],separators=(",",":"))+"\n")
    print("Compatibility matrix:",len(targets),"targets,",len(members),"build jobs")

def stage(name,key,jar,out):
    matches=[m for t in load_pack(name) for m in t["members"] if m["key"]==key]
    require(len(matches)==1,"Unknown member key")
    m=matches[0];p=profile(m["project"]);jar=Path(jar).resolve()
    require(jar.is_file() and jar.parent==(ROOT/m["project"]/"build/libs").resolve(),"Only actual Gradle build output may be staged")
    require(not jar.name.endswith(("-sources.jar","-dev.jar","-javadoc.jar")) and jar.name.endswith(".jar"),"Not production JAR")
    require(zipfile.is_zipfile(jar),"Invalid JAR")
    with zipfile.ZipFile(jar) as z:
        require(z.testzip() is None,"Corrupt member JAR")
        if p["loader"]=="fabric":
            meta=json.loads(z.read("fabric.mod.json"))
            require(meta.get("environment")!="client","Client-only metadata in server pack")
            require(meta.get("id")==p["slug"] and meta.get("version")==p["version"],"Member metadata differs from profile")
    dest=Path(out);dest.mkdir(parents=True,exist_ok=True)
    shutil.copy2(jar,dest/jar.name)
    write_json(dest/"member.json",dict(m,jar=jar.name,sha256=sha256(jar),tree=project_tree(m["project"]),commit=os.environ["GITHUB_SHA"],run_id=os.environ["GITHUB_RUN_ID"],run_attempt=os.environ["GITHUB_RUN_ATTEMPT"]))

def resolve_members(t,inputs,commit,run_id,attempt):
    found={}
    for f in Path(inputs).rglob("member.json"):
        r=json.loads(f.read_text("utf-8"))
        require(r.get("key") not in found,"Duplicate member evidence")
        found[r.get("key")]=(r,f.parent)
    expected={m["key"] for m in t["members"]}
    require(set(found)==expected,"Missing or unexpected staged pack members")
    jars=[];evidence=[];names=set()
    for m in t["members"]:
        r,d=found[m["key"]]
        require(all(r.get(k)==m[k] for k in ("project","slug","version","key","target","scenario")),"Member profile mismatch")
        require(r.get("commit")==commit and str(r.get("run_id"))==str(run_id) and str(r.get("run_attempt"))==str(attempt),"Mixed commit/run member provenance")
        require(r.get("tree") and r["tree"]==project_tree(m["project"]),"Member source tree differs")
        name=r.get("jar","")
        require(re.fullmatch(r"[A-Za-z0-9_.+-]+\.jar",name) and name not in names,"Invalid/duplicate JAR name")
        names.add(name);jar=d/name
        require(jar.is_file() and sha256(jar)==r.get("sha256"),"Staged member SHA-256 mismatch: "+name)
        jars.append(jar.resolve());evidence.append(r)
    return jars,evidence

def test(name,key,inputs,out):
    out=Path(out);out.mkdir(parents=True,exist_ok=True)
    report={"schema":1,"kind":"compatibility","pack":name,"target":key,"commit":os.getenv("GITHUB_SHA",""),"run_id":os.getenv("GITHUB_RUN_ID",""),"run_attempt":os.getenv("GITHUB_RUN_ATTEMPT",""),"status":"failed","qa_level":0,"eligible":False,"checks":[]}
    try:
        ts=[t for t in load_pack(name) if t["id"]==key]
        require(len(ts)==1,"Unknown pack target")
        t=ts[0]
        jars,evidence=resolve_members(t,inputs,report["commit"],report["run_id"],report["run_attempt"])
        report["members"]=evidence
        p=profile(t["members"][0]["project"])
        q={"loader_version":t["loader_version"],"dependencies":t["dependencies"]}
        report["loader_version"]=q["loader_version"]
        report["external_dependencies"]=t["dependencies"]
        scenarios=[(m["slug"],m["scenario"]) for m in t["members"]]
        run(p,q,jars[0],out/"runtime",report,extra_jars=jars[1:],scenarios=scenarios)
        for m in evidence:
            require(report["installed_jars"].get(m["jar"])==m["sha256"],"Installed pack member hash mismatch")
            require(any(c.get("suite")==m["slug"] and c.get("assertion") and c.get("passed") for c in report["checks"]),"No successful gameplay assertion for "+m["slug"])
        require(report["qa_level"]==4,"Server compatibility scenario incomplete")
        report.update(status="passed",scope="exact-combination-only",eligible=False)
        return 0
    except Exception as e:
        report["error"]=f"{type(e).__name__}: {e}"
        logs=[p.read_text(errors="replace")[-500000:] for p in (out/"runtime/logs").glob("*.log")]
        report["diagnostics"]=analyze("\n".join([report["error"],*logs]))
        print("PACK FAILED:",report["error"],flush=True)
        return 1
    finally:
        write_json(out/"compatibility-report.json",report)

def main():
    ap=argparse.ArgumentParser()
    sub=ap.add_subparsers(dest="action",required=True)
    a=sub.add_parser("plan");a.add_argument("pack")
    a=sub.add_parser("stage");a.add_argument("pack");a.add_argument("key");a.add_argument("jar");a.add_argument("out")
    a=sub.add_parser("test");a.add_argument("pack");a.add_argument("key");a.add_argument("inputs");a.add_argument("out")
    x=ap.parse_args()
    if x.action=="plan":plan(x.pack)
    elif x.action=="stage":stage(x.pack,x.key,x.jar,x.out)
    else:return test(x.pack,x.key,x.inputs,x.out)
    return 0

if __name__=="__main__":raise SystemExit(main())
