"""Experimental visible DEV-client startup/screenshot under Xvfb, not production QA."""
import argparse
import json
import os
import re
import signal
import shutil
import struct
import subprocess
import time
from pathlib import Path
from .common import relative_project, profile, report_for, write_json, require
from .crash import analyze

FATAL=re.compile(r"MixinApplyError|InvalidInjectionException|A critical error occurred|Failed to initialize GLFW|Failed to start the game|The game crashed|ReportedException|Exception in thread \"Render thread\"",re.I)
def command(*args,timeout=15):
    return subprocess.run(args,capture_output=True,text=True,check=False,timeout=timeout)

def main():
    ap=argparse.ArgumentParser();ap.add_argument("project");ap.add_argument("--out",required=True);a=ap.parse_args()
    base=relative_project(a.project);p=profile(a.project);out=Path(a.out).resolve();out.mkdir(parents=True,exist_ok=True)
    report=report_for(p);report.update(mode="experimental-dev-client",status="failed",eligible=False)
    log=out/"client.log";screenshot=out/"client.png"
    proc=None
    try:
        require(os.getenv("DISPLAY"),"Run through xvfb-run")
        env=dict(os.environ,LIBGL_ALWAYS_SOFTWARE="1",ALSOFT_DRIVERS="null")
        probe=command("glxinfo","-B",timeout=25)
        (out/"glxinfo.txt").write_text(probe.stdout+"\n"+probe.stderr)
        report["glx_probe_exit"]=probe.returncode
        # Record the Vulkan software ICD if present; do not claim GPU rendering.
        icds=list(Path("/usr/share/vulkan/icd.d").glob("lvp_icd*.json"))
        if icds: env["VK_DRIVER_FILES"]=str(icds[0])
        if icds and shutil.which("vulkaninfo"):
            probe=command("vulkaninfo","--summary",timeout=25)
            (out/"vulkaninfo.txt").write_text(probe.stdout+"\n"+probe.stderr)
            report["vulkan_probe_exit"]=probe.returncode
        with log.open("w") as f:
            proc=subprocess.Popen(["gradle","--no-daemon","runClient"],cwd=base,env=env,stdout=f,stderr=subprocess.STDOUT,start_new_session=True)
            deadline=time.monotonic()+360
            visible_since=None;window=None
            while time.monotonic()<deadline:
                contents=log.read_text(errors="replace")
                require(proc.poll() is None,"Development client exited before visible window")
                require(not FATAL.search(contents),"Fatal client startup error")
                if "Failed to create backend OpenGL" in contents and "Failed to create backend Vulkan" in contents:
                    report["status"]="graphics-backend-unavailable"
                    raise RuntimeError("Both OpenGL and Vulkan backend creation failed; inspect graphics probes")
                found=command("xdotool","search","--onlyvisible","--name","Minecraft")
                candidates=[]
                for wid in found.stdout.splitlines():
                    if not wid.isdigit(): continue
                    geometry=command("xdotool","getwindowgeometry",wid).stdout
                    m=re.search(r"Geometry:\s*(\d+)x(\d+)",geometry)
                    if m and int(m[1])>=640 and int(m[2])>=360:candidates.append(wid)
                if candidates:
                    if candidates[-1]!=window:window=candidates[-1];visible_since=time.monotonic()
                    if time.monotonic()-visible_since>=15:
                        result=command("import","-window",window,str(screenshot))
                        require(result.returncode==0 and screenshot.is_file(),"Screenshot capture failed")
                        data=screenshot.read_bytes()
                        require(data[:8]==b"\x89PNG\r\n\x1a\n" and len(data)>5000,"Invalid/empty client screenshot")
                        w,h=struct.unpack(">II",data[16:24])
                        require(w>=640 and h>=360,"Client screenshot too small")
                        report.update(status="dev-client-window-observed",window=command("xdotool","getwindowname",window).stdout.strip(),screenshot=[w,h],manual_client_verified=False)
                        return 0
                else:visible_since=None;window=None
                time.sleep(2)
            raise TimeoutError("No stable Minecraft window observed")
    except Exception as e:
        report["error"]=str(e);report["diagnostics"]=analyze(log.read_text(errors="replace") if log.is_file() else str(e))
        print("EXPERIMENTAL CLIENT FAILED:",e)
        return 1
    finally:
        if proc is not None and proc.poll() is None:
            os.killpg(proc.pid,signal.SIGTERM)
            try:proc.wait(timeout=20)
            except subprocess.TimeoutExpired:os.killpg(proc.pid,signal.SIGKILL);proc.wait()
        write_json(out/"client-report.json",report)
        if log.is_file():print(log.read_text(errors="replace")[-10000:])

if __name__=="__main__":raise SystemExit(main())
