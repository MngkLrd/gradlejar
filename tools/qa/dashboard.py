"""Generate an offline HTML/JSON dashboard from actual recent QA artifacts."""
import argparse
import html
import io
import json
import os
import subprocess
import zipfile
from pathlib import Path
from .common import ROOT, LEGACY, project_tree, write_json

def gh(*args):
    return subprocess.run(["gh","api",*args],check=True,capture_output=True,timeout=40).stdout

def github_reports(repo,max_pages=5,max_reports=100):
    reports=[];warnings=[]
    for page in range(1,max_pages+1):
        try:
            body=json.loads(gh(f"repos/{repo}/actions/artifacts?per_page=100&page={page}"))
        except (ValueError,OSError,subprocess.SubprocessError) as e:
            warnings.append("Artifact listing incomplete: "+str(e));break
        items=body.get("artifacts",[])
        for a in items:
            if a.get("expired") or not a.get("name","").startswith("qa-report-"): continue
            if a.get("size_in_bytes",0)>2*1024*1024:
                warnings.append("Oversized QA report artifact ignored");continue
            try:
                data=gh(f"repos/{repo}/actions/artifacts/{int(a['id'])}/zip")
                with zipfile.ZipFile(io.BytesIO(data)) as z:
                    for info in z.infolist():
                        if not info.filename.endswith(".json") or info.file_size>1024*1024: continue
                        r=json.loads(z.read(info))
                        if isinstance(r,dict) and r.get("schema")==1 and r.get("project"):
                            r["_artifact_created"]=a.get("created_at","")
                            reports.append(r)
            except (ValueError,KeyError,OSError,subprocess.SubprocessError,zipfile.BadZipFile) as e:
                warnings.append(f"Could not read report artifact {a.get('id')}: {e}")
            if len(reports)>=max_reports:
                warnings.append(f"Report history bounded to {max_reports} recent reports")
                return reports,warnings
        if len(items)<100: break
    else: warnings.append(f"Artifact search bounded to {max_pages} pages")
    return reports,warnings

def local_reports(path):
    result=[]
    if not path: return result
    for f in Path(path).rglob("*.json"):
        try:
            r=json.loads(f.read_text("utf-8"))
            if isinstance(r,dict) and r.get("schema")==1 and r.get("project"): result.append(r)
        except (OSError,ValueError): pass
    return result

def inventory(reports):
    latest={}
    def order(r):
        def number(key):
            try: return int(r.get(key,0))
            except (ValueError,TypeError): return 0
        return (number("run_id"),number("run_attempt"),r.get("_artifact_created",""))
    for r in reports:
        project=r["project"]
        if project not in latest or order(r)>order(latest[project]): latest[project]=r
    rows=[]
    for f in [ROOT/"build-profile.json",*sorted((ROOT/"projects").glob("*/build-profile.json"))]:
        project="." if f.parent==ROOT else f.parent.relative_to(ROOT).as_posix()
        try:
            p=json.loads(f.read_text("utf-8"))
            if not isinstance(p,dict): raise ValueError("Not an object")
            row={"project":project,"slug":p.get("slug","?"),"version":p.get("version","?"),"minecraft":p.get("minecraft","?"),"loader":p.get("loader","?"),"mode":(p.get("qa") or {}).get("mode","legacy" if project in LEGACY else "missing-policy"),"qa_level":None,"status":"no-recent-evidence"}
        except (OSError,ValueError,AttributeError) as e:
            rows.append({"project":project,"slug":"?","version":"?","minecraft":"?","loader":"?","mode":"invalid","status":"invalid-profile","error":str(e),"qa_level":None});continue
        if r:=latest.get(project):
            row.update(latest_version=r.get("version"),run_id=r.get("run_id"),run_attempt=r.get("run_attempt"),commit=r.get("commit"),latest_status=r.get("status"),latest_level=r.get("qa_level"),error=r.get("error"),diagnostics=r.get("diagnostics",[]))
            if r.get("version")==row["version"] and r.get("tree") and r.get("tree")==project_tree(project):
                row["qa_level"]=r.get("qa_level")
                row["status"]=r.get("status","unknown")
            else: row["status"]="stale-evidence"
        rows.append(row)
    return rows

def render(rows,warnings,repo):
    e=lambda x:html.escape(str(x if x is not None else ""),quote=True)
    def status(row):
        s=row["status"]
        return s, "ok" if s=="passed" else "bad" if s in ("failed","invalid-profile","build-failed-or-skipped") else "neutral"
    trs=[]
    for r in rows:
        s,cls=status(r)
        run=r.get("run_id","")
        link=f'<a href="https://github.com/{e(repo)}/actions/runs/{e(run)}">#{e(run)}</a>' if str(run).isdigit() else "—"
        detail=r.get("error") or "; ".join(d.get("summary","") for d in r.get("diagnostics",[])) or ""
        if s=="stale-evidence": detail="Last report: "+str(r.get("latest_status"))+" for "+str(r.get("latest_version"))
        trs.append(f'<tr data-status="{e(s)}"><td><b>{e(r["slug"])}</b><small>{e(r["project"])}</small></td><td>{e(r["version"])}</td><td>{e(r["minecraft"])}</td><td>{e(r["loader"])}</td><td>{e(r["mode"])}</td><td>{e("QA-"+str(r["qa_level"]) if r["qa_level"] is not None else "—")}</td><td><span class="{cls}">{e(s)}</span></td><td>{link}</td><td>{e(detail)}</td></tr>')
    counts={"Profiles":len(rows),"Current QA-4":sum(r["status"]=="passed" and r["qa_level"]==4 for r in rows),"Failed current":sum(r["status"] in ("failed","invalid-profile","build-failed-or-skipped") for r in rows),"Legacy policies":sum(r["mode"]=="legacy" for r in rows),"Unknown or stale":sum(r["status"] in ("no-recent-evidence","stale-evidence") for r in rows)}
    cards="".join(f'<div class="metric"><strong>{n}</strong><span>{e(k)}</span></div>' for k,n in counts.items())
    options="".join(f'<option value="{e(s)}">{e(s)}</option>' for s in sorted({r["status"] for r in rows}))
    return """<!doctype html><html lang="en"><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1"><title>MineMods QA dashboard</title><style>
    :root{color-scheme:dark;--bg:#111820;--panel:#1c2835;--line:#354658;--text:#e2eaf1;--muted:#aebdca}
    *{box-sizing:border-box}body{margin:0;background:var(--bg);color:var(--text);font:14px system-ui,sans-serif}main{max-width:1500px;margin:auto;padding:30px}h1{font-size:27px;margin:0 0 8px}p,small{color:var(--muted)}.metrics{display:flex;gap:12px;flex-wrap:wrap;margin:24px 0}.metric{background:var(--panel);border:1px solid var(--line);padding:16px;min-width:150px;flex:1;border-radius:8px}.metric strong{font-size:28px;display:block}.metric span{color:var(--muted)}.filters{display:flex;gap:10px;margin:16px 0;flex-wrap:wrap}input,select{background:var(--panel);border:1px solid var(--line);color:var(--text);padding:10px;border-radius:6px}input{flex:1;min-width:160px}.wrap{overflow:auto;border:1px solid var(--line);border-radius:7px}table{width:100%;border-collapse:collapse;min-width:940px}th,td{padding:11px;text-align:left;border-bottom:1px solid var(--line);vertical-align:top}th{background:var(--panel);position:sticky;top:0}td small{display:block;font-size:11px;max-width:270px;overflow-wrap:anywhere}a{color:#85c4ff}.ok{color:#9ce1ae}.bad{color:#ffaeae}.neutral{color:#c7cbd1}ul{color:var(--muted)}
    </style><main><h1>MineMods · Build and QA status</h1><p>Actual recent GitHub Actions evidence, not inferred gameplay results. A changed source tree invalidates earlier evidence. Unknown is not a failure.</p>"""+'<div class="metrics">'+cards+'</div><div class="filters"><input id="search" placeholder="Search mod, loader or version"><select id="status"><option value="">All statuses</option>'+options+'</select></div><div class="wrap"><table><thead><tr><th>Mod / project</th><th>Version</th><th>Minecraft</th><th>Loader</th><th>Policy</th><th>Current QA</th><th>Status</th><th>Run</th><th>Detail</th></tr></thead><tbody>'+"".join(trs)+"""</tbody></table></div><p>QA-4 means production server gameplay assertions; client visuals remain unverified. Legacy profiles are explicitly not runtime-certified.</p><ul>"""+"".join("<li>"+e(w)+"</li>" for w in warnings)+"""</ul></main><script>
    const search=document.querySelector('#search'), status=document.querySelector('#status');
    function filter(){const q=search.value.toLowerCase();for(const row of document.querySelectorAll('tbody tr'))row.hidden=!(row.textContent.toLowerCase().includes(q)&&(!status.value||row.dataset.status===status.value));}
    search.addEventListener('input',filter);status.addEventListener('change',filter);
    </script></html>"""

if __name__=="__main__":
    ap=argparse.ArgumentParser()
    ap.add_argument("--reports");ap.add_argument("--github",action="store_true");ap.add_argument("--out",required=True)
    ap.add_argument("--repo",default=os.getenv("GITHUB_REPOSITORY","MngkLrd/gradlejar"))
    a=ap.parse_args()
    reports=local_reports(a.reports);warnings=[]
    if a.github:
        recent,warnings=github_reports(a.repo);reports+=recent
    else: warnings.append("Local report mode; no GitHub artifact history queried")
    rows=inventory(reports);out=Path(a.out);out.mkdir(parents=True,exist_ok=True)
    write_json(out/"dashboard.json",{"profiles":rows,"warnings":warnings})
    (out/"index.html").write_text(render(rows,warnings,a.repo),encoding="utf-8")
    print("Dashboard:",len(rows),"profiles,",len(reports),"reports")
