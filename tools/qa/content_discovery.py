"""Advisory final-JAR content inventory; resource hints are not live registry entries."""
import argparse
import hashlib
import json
import re
import zipfile
from collections import Counter
from pathlib import Path

NS = r"[a-z0-9_.-]+"
PATH = r"[a-z0-9_./-]+"
RESOURCE = re.compile(rf"^{NS}:{PATH}$")
PATTERNS = [
    ("recipe", "packaged_recipe", rf"data/({NS})/recipes?/({PATH})\.json"),
    ("block", "blockstate", rf"assets/({NS})/blockstates/({PATH})\.json"),
    ("item", "item_definition", rf"assets/({NS})/items/({PATH})\.json"),
    ("item", "item_model_hint", rf"assets/({NS})/models/item/({PATH})\.json"),
    ("block", "block_loot_hint", rf"data/({NS})/loot_tables?/blocks/({PATH})\.json"),
    ("other", "worldgen_resource", rf"data/({NS})/worldgen/({PATH})\.json"),
    ("other", "dimension_resource", rf"data/({NS})/(dimension/({PATH}))\.json"),
]
# Translation keys are not registry proof. Dotted/path-derived keys are ambiguous;
# only simple names are suggested, and each remains a hint.
LANG = re.compile(rf"assets/({NS})/lang/[a-z_]+\.json")
LANG_KEY = re.compile(rf"(item|block|entity|effect)\.({NS})\.([a-z0-9_-]+)")

def digest(path):
    with Path(path).open("rb") as f:
        return hashlib.file_digest(f, "sha256").hexdigest()

def inventory(jar, modid):
    if not re.fullmatch(NS, modid):
        raise ValueError("Invalid mod namespace")
    rows = {}
    warnings = []
    def add(kind, ident, source, file):
        if not RESOURCE.fullmatch(ident) or ".." in ident.split(":", 1)[1].split("/"):
            return
        row = rows.setdefault((kind, ident), {"kind":kind,"id":ident,"sources":[],
            "namespace_scope":"own" if ident.split(":")[0]==modid else "other_namespace"})
        entry = {"type":source,"file":file}
        if entry not in row["sources"]: row["sources"].append(entry)
    with zipfile.ZipFile(jar) as z:
        if len(z.infolist()) > 100_000:
            raise ValueError("JAR inventory exceeds entry budget")
        for info in sorted(z.infolist(), key=lambda i:i.filename):
            name = info.filename
            for kind, source, pattern in PATTERNS:
                match = re.fullmatch(pattern, name)
                if match:
                    add(kind, match[1]+":"+match[2], source, name)
                    break
            if not LANG.fullmatch(name): continue
            try:
                if info.file_size > 1_000_000: raise ValueError("Language file exceeds budget")
                doc = json.loads(z.read(info))
                if not isinstance(doc, dict): raise ValueError("Language file is not an object")
                for key in doc:
                    match = LANG_KEY.fullmatch(key)
                    if match: add(match[1],match[2]+":"+match[3],"translation_hint",name)
            except (ValueError, UnicodeError) as e:
                warnings.append({"file":name,"reason":str(e)})
    return [rows[k] for k in sorted(rows)], warnings

def reconcile(jar, modid, qa=None):
    sha = digest(jar)
    rows, warnings = inventory(jar, modid)
    evidence_status = "NOT_AVAILABLE"
    entries = []
    if qa is not None:
        if not isinstance(qa, dict) or qa.get("sha256") != sha:
            raise ValueError("QA report does not match exact JAR checksum")
        coverage = qa.get("coverage", {})
        entries = coverage.get("entries", [])
        if not isinstance(entries, list): raise ValueError("Invalid QA coverage entries")
        evidence_status = "EXACT_JAR_REPORT"
    for row in rows:
        matches = [e for e in entries if isinstance(e,dict) and e.get("kind")==row["kind"]
                   and e.get("subject")==row["id"]]
        row["coverage_ids"] = sorted({e["id"] for e in matches if isinstance(e.get("id"),str)})
        if not matches:
            row["status"] = "NO_EXACT_DECLARATION" if evidence_status=="EXACT_JAR_REPORT" else "EVIDENCE_UNAVAILABLE"
        elif any(e.get("status")=="FAIL" for e in matches):
            row["status"] = "DECLARED_FAIL"
        elif all(e.get("status")=="PASS" for e in matches):
            row["status"] = "DECLARED_PASS"
        else:
            row["status"] = "DECLARED_NOT_RUN"
    return {"schema":1,"jar":Path(jar).name,"sha256":sha,"modid":modid,
            "scope":"Packaged resource inventory and exact named coverage links only; not live registries or semantic completeness.",
            "live_registry":"NOT_RUN","evidence":evidence_status,"warnings":warnings,
            "counts":dict(sorted(Counter(r["status"] for r in rows).items())),"content":rows}

def main():
    p=argparse.ArgumentParser(description=__doc__)
    p.add_argument("jar");p.add_argument("--modid",required=True)
    p.add_argument("--qa-report",type=Path);p.add_argument("--out",type=Path,required=True)
    a=p.parse_args()
    qa=json.loads(a.qa_report.read_text()) if a.qa_report and a.qa_report.is_file() else None
    report=reconcile(a.jar,a.modid,qa)
    a.out.parent.mkdir(parents=True,exist_ok=True)
    a.out.write_text(json.dumps(report,indent=2,ensure_ascii=False)+"\n",encoding="utf-8")
    print("Content discovery (advisory):",report["counts"],"| live registry NOT_RUN")

if __name__=="__main__": main()
