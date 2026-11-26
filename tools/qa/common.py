"""Shared strict profile, paths, and reports. No external Python dependencies."""
import hashlib
import json
import os
import re
import subprocess
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
HERE = Path(__file__).resolve().parent
REGISTRY = json.loads((HERE / "compatibility.json").read_text())
LEGACY = set(json.loads((HERE / "legacy-projects.json").read_text())["projects"])
VERSION = re.compile(r"^[0-9]+(?:\.[0-9]+){1,4}$")
SLUG = re.compile(r"^[a-z][a-z0-9_-]{0,60}$")
SAFE_VERSION = re.compile(r"^[0-9A-Za-z][0-9A-Za-z._-]{0,63}$")

def require(ok, message):
    if not ok:
        raise ValueError(message)

def write_json(path, value):
    path = Path(path)
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(value, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")

def sha256(path):
    with Path(path).open("rb") as f:
        return hashlib.file_digest(f, "sha256").hexdigest()

def relative_project(project):
    require(project == "." or re.fullmatch(r"projects/[a-z][a-z0-9_.-]{0,60}", project), "Invalid project path")
    path = (ROOT / project).resolve()
    require(path.is_relative_to(ROOT), "Project escapes repository")
    return path

def safe_path(root, name):
    require(isinstance(name, str) and name and not Path(name).is_absolute(), "Expected relative path")
    p = (root / name).resolve()
    require(p.is_relative_to(root.resolve()), "Path escapes its root: " + name)
    return p

def profile(project):
    base = relative_project(project)
    p = json.loads((base / "build-profile.json").read_text("utf-8"))
    require(isinstance(p, dict), "Profile must be an object")
    require(all(isinstance(p.get(k), str) for k in ("slug","version","minecraft","loader","java","gradle","jar_glob")), "Invalid profile fields")
    require(SLUG.fullmatch(p["slug"]) and SAFE_VERSION.fullmatch(p["version"]), "Unsafe slug/version")
    require(p["loader"] in ("forge","neoforge","fabric") and VERSION.fullmatch(p["minecraft"]), "Invalid loader or Minecraft version")
    p["project_dir"] = project
    return p

def compatibility(p):
    mc = REGISTRY["minecraft"].get(p["minecraft"])
    require(mc, "Minecraft version missing from compatibility registry: " + p["minecraft"])
    require(p["loader"] in mc["loaders"], "Unsupported loader/Minecraft pair")
    return mc, mc["loaders"][p["loader"]]

def qa_config(p):
    require(not p.get("draft"), "Draft scaffold cannot be enrolled or released")
    q = p.get("qa")
    if q is None:
        require(p["project_dir"] in LEGACY, "New project requires qa configuration; see AGENTS.md")
        return {"mode":"legacy"}
    require(isinstance(q, dict) and q.get("mode") in ("server","client"), "qa.mode must be server or client")
    mc, loader = compatibility(p)
    require(int(p["java"]) >= mc["java"], "Build Java is older than runtime Java")
    require(VERSION.fullmatch(q.get("loader_version", "")), "qa.loader_version must pin an exact installed loader version")
    require(q.get("release_targets"), "qa.release_targets must declare complete release matrix")
    targets=q["release_targets"]
    require(isinstance(targets,list) and len(targets)==len(set(targets)), "Duplicate/invalid release targets")
    expected={(l+"-mc"+v) for v,m in REGISTRY["minecraft"].items() for l in m["loaders"]}
    require(set(targets)<=expected and target(p) in targets, "Invalid/missing release target")
    require(p["jar_glob"]==filename(p), "QA projects must use exact standard release JAR name")
    if q["mode"]=="server":
        require(isinstance(q.get("scenario"),str), "Gameplay server mod needs a scenario")
        require(safe_path(ROOT,q["scenario"]).is_file(), "Missing scenario")
    for item in q.get("dependencies",[]):
        require(isinstance(item,dict) and re.fullmatch(r"[A-Za-z0-9_.-]+\.jar",item.get("name","")), "Bad dependency name")
        require(re.fullmatch(r"[a-f0-9]{64}",item.get("sha256","")), "Dependencies must be SHA-256 pinned")
        require(item.get("url","").startswith("https://"),"Dependencies require HTTPS")
    return q

def target(p):
    return p["loader"]+"-mc"+p["minecraft"]

def filename(p):
    return p["slug"]+"-"+target(p)+"-"+p["version"]+".jar"

def project_tree(project):
    spec="HEAD^{tree}" if project=="." else "HEAD:"+project
    return subprocess.run(["git","rev-parse",spec],cwd=ROOT,check=False,capture_output=True,text=True).stdout.strip()

def report_for(p):
    return {"schema":1,"project":p["project_dir"],"slug":p["slug"],"version":p["version"],"minecraft":p["minecraft"],"loader":p["loader"],"target":target(p),"commit":os.getenv("GITHUB_SHA",""),"tree":project_tree(p["project_dir"]),"run_id":os.getenv("GITHUB_RUN_ID",""),"run_attempt":os.getenv("GITHUB_RUN_ATTEMPT",""),"qa_level":0,"status":"pending","eligible":False,"checks":[]}
