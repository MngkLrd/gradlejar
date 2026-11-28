"""Materialize SHA-pinned cross-run Actions JAR dependencies into the existing QA cache.
Opt-in only; non-release candidates are allowed only on PR diagnostic runs."""
import hashlib
import io
import json
import os
import subprocess
import sys
import zipfile
from pathlib import Path

def digest(data):
    return hashlib.sha256(data).hexdigest()

def extract_jar(raw, dep):
    with zipfile.ZipFile(io.BytesIO(raw)) as z:
        names=[n for n in z.namelist() if Path(n).name==dep["name"] and not n.endswith("/")]
        if len(names)!=1:
            raise ValueError("Artifact must contain exactly one named dependency JAR")
        jar=z.read(names[0])
    if digest(jar)!=dep["sha256"] or not zipfile.is_zipfile(io.BytesIO(jar)):
        raise ValueError("Dependency JAR checksum or format mismatch")
    return jar

def api(path):
    return subprocess.check_output(["gh","api",path])

def restore(project):
    profile=json.loads((Path(project)/"build-profile.json").read_text())
    deps=[d for d in profile.get("qa",{}).get("dependencies",[]) if "actions_artifact" in d]
    if not deps:
        return
    repo=os.environ["GITHUB_REPOSITORY"]
    cache=Path(os.environ["RUNNER_TEMP"])/"minemods-pinned-deps"
    cache.mkdir(parents=True,exist_ok=True)
    for dep in deps:
        pin=dep["actions_artifact"]
        ident=int(pin["id"])
        path=f"repos/{repo}/actions/artifacts/{ident}"
        if dep["url"]!=f"https://api.github.com/{path}/zip":
            raise ValueError("Artifact URL does not match pinned repository/id")
        info=json.loads(api(path))
        run=info.get("workflow_run") or {}
        if info["id"]!=ident or info["expired"] or run.get("id")!=pin["run_id"] or run.get("head_sha")!=pin["head_sha"]:
            raise ValueError("Artifact provenance mismatch")
        if info["name"]!=pin["name"]:
            raise ValueError("Artifact name mismatch")
        # Never use an unapproved dependency for main/release builds.
        if "NOT-RELEASE" in info["name"] and os.environ.get("GITHUB_EVENT_NAME")!="pull_request":
            raise ValueError("Unapproved dependency allowed only for PR diagnostic testing")
        raw=api(path+"/zip")
        if info.get("digest") and info["digest"]!="sha256:"+digest(raw):
            raise ValueError("Artifact ZIP digest mismatch")
        jar=extract_jar(raw,dep)
        (cache/dep["sha256"]).write_bytes(jar)
        print("Exact dependency:",dep["name"],dep["sha256"],"artifact",ident,flush=True)
    with open(os.environ["GITHUB_ENV"],"a",encoding="utf-8") as f:
        f.write("MINEMODS_DEP_CACHE="+str(cache)+"\n")

if __name__=="__main__":
    restore(sys.argv[1])
