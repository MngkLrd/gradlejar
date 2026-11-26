"""P7: bounded batch selection, version discovery, and build-minute planning."""
import argparse
import json
import os
import re
import statistics
import urllib.request
import xml.etree.ElementTree as ET
from collections import defaultdict
from datetime import datetime
from pathlib import Path
from .common import ROOT,REGISTRY,SLUG,target,require,write_json
from .dashboard import gh,github_reports,inventory

def batch_select(profiles,slugs,limit=48):
    require(1<=limit<=64,"Batch limit must be 1..64")
    require(slugs and len(slugs)==len(set(slugs)) and all(SLUG.fullmatch(s) for s in slugs),"Invalid/duplicate batch slug")
    selected=[(d,p) for d,p in profiles if p.get("slug") in slugs and isinstance(p.get("qa"),dict) and not p.get("draft")]
    require(set(slugs)=={p["slug"] for _,p in selected},"Some requested mods have no enrolled QA profiles")
    require(len(selected)<=limit,"Batch exceeds runner budget; split into smaller complete cohorts")
    groups=defaultdict(list)
    for d,p in selected:groups[(p["slug"],p["version"])].append((d,p))
    for key,rows in groups.items():
        expected=set(rows[0][1]["qa"]["release_targets"])
        require(len(rows)==len(expected) and {target(p) for _,p in rows}==expected and all(set(p["qa"]["release_targets"])==expected for _,p in rows),"Incomplete enrolled cohort: "+str(key))
    return sorted(d for d,p in selected)

def fetch(url):
    with urllib.request.urlopen(urllib.request.Request(url,headers={"User-Agent":"MineMods-QA/2.0"}),timeout=25) as r:return r.read()

def release_candidates(manifest,registered,limit=40):
    releases=[x for x in manifest["versions"] if x["type"]=="release"]
    return releases[:limit]

def discover(limit=40):
    warnings=[]
    manifest=json.loads(fetch("https://piston-meta.mojang.com/mc/game/version_manifest_v2.json"))
    fabric_games=set();fabric_loader=[];forge={};neo=[]
    for source,url,parser in [
        ("fabric game","https://meta.fabricmc.net/v2/versions/game",lambda b:{x["version"] for x in json.loads(b) if x.get("stable")}),
        ("fabric loader","https://meta.fabricmc.net/v2/versions/loader",lambda b:[x["version"] for x in json.loads(b) if x.get("stable")]),
        ("forge","https://files.minecraftforge.net/net/minecraftforge/forge/promotions_slim.json",lambda b:json.loads(b)["promos"]),
        ("neoforge","https://maven.neoforged.net/releases/net/neoforged/neoforge/maven-metadata.xml",lambda b:[v.text for v in ET.fromstring(b).findall(".//version") if v.text])
    ]:
        try:
            value=parser(fetch(url))
            if source=="fabric game":fabric_games=value
            elif source=="fabric loader":fabric_loader=value
            elif source=="forge":forge=value
            else:neo=value
        except Exception as e:warnings.append(source+" unavailable: "+str(e))
    rows=[]
    for item in release_candidates(manifest,REGISTRY["minecraft"],limit):
        v=item["id"];installed=REGISTRY["minecraft"].get(v,{})
        parts=v.split(".");prefix=None
        if all(x.isdigit() for x in parts):
            if parts[0]=="1" and int(parts[1])>=20:
                prefix=parts[1]+"."+(parts[2] if len(parts)>2 else "0")+"."
            elif int(parts[0])>=26:
                prefix=v+("." if len(parts)>=3 else ".0.")
        builds=[x for x in neo if prefix and x.startswith(prefix) and re.fullmatch(r"\d+(?:\.\d+)+",x)]
        builds.sort(key=lambda x:tuple(map(int,x.split("."))))
        loaders={}
        if v in fabric_games:loaders["fabric"]={"status":"game-listed","latest_stable_loader":fabric_loader[0] if fabric_loader else None,"pair_requires_validation":True}
        fv=forge.get(v+"-recommended") or forge.get(v+"-latest")
        if fv:loaders["forge"]={"status":"promotion-listed","version":fv}
        if builds:loaders["neoforge"]={"status":"maven-prefix-match","version":builds[-1],"pair_requires_validation":True}
        rows.append({"minecraft":v,"release_time":item.get("releaseTime"),"registered":bool(installed),"registry_loaders":list(installed.get("loaders",{})),"candidates":loaders,"status":"registered-baseline" if installed else "discovered-not-ported"})
    return {"schema":1,"latest_release":manifest.get("latest",{}).get("release"),"versions":rows,"warnings":warnings,"scope":"Discovery only. No automatic registry edit, port, upgrade or certification."}

def job_minutes(repo):
    rows=[];warnings=[]
    try:
        runs=json.loads(gh(f"repos/{repo}/actions/workflows/build.yml/runs?status=completed&per_page=8"))["workflow_runs"]
        for r in runs:
            jobs=json.loads(gh(f"repos/{repo}/actions/runs/{r['id']}/jobs?per_page=100"))["jobs"]
            for j in jobs:
                if not j.get("started_at") or not j.get("completed_at") or not j["name"].startswith("build ("):continue
                mins=(datetime.fromisoformat(j["completed_at"].replace("Z","+00:00"))-datetime.fromisoformat(j["started_at"].replace("Z","+00:00"))).total_seconds()/60
                if mins>0:rows.append({"run":r["id"],"job":j["name"],"minutes":round(mins,2),"conclusion":j["conclusion"]})
    except Exception as e:warnings.append("Job timing unavailable: "+str(e))
    return {"observations":rows,"median_build_job_minutes":round(statistics.median([r["minutes"] for r in rows]),2) if rows else None,"warnings":warnings,"scope":"Observed runner job duration, not billed cost; bounded recent sample"}

def main():
    ap=argparse.ArgumentParser();ap.add_argument("--out",required=True);ap.add_argument("--repo",default=os.getenv("GITHUB_REPOSITORY","MngkLrd/gradlejar"));a=ap.parse_args()
    out=Path(a.out);out.mkdir(parents=True,exist_ok=True)
    try:versions=discover()
    except Exception as e:versions={"schema":1,"versions":[],"warnings":[str(e)],"status":"discovery-failed"}
    write_json(out/"version-discovery.json",versions)
    timings=job_minutes(a.repo);write_json(out/"runner-minutes.json",timings)
    reports,w=github_reports(a.repo,max_pages=5,max_reports=100)
    rows=inventory(reports);profiles=[]
    for f in [ROOT/"build-profile.json",*sorted((ROOT/"projects").glob("*/build-profile.json"))]:
        try:p=json.loads(f.read_text());d="." if f.parent==ROOT else f.parent.relative_to(ROOT).as_posix();profiles.append((d,p))
        except (OSError,ValueError):continue
    groups=defaultdict(list)
    for d,p in profiles:
        if isinstance(p.get("qa"),dict) and not p.get("draft"):groups[p["slug"]].append((d,p))
    queue=[]
    bypath={r["project"]:r for r in rows}
    for slug,ps in sorted(groups.items()):
        try:ds=batch_select(profiles,[slug]);complete=True;error=None
        except ValueError as e:ds=[d for d,p in ps];complete=False;error=str(e)
        stale=sum(bypath.get(d,{}).get("status") not in ("passed","manual-client-approved") for d in ds)
        median=timings["median_build_job_minutes"]
        queue.append({"slug":slug,"projects":ds,"complete_cohorts":complete,"error":error,"needs_qa":stale,"estimated_runner_minutes":round(len(ds)*median,1) if median is not None else None,"dispatch":"@batch:"+slug if complete else None})
    queue.sort(key=lambda r:(not r["complete_cohorts"],-r["needs_qa"],r["slug"]))
    write_json(out/"control-center.json",{"schema":1,"batch_limit":48,"parallel_limit":6,"queue":queue,"warnings":w,"scope":"Plan only; no unattended build or release dispatch"})
    print("Control Center:",len(versions.get("versions",[])),"Minecraft releases,",len(queue),"enrolled mods")
if __name__=="__main__":main()
