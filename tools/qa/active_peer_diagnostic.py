import hashlib, json, pathlib, subprocess, time, zipfile
from tools.qa import server

root=pathlib.Path("active-peer-candidates")
root.mkdir(exist_ok=True)
pins=json.loads(pathlib.Path("tools/qa/active-peer-diagnostic-pins.json").read_text())
report={"kind":"worldlogic-current-six-peer-diagnostic","eligible":False,"status":"failed","checks":[],"pins":pins,"installed_evidence":{}}
def gh(*args):
    return subprocess.check_output(["gh","api",*args])
import os
repo=os.environ["GITHUB_REPOSITORY"]
def sha(p):
    return hashlib.sha256(p.read_bytes()).hexdigest()
def artifact(aid,head,digest,name):
    meta=json.loads(gh(f"repos/{repo}/actions/artifacts/{aid}"))
    assert meta["workflow_run"]["head_sha"]==head and meta["digest"]=="sha256:"+digest and not meta["expired"],meta
    data=gh(f"repos/{repo}/actions/artifacts/{aid}/zip")
    assert hashlib.sha256(data).hexdigest()==digest,"artifact ZIP digest mismatch"
    path=root/(str(aid)+".zip")
    path.write_bytes(data)
    with zipfile.ZipFile(path) as z:
        matches=[n for n in z.namelist() if pathlib.PurePosixPath(n).name==name]
        assert len(matches)==1,(name,matches)
        dest=root/name
        dest.write_bytes(z.read(matches[0]))
    report["installed_evidence"][name]={"source_commit":head,"artifact":aid,"zip_sha256":digest,"jar_sha256":sha(dest)}
    return dest

try:
    core=pins["core"]
    for _ in range(90):
        run=json.loads(gh(f"repos/{repo}/actions/runs/{core['run']}"))
        assert run["head_sha"]==core["head"]
        if run["status"]=="completed":break
        time.sleep(10)
    assert run["status"]=="completed" and run["conclusion"]=="success",run["conclusion"]
    artifacts=json.loads(gh(f"repos/{repo}/actions/runs/{core['run']}/artifacts"))["artifacts"]
    a=next(x for x in artifacts if x["name"]=="jar-worldlogic-1.21.1-neoforge-1.5.2-0")
    corejar=artifact(a["id"],core["head"],a["digest"].removeprefix("sha256:"),core["jar"])
    qa=next(x for x in artifacts if x["name"]=="qa-report-0-1")
    qa_data=gh(f"repos/{repo}/actions/artifacts/{qa['id']}/zip")
    assert hashlib.sha256(qa_data).hexdigest()==qa["digest"].removeprefix("sha256:")
    import io
    with zipfile.ZipFile(io.BytesIO(qa_data)) as z:
        meta=json.loads(z.read(next(n for n in z.namelist() if pathlib.PurePosixPath(n).name=="qa-report-0.json")))
    assert meta["eligible"] and meta["jar"]==corejar.name and meta["sha256"]==sha(corejar)
    jars=[artifact(p["id"],p["head"],p["zip_sha256"],p["jar"]) for p in pins["peers"]]
    profile=json.loads(pathlib.Path("projects/worldlogic-1.21.1-neoforge/build-profile.json").read_text())
    server.run(profile,profile["qa"],corejar,"active-peer-runtime",report,extra_jars=jars,scenarios=[("current-six-peers","tools/qa/scenarios/worldlogic-active-peer-diagnostic.json")])
    report["status"]="passed-diagnostic-only"
finally:
    pathlib.Path("active-peer-report.json").write_text(json.dumps(report,indent=2))
