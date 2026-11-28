"""Manifest-backed ecosystem capability catalogue and conservative affected-project planner."""
import argparse
import json
import re
import subprocess
from collections import defaultdict
from pathlib import Path, PurePosixPath

ROOT = Path(__file__).resolve().parents[2]
TOKEN = re.compile(r"[a-z][a-z0-9_.-]*")
CAP = re.compile(r"[a-z][a-z0-9_.-]*:[a-z][a-z0-9_./-]*")
LOADERS = {"forge", "fabric", "neoforge"}

def require(ok, message):
    if not ok:
        raise ValueError(message)

def token(value, regex=TOKEN):
    return isinstance(value, str) and regex.fullmatch(value) is not None

def integer(value, minimum=0):
    return type(value) is int and value >= minimum

def array(value):
    return isinstance(value, list) and all(isinstance(x, dict) for x in value)

def words(value):
    return isinstance(value, list) and bool(value) and all(isinstance(x, str) and x.strip() for x in value)

def relative(value):
    return isinstance(value, str) and bool(value) and not PurePosixPath(value).is_absolute() and ".." not in PurePosixPath(value).parts and "\\" not in value

def git(root, *args):
    return subprocess.check_output(["git", *args], cwd=root, text=True, timeout=60)

def collect(root=ROOT, ref=None):
    """Only explicit declarations enroll a project; no fuzzy name or dependency inference."""
    root = Path(root)
    if ref:
        names = git(root, "ls-tree", "-r", "--name-only", ref).splitlines()
        paths = [p for p in names if re.fullmatch(r"projects/[^/]+/integration\.json", p)]
        read = lambda p: json.loads(git(root, "show", f"{ref}:{p}"))
    else:
        paths = sorted(p.relative_to(root).as_posix() for p in (root / "projects").glob("*/integration.json"))
        read = lambda p: json.loads((root / p).read_text(encoding="utf-8"))
    entries = {}
    for path in paths:
        d = str(PurePosixPath(path).parent)
        m = read(path)
        p = read(d + "/build-profile.json")
        validate(d, m, p)
        entries[d] = m
    return entries

def validate(path, m, profile):
    def check(ok, field):
        require(ok, f"{path}/integration.json: invalid {field}")
    check(isinstance(m, dict) and type(m.get("schema")) is int and m["schema"] == 1, "schema")
    for field in ("ecosystem", "module"):
        check(token(m.get(field)), field)
    check("parent_module" not in m or (token(m["parent_module"]) and m["parent_module"] != m["module"]), "parent_module")
    t = m.get("target")
    check(isinstance(t, dict) and t.get("loader") in LOADERS and isinstance(t.get("minecraft"), str) and re.fullmatch(r"\d+\.\d+(?:\.\d+)?", t["minecraft"]) and all(t.get(k) == profile.get(k) for k in ("minecraft", "loader")), "target/profile mismatch")
    for field in ("provides", "consumes", "tests"):
        check(array(m.get(field)), field)
    for field in ("provides", "consumes"):
        seen = set()
        for item in m[field]:
            cid = item.get("id")
            check(token(cid, CAP) and cid.split(":")[0] == m["ecosystem"] and cid not in seen, field + ".id (must be unique and ecosystem-namespaced)")
            seen.add(cid)
            check(integer(item.get("version"), 1), field + ".version")
            if field == "provides":
                check(integer(item.get("revision")), "provides.revision")
                check(words(item.get("domains")) and len(set(item["domains"])) == len(item["domains"]), "provides.domains")
                check(words(item.get("entrypoints")) and relative(item.get("documentation")), "provides.entrypoints/documentation")
            else:
                check(integer(item.get("min_revision")) and type(item.get("required")) is bool, "consumes version/required")
                check(item.get("status") in {"implemented", "planned", "missing"}, "consumes.status")
                check(isinstance(item.get("fallback"), str) and item["fallback"].strip(), "consumes.fallback")
                if item["status"] != "implemented":
                    check(isinstance(item.get("issue"), str) and re.fullmatch(r"https://github\.com/[^/]+/[^/]+/issues/\d+", item["issue"]), "consumes.issue")
    seen = set()
    for test in m["tests"]:
        check(token(test.get("id")) and test["id"] not in seen, "tests.id")
        seen.add(test["id"])
        declared = {x["id"] for f in ("provides", "consumes") for x in m[f]}
        check(words(test.get("capabilities")) and set(test["capabilities"]) <= declared, "tests.capabilities")
        check(relative(test.get("ref")) and test.get("status") in {"implemented", "planned"}, "tests.ref/status")

def cohort(m):
    return m["ecosystem"], m["target"]["minecraft"], m["target"]["loader"]

def catalogue(entries):
    providers, modules, domains = {}, {}, {}
    edges, diagnostics = [], []
    def error(kind, project, **fields):
        diagnostics.append(dict(kind=kind, project=project, severity="error", **fields))
    for path, m in sorted(entries.items()):
        group = cohort(m)
        key = (*group, m["module"])
        if key in modules:
            error("duplicate_module", path, other=modules[key])
        modules[key] = path
        for p in m["provides"]:
            key = (*group, p["id"])
            if key in providers:
                error("duplicate_capability", path, capability=p["id"], other=providers[key][0])
            else:
                providers[key] = (path, p)
            for domain in p["domains"]:
                dk = (*group, domain)
                if dk in domains and domains[dk] != path:
                    error("duplicate_domain", path, domain=domain, other=domains[dk])
                domains[dk] = path
    for path, m in sorted(entries.items()):
        group = cohort(m)
        parent = m.get("parent_module")
        if parent and (*group, parent) not in modules:
            diagnostics.append(dict(kind="parent_not_in_checkout", project=path, parent=parent, severity="warning"))
        for c in m["consumes"]:
            found = providers.get((*group, c["id"]))
            compatible = bool(found and found[1]["version"] == c["version"] and found[1]["revision"] >= c["min_revision"])
            status = "compatible" if compatible else "incompatible" if found else "missing"
            edges.append(dict(consumer=path, provider=found[0] if found else None, capability=c["id"], status=status, required=c["required"], declaration=c["status"]))
            if not compatible:
                severity = "error" if c["required"] and c["status"] == "implemented" and found else "warning"
                diagnostics.append(dict(kind=status, project=path, capability=c["id"], severity=severity))
    # Required implemented dependency cycles are not loadable as independent hard dependencies.
    graph = defaultdict(set)
    for e in edges:
        if e["required"] and e["declaration"] == "implemented" and e["provider"]:
            graph[e["consumer"]].add(e["provider"])
    visiting, done = set(), set()
    def visit(node):
        if node in visiting:
            error("required_cycle", node)
            return
        if node in done:
            return
        visiting.add(node)
        for dest in sorted(graph.get(node, ())):
            visit(dest)
        visiting.remove(node)
        done.add(node)
    for path in sorted(entries):
        visit(path)
    return dict(schema=1, projects=entries, edges=edges, diagnostics=diagnostics)

def affected(entries, changed, previous=None, initial=()):
    """Expand only same-target declared dependencies. Old edges preserve removal impact."""
    previous = previous or {}
    all_paths = set(entries) | set(previous)
    impacted = set(initial)
    impacted.update(p for p in all_paths if any(f == p or f.startswith(p + "/") for f in changed))
    for entries_at_ref in (entries, previous):
        for p, m in entries_at_ref.items():
            refs = [t["ref"] for t in m["tests"]] + [c["documentation"] for c in m["provides"]]
            if any(r in changed for r in refs):
                impacted.add(p)
    edges = catalogue(entries)["edges"] + catalogue(previous)["edges"]
    progress = True
    while progress:
        added = {e["consumer"] for e in edges if e["provider"] in impacted} - impacted
        impacted.update(added)
        progress = bool(added)
    return sorted(impacted & set(entries) | set(initial))

def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--root", type=Path, default=ROOT)
    parser.add_argument("--base", help="Exact previous ref for deletion/renaming impact")
    parser.add_argument("--out", type=Path)
    args = parser.parse_args()
    entries = collect(args.root)
    previous = collect(args.root, args.base) if args.base else {}
    changed = git(args.root, "diff", "--name-only", "--no-renames", args.base, "HEAD").splitlines() if args.base else []
    report = catalogue(entries)
    report["affected_projects"] = affected(entries, changed, previous)
    data = json.dumps(report, indent=2, sort_keys=True) + "\n"
    if args.out:
        args.out.parent.mkdir(parents=True, exist_ok=True)
        args.out.write_text(data, encoding="utf-8")
    print(data, end="")
    return int(any(d["severity"] == "error" for d in report["diagnostics"]))

if __name__ == "__main__":
    raise SystemExit(main())
