"""Run diagnostics on production JARs, separate from release eligibility."""
import argparse
import json
import os
from pathlib import Path
from .common import profile, relative_project, report_for, require, sha256, write_json
from .server import run
from .assets import inspect
from .performance import benchmark
from .virtual_player import run_player
from .crash import analyze

def main():
    ap=argparse.ArgumentParser();ap.add_argument("mode",choices=["performance","virtual-player"]);ap.add_argument("project");ap.add_argument("jar");ap.add_argument("--out",required=True)
    a=ap.parse_args();p=profile(a.project);jar=Path(a.jar).resolve();out=Path(a.out).resolve();out.mkdir(parents=True,exist_ok=True)
    report=report_for(p);report.update(kind="p4-diagnostic",mode=a.mode,eligible=False,jar=jar.name,sha256=sha256(jar))
    try:
        require(jar.parent==(relative_project(a.project)/"build/libs").resolve(),"Not final Gradle output")
        report["assets"]=inspect(jar)
        require(not report["assets"]["errors"],"Asset validation failed")
        if a.mode=="performance":
            require(p["minecraft"]=="26.3" and p["slug"]=="fragileglass","Pilot only: Fragile Glass 26.3")
            q=p["qa"]
            run(p,q,jar,out/"runtime",report,after_suites=benchmark)
        else:
            require(p["minecraft"]=="1.21.1" and p["loader"]=="fabric" and p["slug"]=="fragileglass","Pilot only: Fragile Glass Fabric 1.21.1")
            q={"loader_version":"0.17.3","scenario":"tools/qa/scenarios/virtual-player-setup.json","dependencies":[]}
            run(p,q,jar,out/"runtime",report,after_suites=run_player)
        report.update(status="passed",eligible=False)
        return 0
    except Exception as e:
        report.update(status="failed",error=f"{type(e).__name__}: {e}",eligible=False)
        logs=[x.read_text(errors="replace")[-500000:] for x in (out/"runtime/logs").glob("*.log")]
        report["diagnostics"]=analyze("\n".join([report["error"],*logs]))
        print("P4 FAILED",report["error"],flush=True)
        return 1
    finally:
        # Diagnostic workflow is never a release QA certification.
        report["server_level_observed"]=report.pop("qa_level",0)
        write_json(out/"p4-report.json",report)
if __name__=="__main__":raise SystemExit(main())
