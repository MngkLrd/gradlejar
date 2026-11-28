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

MEMBERSHIP = "ecosystem_membership"
OPERATION_KINDS = {"query", "command", "event", "extension"}

def membership(m):
    return {k: m[k] for k in ("ecosystem", "module", "role", "core_module")}

def member_paths(m):
    return [p for c in m["provides"] for p in c.get("source_paths", [])] + [p for f in m.get("features", []) for p in f["source_paths"]]

def validate_files(path, m, profile, exists):
    for capability in m["provides"]:
        require(exists(capability["documentation"]), f"{path}: missing capability documentation {capability['documentation']}")
    for test in m["tests"]:
        if test["status"] == "implemented":
            require(exists(test["ref"]), f"{path}: missing implemented test reference {test['ref']}")
    if m["schema"] == 2:
        shared = profile.get("shared_sources", [])
        require(isinstance(shared, list) and all(relative(p) for p in shared), f"{path}: invalid shared source roots")
        for source in member_paths(m):
            require(owns(path, source) or any(source.startswith(p+"/") for p in shared), f"{path}: source outside owner/shared roots: {source}")
            require(exists(source), f"{path}: missing API/feature source: {source}")

def check_project(root, project):
    """Scoped local evidence gate. No peer graph, runtime or test execution claim."""
    require(project == "." or (isinstance(project,str) and re.fullmatch(r"projects/[a-zA-Z0-9_.-]+",project) and relative(project)), "Invalid project path")
    root = Path(root).resolve()
    directory = root / project
    require(directory.resolve().is_relative_to(root), "Project outside checkout")
    profile_path = directory / "build-profile.json"
    manifest_path = directory / "integration.json"
    if not profile_path.is_file():
        require(not manifest_path.exists(), f"{project}: integration manifest requires build profile")
        return {"project":project,"status":"NOT_ENROLLED","integration_qa":"NOT_RUN"}
    p = json.loads(profile_path.read_text(encoding="utf-8"))
    require(isinstance(p,dict), f"{project}: invalid build profile")
    if not manifest_path.is_file():
        require(MEMBERSHIP not in p, f"{project}: enrolled project missing integration.json")
        return {"project":project,"status":"NOT_ENROLLED","integration_qa":"NOT_RUN"}
    m = json.loads(manifest_path.read_text(encoding="utf-8"))
    validate(project,m,p)
    exists = lambda path: (root/path).is_file() and (root/path).resolve().is_relative_to(root)
    validate_files(project,m,p,exists)
    return {"project":project,"status":"STATIC_EVIDENCE_VALIDATED" if m["schema"]==2 else "LEGACY_V1",
            "integration_qa":"NOT_RUN","runtime_verification":"NOT_RUN"}

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
        validate_files(d,m,p,exists)
        entries[d] = m
    return entries

def validate(path, m, profile):
    def check(ok, field):
        require(ok, f"{path}/integration.json: invalid {field}")
    check(isinstance(m, dict) and type(m.get("schema")) is int and m["schema"] in (1, 2), "schema")
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
    if m["schema"] == 1:
        check(MEMBERSHIP not in profile, "enrolled v2 project must not downgrade its manifest")
        return
    check(m.get("role") in ("core","addon") and token(m.get("core_module")), "role/core_module")
    if m["role"] == "core":
        check(m["core_module"] == m["module"] and "parent_module" not in m, "core must be its own root")
    else:
        check(m["core_module"] != m["module"] and m.get("parent_module") == m["core_module"], "first-party addon must identify its core")
    check(profile.get(MEMBERSHIP) == membership(m), "persistent build-profile ecosystem_membership")
    check(bool(m["provides"]), "core/addon must expose real public capabilities")
    provided = {p["id"] for p in m["provides"]}
    consumed = {c["id"] for c in m["consumes"]}
    def sources(value):
        return words(value) and all(relative(x) for x in value) and len(set(value))==len(value)
    def note(value):
        return isinstance(value,str) and len(value.strip())>=12 and value.strip().lower() not in ("not applicable","not implemented")
    for p in m["provides"]:
        check(sources(p.get("source_paths")), "provides.source_paths")
        check(array(p.get("operations")) and bool(p["operations"]), "provides.operations")
        seen_ops = set()
        for op in p["operations"]:
            check(token(op.get("id")) and op["id"] not in seen_ops, "operations.id")
            seen_ops.add(op["id"])
            check(isinstance(op.get("kind"),str) and op["kind"] in OPERATION_KINDS, "operations.kind")
            check(op.get("entrypoint") in p["entrypoints"] and note(op.get("semantics")), "operations.entrypoint/semantics")
    check(array(m.get("features")) and bool(m["features"]), "features inventory")
    seen_features, exposed, used = set(), set(), set()
    for f in m["features"]:
        check(token(f.get("id")) and f["id"] not in seen_features, "features.id")
        seen_features.add(f["id"])
        check(sources(f.get("source_paths")), "features.source_paths")
        for field, declared in (("provides",provided),("consumes",consumed)):
            values = f.get(field)
            check(isinstance(values,list) and all(isinstance(v,str) for v in values) and set(values)<=declared and len(set(values))==len(values), "features."+field)
        check(note(f.get("integration_review")), "features.integration_review: actual peer/API decisions")
        if not f["provides"]:
            check(note(f.get("no_public_surface_reason")), "features.no_public_surface_reason")
        exposed.update(f["provides"]);used.update(f["consumes"])
    check(exposed == provided and used == consumed, "all declared capabilities must map to actual feature inventory")
    tested = {cid for t in m["tests"] if t["status"]=="implemented" for cid in t["capabilities"]}
    check(provided | {c["id"] for c in m["consumes"] if c["status"]=="implemented"} <= tested, "each public and implemented consumed capability needs an implemented test reference")


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
    # Resolve first-party cores within a single ecosystem / target, without
    # deriving ownership from repository names or third-party dependencies.
    core_groups = defaultdict(list)
    for path,m in sorted(entries.items()):
        if m["schema"]==2 and m["role"]=="core":
            core_groups[cohort(m)].append(path)
    for group,paths in core_groups.items():
        if len(paths)>1:
            for path in paths:
                diagnostic("multiple_cores",path,other=next(p for p in paths if p!=path))
    for path, m in sorted(entries.items()):
        group = cohort(m)
        parent = m.get("parent_module")
        if parent:
            candidates = modules.get((*group, parent), [])
            parents.append(dict(consumer=path, providers=candidates, module=parent))
            if len(candidates) != 1:
                diagnostic("parent_not_in_checkout" if not candidates else "ambiguous_parent",
                           path, "error" if candidates or m["schema"]==2 else "warning", parent=parent)
            elif m["schema"]==2:
                parent_m=entries[candidates[0]]
                if parent_m["schema"]!=2 or parent_m.get("role")!="core" or parent_m.get("core_module")!=parent:
                    diagnostic("parent_not_promoted_to_core",path,other=candidates[0],parent=parent)
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
                first_party_cores=[{"project":p,"ecosystem":m["ecosystem"],"module":m["module"],"target":m["target"]} for p,m in sorted(entries.items()) if m["schema"]==2 and m["role"]=="core"],
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
            refs = [t["ref"] for t in m["tests"]] + [c["documentation"] for c in m["provides"]] + member_paths(m)
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
    parser.add_argument("--check-project", help="Validate local API-first declaration and evidence for one selected project only")
    args = parser.parse_args()
    if args.check_project is not None:
        if args.base or args.out:parser.error("--check-project cannot be combined with --base or --out")
        try:report=check_project(args.root,args.check_project)
        except (ValueError,OSError,TypeError,KeyError) as exc:
            print("API-FIRST BLOCKED: "+str(exc))
            return 1
        print("API-FIRST "+json.dumps(report,sort_keys=True))
        return 0
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
