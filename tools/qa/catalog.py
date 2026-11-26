"""Bounded real release history, technical debt and migration priorities."""
import argparse
import json
import os
from collections import Counter,defaultdict
from pathlib import Path
from .common import ROOT,REGISTRY,LEGACY,write_json,require
from .dashboard import github_reports,local_reports,inventory,gh

def release_history(repo,pages=5):
    result=[];warnings=[]
    for page in range(1,pages+1):
        try:items=json.loads(gh(f"repos/{repo}/releases?per_page=100&page={page}"))
        except Exception as e:warnings.append("Release history unavailable: "+str(e));break
        for r in items:
            result.append({"id":r["id"],"tag":r["tag_name"],"name":r.get("name"),"published_at":r.get("published_at"),"draft":r.get("draft"),"prerelease":r.get("prerelease"),"url":r.get("html_url"),"assets":[{"name":a["name"],"size":a.get("size"),"digest":a.get("digest"),"url":a.get("browser_download_url")} for a in r.get("assets",[])],"qa_status":"not inferred from release existence"})
        if len(items)<100:break
    else:warnings.append("Release history bounded to "+str(pages*100)+" recent releases")
    return result,warnings

def debt(rows,incidents):
    result=[]
    for r in rows:
        reasons=[]
        if r["mode"]=="legacy":reasons.append("legacy-static-policy")
        if r["mode"] in ("invalid","missing-policy"):reasons.append("missing-or-invalid-policy")
        if r["status"] in ("no-recent-evidence","stale-evidence"):reasons.append(r["status"])
        if r["status"] in ("failed","build-failed-or-skipped","invalid-profile"):reasons.append("latest-check-failed")
        if r["mode"]=="client" and r["status"]!="manual-client-approved":reasons.append("client-review-missing")
        known=[x["id"] for x in incidents if r["project"] in x.get("projects",[]) and x.get("status")=="open"]
        if known:reasons.append("confirmed-open-regression")
        if reasons:
            priority=0 if known or "latest-check-failed" in reasons else 1 if r["mode"] in ("legacy","missing-policy","invalid") else 2
            result.append({"project":r["project"],"slug":r["slug"],"minecraft":r["minecraft"],"loader":r["loader"],"priority":priority,"reasons":reasons,"open_incidents":known,"status":r["status"]})
    return sorted(result,key=lambda r:(r["priority"],r["slug"],r["project"]))

def migration_queue(rows,debts):
    byslug=defaultdict(list)
    for r in debts:
        if "legacy-static-policy" in r["reasons"] or "missing-or-invalid-policy" in r["reasons"]:byslug[r["slug"]].append(r)
    result=[]
    for slug,rs in byslug.items():
        ports=[]
        for r in rs:
            root=ROOT/r["project"]
            direct=(root/"src/main/java").is_dir()
            valid=r["loader"] in REGISTRY["minecraft"].get(r["minecraft"],{}).get("loaders",{})
            ports.append({"project":r["project"],"target":r["loader"]+"-mc"+r["minecraft"],"direct_java":direct,"registry_supported":valid,"next":"review gameplay and create explicit regression scenario" if direct and valid else "inspect source restoration and loader support first"})
        result.append({"slug":slug,"priority":min(r["priority"] for r in rs),"legacy_ports":len(rs),"ports":ports,"status":"migration-planned-not-approved"})
    return sorted(result,key=lambda r:(r["priority"],-r["legacy_ports"],r["slug"]))

def main():
    p=argparse.ArgumentParser();p.add_argument("--out",required=True);p.add_argument("--github",action="store_true");p.add_argument("--reports");p.add_argument("--repo",default=os.getenv("GITHUB_REPOSITORY","MngkLrd/gradlejar"));a=p.parse_args()
    reports=local_reports(a.reports);warnings=[];releases=[]
    if a.github:
        extra,w=github_reports(a.repo,max_pages=10,max_reports=200);reports+=extra;warnings+=w
        releases,w=release_history(a.repo);warnings+=w
    else:warnings.append("Offline mode; no live release history")
    rows=inventory(reports)
    incidents=json.loads((ROOT/"tools/qa/quality-incidents.json").read_text())["incidents"]
    debts=debt(rows,incidents);queue=migration_queue(rows,debts)
    out=Path(a.out);out.mkdir(parents=True,exist_ok=True)
    write_json(out/"quality-debt.json",{"schema":1,"profiles":len(rows),"debt_profiles":len(debts),"reasons":dict(Counter(reason for d in debts for reason in d["reasons"])),"items":debts,"warnings":warnings})
    write_json(out/"migration-queue.json",{"schema":1,"cohorts":queue,"scope":"Planning only; do not remove legacy exceptions or approve a release without real per-port QA"})
    write_json(out/"release-history.json",{"schema":1,"releases":releases,"qa_evidence":[r for r in reports if r.get("project") and r.get("eligible") is True and r.get("qa_level") in (4,5)],"warnings":warnings})
    print("Catalog:",len(rows),"profiles;",len(debts),"with debt;",len(queue),"migration cohorts;",len(releases),"release records")
if __name__=="__main__":main()
