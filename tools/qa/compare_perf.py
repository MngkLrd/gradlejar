"""Compare vanilla, loader-only and exact production mod on the same runner."""
import argparse
import json
import os
import platform
from pathlib import Path
from .common import profile,relative_project,qa_config,require,sha256,write_json,report_for
from .server import run
from .performance import benchmark
from .static import validate_jar

def differences(a,b):
    return {phase:{metric:round(b[phase][metric]-a[phase][metric],3) if a[phase].get(metric) is not None and b[phase].get(metric) is not None else None for metric in ("effective_tps","mspt","rss_mib_process_tree")} for phase in ("idle","loaded")}

def main():
    ap=argparse.ArgumentParser();ap.add_argument("project");ap.add_argument("jar");ap.add_argument("--out",required=True);a=ap.parse_args()
    p=profile(a.project);q=qa_config(p);jar=Path(a.jar).resolve();out=Path(a.out).resolve();out.mkdir(parents=True,exist_ok=True)
    report=report_for(p);report.update(kind="p5-benchmark",eligible=False,status="failed",jar=jar.name,sha256=sha256(jar),machine={"platform":platform.platform(),"cpu_count":os.cpu_count()},phases={},scope="Sequential fresh worlds on one hosted runner; same Java, JVM heap cap, world seed and workload; not a statistically controlled hardware benchmark. No performance release threshold.")
    try:
        require(p["minecraft"].startswith("26.") and q["mode"]=="server" and not q.get("dependencies"),"Initial comparison supports standalone 26.x gameplay mods without extra dependencies")
        require(jar.parent==(relative_project(a.project)/"build/libs").resolve(),"Not final Gradle output")
        report["static"]=validate_jar(jar,p)
        settings=dict(q,scenario="tools/qa/scenarios/virtual-player-setup.json",dependencies=[])
        for name,baseline,mod in [("vanilla","vanilla",None),("loader-only","loader-only",None),("modded",None,jar)]:
            row={"kind":"p5-benchmark-phase","eligible":False,"checks":[],"status":"failed","qa_level":0}
            report["phases"][name]=row
            run(p,settings,mod,out/name,row,after_suites=benchmark,diagnostic_baseline=baseline)
            require(row.get("performance"),"Missing benchmark observations")
            if baseline:require(row.get("installed_jars")=={},"Baseline contains mods")
            row["status"]="measured"
            row.pop("qa_level",None)
            write_json(out/"comparison.json",report)
        phases={k:v["performance"] for k,v in report["phases"].items()}
        report["deltas"]={"loader_minus_vanilla":differences(phases["vanilla"],phases["loader-only"]),"modded_minus_loader":differences(phases["loader-only"],phases["modded"]),"modded_minus_vanilla":differences(phases["vanilla"],phases["modded"])}
        report["status"]="passed"
        print("P5 benchmark deltas:",json.dumps(report["deltas"]),flush=True)
        return 0
    except Exception as e:
        report["error"]=f"{type(e).__name__}: {e}";print("P5 BENCHMARK FAILED",report["error"],flush=True)
        return 1
    finally:
        report.pop("qa_level",None);write_json(out/"comparison.json",report)
if __name__=="__main__":raise SystemExit(main())
