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
    return (isinstance(value, str) and bool(value) and not PurePosixPath(value).is_absolute()
            and all(part not in ("", ".", "..") for part in value.split("/"))
            and "\\" not in value and "\x00" not in value)

def git(root, *args):
    return subprocess.check_output(["git", *args], cwd=root, text=True, timeout=60)

def collect(root=ROOT, ref=None):
    """Only explicit declarations enroll a project; do not infer membership from names."""
    root = Path(root)
    if ref:
        names = set(git(root, "ls-tree", "-r", "--name-only", ref).splitlines())
        paths = sorted(p for p in names if p == "integration.json" or re.fullmatch(r"projects/[^/]+/integration\.json", p))
        read = lambda p: json.loads(git(root, "show", f"{ref}:{p}"))
        exists = names.__contains__
    else:
        paths = sorted(p.relative_to(root).as_posix() for p in [root / "integration.json", *(root / "projects").glob("*/integration.json")] if p.is_file())
        read = lambda p: json.loads((root / p).read_text(encoding="utf-8"))
        exists = lambda p: (root / p).is_file() and (root / p).resolve().is_relative_to(root.resolve())
    entries = {}
    for path in paths:
        d = str(PurePosixPath(path).parent)
        profile_path = "build-profile.json" if d == "." else d + "/build-profile.json"
        require(exists(profile_path), f"{path}: missing build profile")
        m = read(path)
        p = read(profile_path)
        validate(d, m, p)
        for capability in m["provides"]:
            require(exists(capability["documentation"]), f"{path}: missing capability documentation {capability['documentation']}")
        for test in m["tests"]:
            if test["status"] == "implemented":
                require(exists(test["ref"]), f"{path}: missing implemented test reference {test['ref']}")
        entries[d] = m
    return entries

def validate(path, m, profile):
    def check(ok, field):
        require(ok, f"{path}/integration.json: invalid {field}")
    check(isinstance(m, dict) and type(m.get("schema")) is int and m["schema"] == 1, "schema")
    check(isinstance(profile, dict), "build profile")
    for field in ("ecosystem", "module"):
        check(token(m.get(field)), field)
    check("parent_module" not in m or (token(m["parent_module"]) and m["parent_module"] != m["module"]), "parent_module")
    t = m.get("target")
    check(isinstance(t, dict) and isinstance(t.get("loader"), str) and t["loader"] in LOADERS and isinstance(t.get("minecraft"), str) and re.fullmatch(r"\d+\.\d+(?:\.\d+)?", t["minecraft"]) and all(t.get(k) == profile.get(k) for k in ("minecraft", "loader")), "target/profile mismatch")
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
                check(words(item.get("domains")) and all(token(d) for d in item["domains"]) and len(set(item["domains"])) == len(item["domains"]), "provides.domains (stable canonical IDs)")
                check(words(item.get("entrypoints")) and relative(item.get("documentation")), "provides.entrypoints/documentation")
            else:
                check(integer(item.get("min_revision")) and type(item.get("required")) is bool, "consumes version/required")
                check(item.get("status") in ("implemented", "planned", "missing"), "consumes.status")
                check(isinstance(item.get("fallback"), str) and item["fallback"].strip(), "consumes.fallback")
                if item["status"] != "implemented":
                    check(isinstance(item.get("issue"), str) and re.fullmatch(r"https://github\.com/[^/]+/[^/]+/issues/\d+", item["issue"]), "consumes.issue")
    check(not ({x["id"] for x in m["provides"]} & {x["id"] for x in m["consumes"]}), "self consumption; use the local implementation directly")
    seen = set()
    for test in m["tests"]:
        check(token(test.get("id")) and test["id"] not in seen, "tests.id")
        seen.add(test["id"])
        declared = {x["id"] for f in ("provides", "consumes") for x in m[f]}
        check(words(test.get("capabilities")) and set(test["capabilities"]) <= declared, "tests.capabilities")
        check(relative(test.get("ref")) and test.get("status") in ("implemented", "planned"), "tests.ref/status")

def cohort(m):
    return m["ecosystem"], m["target"]["minecraft"], m["target"]["loader"]

def catalogue(entries):
    providers, modules, domains = defaultdict(list), defaultdict(list), {}
    edges, parents, diagnostics = [], [], []
    def diagnostic(kind, project, severity="error", **fields):
        diagnostics.append(dict(kind=kind, project=project, severity=severity, **fields))
    for path, m in sorted(entries.items()):
        group = cohort(m)
        key = (*group, m["module"])
        if modules[key]:
            diagnostic("duplicate_module", path, other=modules[key][0])
        modules[key].append(path)
        for p in m["provides"]:
            key = (*group, p["id"])
            if providers[key]:
                diagnostic("duplicate_capability", path, capability=p["id"], other=providers[key][0][0])
            providers[key].append((path, p))
            for domain in p["domains"]:
                dk = (*group, domain)
                if dk in domains and domains[dk] != path:
                    diagnostic("duplicate_domain", path, domain=domain, other=domains[dk])
                domains.setdefault(dk, path)
    for path, m in sorted(entries.items()):
        group = cohort(m)
        parent = m.get("parent_module")
        if parent:
            candidates = modules.get((*group, parent), [])
            parents.append(dict(consumer=path, providers=candidates, module=parent))
            if len(candidates) != 1:
                diagnostic("parent_not_in_checkout" if not candidates else "ambiguous_parent",
                           path, "warning" if not candidates else "error", parent=parent)
        for c in m["consumes"]:
            matches = providers.get((*group, c["id"]), [])
            found = matches[0] if len(matches) == 1 else None
            compatible = bool(found and found[1]["version"] == c["version"] and found[1]["revision"] >= c["min_revision"])
            resolution = "ambiguous" if len(matches) > 1 else "compatible" if compatible else "incompatible" if found else "missing"
            status = resolution if c["status"] == "implemented" else "not_implemented"
            edges.append(dict(consumer=path, provider=found[0] if found else None,
                              candidates=[x[0] for x in matches], capability=c["id"], status=status,
                              resolution=resolution, required=c["required"], declaration=c["status"]))
            if resolution != "compatible":
                severity = "error" if c["required"] and c["status"] == "implemented" else "warning"
                diagnostic(resolution, path, severity, capability=c["id"])
            if c["status"] != "implemented":
                diagnostic("contract_not_implemented", path, "warning", capability=c["id"])
    def detect_cycles(graph, kind):
        visiting, done = set(), set()
        def visit(node):
            if node in visiting:
                diagnostic(kind, node)
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
    # A cycle of required contracts needs architectural review. These are not
    # inferred loader dependencies; no claim about actual loader resolution.
    graph, hierarchy = defaultdict(set), defaultdict(set)
    for e in edges:
        if e["required"] and e["declaration"] == "implemented" and e["provider"]:
            graph[e["consumer"]].add(e["provider"])
    for e in parents:
        hierarchy[e["consumer"]].update(e["providers"])
    detect_cycles(graph, "required_cycle")
    detect_cycles(hierarchy, "parent_cycle")
    return dict(schema=1, projects=entries, edges=edges, parent_links=parents, diagnostics=diagnostics,
                runtime_verification="NOT_RUN", scope="Declared contract metadata only; no Java ABI or runtime compatibility certification.")

def owns(project, file):
    if project == ".":
        return file in ("integration.json", "build-profile.json", "gradle.properties", "build.gradle", "settings.gradle",
                        "build.gradle.kts", "settings.gradle.kts") or file.startswith("src/")
    return file == project or file.startswith(project + "/")

def affected(entries, changed, previous=None, initial=()):
    """Expand same-target declared dependencies, including removed/renamed providers and parent links."""
    previous = previous or {}
    changed = set(changed)
    impacted = set(initial)
    impacted.update(p for p in set(entries) | set(previous) if any(owns(p, f) for f in changed))
    for entries_at_ref in (entries, previous):
        for p, m in entries_at_ref.items():
            refs = [t["ref"] for t in m["tests"]] + [c["documentation"] for c in m["provides"]]
            if any(r in changed for r in refs):
                impacted.add(p)
    graphs = [catalogue(entries), catalogue(previous)]
    links = [(e["consumer"], set(e["candidates"])) for g in graphs for e in g["edges"]]
    links += [(e["consumer"], set(e["providers"])) for g in graphs for e in g["parent_links"]]
    while True:
        added = {consumer for consumer, providers in links if providers & impacted} - impacted
        if not added:
            break
        impacted.update(added)
    return sorted((impacted & set(entries)) | set(initial))

def preview(entries, previous, changed, initial, base):
    """Advisory report: never expands the build matrix or substitutes for integration QA."""
    impacted = set(affected(entries, changed, previous, initial))
    enrolled = impacted & set(entries)
    result = catalogue(entries)
    result["selected_build_projects"] = sorted(initial)
    result["affected_projects"] = sorted(enrolled)
    result["additional_consumers_to_review"] = sorted(enrolled - set(initial))
    result["removed_projects"] = sorted(set(previous) - set(entries))
    result["diagnostics"] = [d for d in result["diagnostics"] if d["project"] in enrolled or d.get("other") in enrolled]
    result["base_ref"] = base
    result["previous_graph_available"] = base is not None
    result["mode"] = "ADVISORY_ONLY"
    result["integration_qa"] = "NOT_RUN"
    return result

def build_preview(root, initial, base=None):
    entries = collect(root)
    previous = collect(root, base) if base else {}
    changed = git(root, "diff", "--name-only", "--no-renames", base, "HEAD").splitlines() if base else []
    report = preview(entries, previous, changed, initial, base)
    if not report["affected_projects"] and not report["removed_projects"]:
        return None
    # Keep ordinary build logs compact: no complete unrelated manifests.
    return {k:v for k,v in report.items() if k not in ("projects", "edges", "parent_links")}

def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--root", type=Path, default=ROOT)
    parser.add_argument("--base", help="Exact previous ref for deletion/renaming impact (40-character Git SHA)")
    parser.add_argument("--out", type=Path)
    args = parser.parse_args()
    if args.base and not re.fullmatch(r"[a-f0-9]{40}", args.base):
        parser.error("--base requires an exact Git commit SHA")
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
