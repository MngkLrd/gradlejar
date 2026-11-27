"""Run network-player controls on isolated official Fabric with no mods. Never release evidence."""
import argparse
from pathlib import Path
from .common import REGISTRY, write_json
from .server import run
from .player_suite import SUPPORTED, callback

def main():
    ap=argparse.ArgumentParser()
    ap.add_argument("version")
    ap.add_argument("--out",required=True,type=Path)
    a=ap.parse_args()
    if ("fabric",a.version) not in SUPPORTED:
        ap.error("unsupported protocol")
    mc=REGISTRY["minecraft"][a.version]["loaders"]["fabric"]
    p={"slug":"bot-control","minecraft":a.version,"loader":"fabric"}
    q={"loader_version":mc["version"],"scenario":"tools/qa/scenarios/bot-control-world.json","player_scenario":"tools/qa/scenarios/bot-control-player.json"}
    report={"schema":1,"kind":"p5-benchmark-phase","eligible":False,"qa_level":0,"checks":[],"status":"failed","minecraft":a.version,"scope":"official Fabric loader without mods; network controls only"}
    a.out.mkdir(parents=True,exist_ok=True)
    try:
        run(p,q,None,a.out/"runtime",report,after_suites=callback(p,q),diagnostic_baseline="loader-only")
        report["status"]="passed"
        return 0
    except Exception as exc:
        report["error"]=str(exc)
        print("BOT CONTROL FAILED:",exc,flush=True)
        return 1
    finally:
        write_json(a.out/"report.json",report)

if __name__=="__main__":
    raise SystemExit(main())
