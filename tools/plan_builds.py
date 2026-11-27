"""Select every affected mod across the push range, not just the last commit."""
import fnmatch
import json
import os
import re
import subprocess
import sys
from pathlib import Path

ROOT=Path(__file__).resolve().parents[1]

def git(*args,timeout=60):
    return subprocess.run(["git",*args],cwd=ROOT,capture_output=True,text=True,check=False,timeout=timeout)

def event_payload():
    try: return json.loads(Path(os.environ["GITHUB_EVENT_PATH"]).read_text("utf-8"))
    except (KeyError,OSError,ValueError): return {}

def diff(base,head="HEAD"):
    r=git("diff","--name-only","-z",base,head)
    return r.stdout.split("\0")[:-1] if r.returncode==0 else None

def changed_paths_from_push():
    event=os.getenv("GITHUB_EVENT_NAME","")
    if event not in ("push","pull_request"): return None
    payload=event_payload()
    if event=="pull_request":
        # actions/checkout checks out the test merge commit by default.
        result=diff("HEAD^1")
        if result is not None: return sorted(set(result))
        base=payload.get("pull_request",{}).get("base",{}).get("sha")
    else:
        base=payload.get("before")
    if base and re.fullmatch(r"[a-f0-9]{40}",base) and base!="0"*40:
        result=diff(base)
        if result is None:
            git("fetch","--no-tags","--depth=1","origin",base)
            result=diff(base)
        if result is not None: return sorted(set(result))
        # Never silently use HEAD^ for a known multi-commit push.
        commits=payload.get("commits",[])
        if event=="push" and commits and payload.get("size",len(commits))==len(commits):
            return sorted({p for c in commits for k in ("added","modified","removed") for p in c.get(k,[])})
        raise RuntimeError("Cannot resolve complete push/PR diff; fetch base or run an explicit project")
    if base=="0"*40:
        r=git("ls-files","-z")
        if r.returncode==0: return r.stdout.split("\0")[:-1]
    result=diff("HEAD^")
    return sorted(set(result)) if result is not None else None

def known_profiles():
    paths=[ROOT/"build-profile.json",*sorted((ROOT/"projects").glob("*/build-profile.json"))]
    for path in paths:
        try: p=json.loads(path.read_text("utf-8"))
        except (OSError,ValueError): continue
        if not isinstance(p,dict): continue
        yield ("." if path.parent==ROOT else path.parent.relative_to(ROOT).as_posix()),p

def select_projects(requested,changed=None):
    if requested:
        if requested.startswith("@changed:"):
            base=requested[9:]
            if not re.fullmatch(r"[a-f0-9]{40}",base):raise ValueError("@changed requires an exact base commit SHA")
            paths=diff(base)
            if paths is None:
                git("fetch","--no-tags","--depth=1","origin",base)
                paths=diff(base)
            if paths is None:raise ValueError("Cannot resolve requested change range")
            return select_projects("",paths)
        if requested.startswith("@batch:"):
            if __package__:
                from .qa.control import batch_select
            else:
                from qa.control import batch_select
            return batch_select(list(known_profiles()),requested[7:].split(","))
        if requested.startswith("@"):
            slug=requested[1:]
            selected=[d for d,p in known_profiles() if p.get("slug")==slug and p.get("qa")]
            if not selected: raise ValueError("No enrolled QA profiles for "+requested)
            return sorted(selected)
        return [requested]
    if changed is None: changed=changed_paths_from_push()
    if changed is None: return ["."]
    projects=set()
    for path in changed:
        parts=Path(path).parts
        if len(parts)>=3 and parts[0]=="projects" and (ROOT/"projects"/parts[1]/"build-profile.json").is_file():
            projects.add("projects/"+parts[1])
        elif path in ("build.gradle","settings.gradle","gradle.properties","build-profile.json") or path.startswith("src/"):
            projects.add(".")
    for d,p in known_profiles():
        q=p.get("qa")
        if not isinstance(q,dict): continue
        scenario=q.get("scenario")
        if q.get("player_scenario") and any(path==q["player_scenario"] or path.startswith("tools/qa/bot/") or path in ("tools/qa/player_suite.py","tools/qa/virtual_player.py") for path in changed): projects.add(d)
        watches=q.get("watch_paths",[])
        if any(path==root or path.startswith(root+"/") for path in changed for root in p.get("shared_sources",[])): projects.add(d)
        if scenario in changed or any(path in changed for path in ("tools/qa/compatibility.json","tools/qa/common.py","tools/qa/static.py","tools/qa/assets.py","tools/qa/server.py","tools/qa/gate.py")) or any(fnmatch.fnmatch(path,pattern) or path.startswith(pattern.rstrip("/")+"/") for path in changed for pattern in watches):
            projects.add(d)
    if len(projects)>256: raise ValueError("GitHub matrix exceeds 256 targets; dispatch enrolled cohorts separately")
    return sorted(projects)

if __name__=="__main__":
    from resolve_project import read_profile
    requested=(sys.argv[1] if len(sys.argv)>1 else "").strip()
    dirs=select_projects(requested)
    profiles=[read_profile(d) for d in dirs]
    result=json.dumps(dirs,separators=(",",":"))
    print("Projects: "+result,file=sys.stderr)
    with open(os.environ["GITHUB_OUTPUT"],"a",encoding="utf-8") as f:
        f.write("project_dirs="+result+"\n")
        f.write("qa_enabled="+str(any(p.get("qa") for p in profiles)).lower()+"\n")
