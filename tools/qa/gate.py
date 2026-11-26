"""Command-line release gate. Legacy is explicit; failures never become passes."""
import argparse
import os
import sys
from pathlib import Path
from .common import ROOT, compatibility, profile, qa_config, report_for, write_json, require, sha256
from .static import validate_jar
from .server import run
from .crash import analyze

def outputs(report):
    if out:=os.getenv("GITHUB_OUTPUT"):
        with open(out,"a",encoding="utf-8") as f:
            f.write("eligible="+str(report["eligible"]).lower()+"\n")
            f.write("level="+str(report["qa_level"])+"\n")

def main():
    ap=argparse.ArgumentParser()
    ap.add_argument("project")
    ap.add_argument("--jar")
    ap.add_argument("--report",required=True)
    ap.add_argument("--work",required=True)
    ap.add_argument("--failure-log")
    args=ap.parse_args()
    report={"schema":1,"project":args.project,"status":"failed","eligible":False,"qa_level":0,"checks":[]}
    try:
        p=profile(args.project)
        report=report_for(p)
        q=qa_config(p)
        report["mode"]=q["mode"]
        if args.failure_log:
            log=Path(args.failure_log)
            report["status"]="build-failed-or-skipped"
            report["diagnostics"]=analyze(log.read_text(errors="replace")) if log.is_file() else []
            return 1
        require(args.jar and Path(args.jar).is_file(),"Compiled JAR missing")
        jar=Path(args.jar).resolve()
        report["jar"]=jar.name
        report["sha256"]=sha256(jar)
        report["qa_level"]=2 # existing verify_build_output.py is the prerequisite
        if q["mode"]=="legacy":
            report.update(status="legacy-static-only",eligible=True)
            return 0
        report["release_targets"]=q["release_targets"]
        report["static"]=validate_jar(jar,p)
        if q["mode"]=="client":
            report.update(status="client-candidate-awaiting-validation",eligible=False)
            return 2
        run(p,q,jar,args.work,report)
        require(report["qa_level"]==4,"Gameplay runtime not verified")
        report.update(status="passed",eligible=True)
        return 0
    except Exception as e:
        report["status"]="failed"
        report["error"]=f"{type(e).__name__}: {e}"
        texts=[report["error"]]
        for pth in Path(args.work).glob("logs/*.log"):
            texts.append(pth.read_text(errors="replace")[-1000000:])
        report["diagnostics"]=analyze("\n".join(texts))
        print("QA FAILED:",report["error"],file=sys.stderr)
        return 1
    finally:
        write_json(args.report,report)
        outputs(report)
        print("QA:",report["project"],report["status"],"level",report["qa_level"],flush=True)

if __name__=="__main__":
    sys.exit(main())
