"""Enforce declared feature-to-executed-assertion coverage; not semantic verification."""
import json
import re
from collections import Counter
from .common import ROOT, REGISTRY, require, safe_path, target
from .server import scenario_steps
from .player_suite import SUPPORTED, validate as validate_player

KINDS = {"mechanic", "item", "block", "entity", "recipe", "effect", "menu", "other"}

def named_assertions(steps):
    return Counter(s["name"] for s in steps
                   if s.get("assertion") is True and isinstance(s.get("name"), str)
                   and s["name"].strip() and isinstance(s.get("expect"), str) and s["expect"])

def validate_plan(rows, current, server_steps, player_steps, known_targets):
    require(isinstance(rows, list) and 1 <= len(rows) <= 500, "Coverage needs 1..500 declared entries")
    available = {"server": named_assertions(server_steps), "player": named_assertions(player_steps)}
    ids = set()
    active = []
    for row in rows:
        require(isinstance(row, dict) and set(row) <= {"id", "kind", "subject", "targets", "checks"}, "Invalid coverage entry")
        ident = row.get("id")
        require(isinstance(ident, str) and re.fullmatch(r"[a-z][a-z0-9_.-]{0,95}", ident) and ident not in ids, "Invalid or duplicate coverage ID")
        ids.add(ident)
        require(row.get("kind") in KINDS and isinstance(row.get("subject"), str) and row["subject"].strip(), "Missing coverage kind/subject")
        targets = row.get("targets")
        if targets is not None:
            require(isinstance(targets, list) and targets and all(isinstance(t, str) for t in targets)
                    and len(targets) == len(set(targets)) and set(targets) <= known_targets, "Invalid coverage targets")
        checks = row.get("checks")
        require(isinstance(checks, list) and 1 <= len(checks) <= 50, "Coverage entry needs named assertions: " + ident)
        refs = set()
        for ref in checks:
            require(isinstance(ref, dict) and set(ref) == {"backend", "name"} and ref["backend"] in available
                    and isinstance(ref["name"], str) and ref["name"].strip(), "Invalid assertion reference: " + ident)
            key = (ref["backend"], ref["name"])
            require(key not in refs, "Duplicate assertion reference: " + ident)
            refs.add(key)
            if targets is None or current in targets:
                require(available[key[0]][key[1]] == 1, "Missing or ambiguous executable assertion: " + ident + " / " + key[1])
        if targets is None or current in targets:
            active.append(row)
    require(active, "No coverage entries applicable to " + current)
    return active

def prepare(p, q):
    doc = json.loads(safe_path(ROOT, q["scenario"]).read_text())
    require("coverage" in doc, "New or changed server profile requires machine-readable scenario coverage")
    if (p["loader"], p["minecraft"]) in SUPPORTED:
        require(q.get("player_scenario"), "Supported protocol target requires player_scenario; request reviewed backend exception if incompatible")
    player = []
    if q.get("player_scenario"):
        player = validate_player(json.loads(safe_path(ROOT, q["player_scenario"]).read_text()))
    known = {l + "-mc" + v for v, m in REGISTRY["minecraft"].items() for l in m["loaders"]}
    return validate_plan(doc["coverage"], target(p), scenario_steps(doc, p["loader"]), player, known)

def evaluate(plan, report):
    if plan is None:
        return {"status": "not-declared-existing-baseline", "scope": "no coverage certification"}
    sources = {"server": report.get("checks", []),
               "player": report.get("virtual_player", {}).get("checks", [])}
    rows = []
    for entry in plan:
        refs = []
        for ref in entry["checks"]:
            matches = [c for c in sources[ref["backend"]]
                       if c.get("assertion") is True and c.get("name") == ref["name"]]
            status = "PASS" if len(matches) == 1 and matches[0].get("passed") is True else (
                "NOT_RUN" if not matches else "FAIL")
            refs.append(dict(ref, status=status))
        status = "FAIL" if any(r["status"] == "FAIL" for r in refs) else (
            "NOT_RUN" if any(r["status"] == "NOT_RUN" for r in refs) else "PASS")
        rows.append({"id": entry["id"], "kind": entry["kind"], "subject": entry["subject"], "status": status, "checks": refs})
    counts = {s: sum(r["status"] == s for r in rows) for s in ("PASS", "FAIL", "NOT_RUN")}
    return {"status": "declared-pass" if counts["PASS"] == len(rows) else "incomplete",
            "scope": "declared entries and actual assertion evidence; not automatic discovery of all mechanics",
            "total": len(rows), "counts": counts, "entries": rows}
