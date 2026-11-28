"""Targeted production runtime check, not a release gate substitute.
Upstream Core candidate is intentionally not claimed as a same-run pack member.
"""
import json
import os
from pathlib import Path
import sys
sys.path.insert(0,str(Path(__file__).resolve().parents[3]))
from tools.qa.common import sha256
from tools.qa.server import run

out=Path("hydration-exact-evidence")
out.mkdir(exist_ok=True)
report={"kind":"hydration-exact-core-integration","eligible":False,"status":"failed","qa_level":0,"checks":[],
        "core_run":46123087105,"core_artifact":15254721484,"core_source":"4a3e510fd4dcfae50e4292b2b2f95eca2f26a0e4",
        "core_qa":"Build Hub success; separate ore QA pending at pin","commit":os.environ["GITHUB_SHA"],"run_id":os.environ["GITHUB_RUN_ID"]}
try:
    core=Path(os.environ["HYDRATION_CORE_JAR"])
    addon=Path(os.environ["HYDRATION_JAR"])
    expected="a966fb939988e794586cd6a1ea18fafe340296447b67ef8a9827963328e20548"
    if sha256(core)!=expected: raise ValueError("Core binary SHA-256 mismatch")
    report.update(core_sha256=expected,hydration_sha256=sha256(addon))
    run({"slug":"hydration","minecraft":"1.21.1","loader":"neoforge"},
        {"loader_version":"21.1.262","scenario":"tools/qa/scenarios/hydration-neoforge.json"},
        addon,out/"runtime",report,extra_jars=(core,))
    report["status"]="passed"
except Exception as exc:
    report["error"]=str(exc)
    raise
finally:
    (out/"report.json").write_text(json.dumps(report,indent=2)+"\n",encoding="utf-8")
