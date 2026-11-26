"""Install an official loader and run the actual verified JAR in an isolated server."""
import hashlib
import json
import os
import re
import secrets
import shutil
import signal
import socket
import struct
import subprocess
import time
import urllib.request
import zipfile
from pathlib import Path
from .common import REGISTRY, ROOT, require, sha256, safe_path

def download(url, path, expected=None):
    require(url.startswith("https://"),"HTTPS required")
    path=Path(path)
    request=urllib.request.Request(url,headers={"User-Agent":"MineMods-QA/2.0"})
    with urllib.request.urlopen(request,timeout=90) as r, path.open("wb") as f:
        shutil.copyfileobj(r,f)
    require(zipfile.is_zipfile(path),"Downloaded file is not a JAR: "+url)
    if expected: require(sha256(path)==expected,"Dependency checksum mismatch: "+path.name)
    return path

def install(p,q,root,log):
    mc,l,v=p["minecraft"],p["loader"],q["loader_version"]
    if l=="fabric":
        url=f"https://meta.fabricmc.net/v2/versions/loader/{mc}/{v}/{REGISTRY['fabric_installer']}/server/jar"
        download(url,root/"fabric-server-launch.jar")
        return ["java","-Xmx2G","-jar","fabric-server-launch.jar","nogui"]
    if l=="forge":
        coord=f"{mc}-{v}"; artifact="forge"; group="net/minecraftforge/forge"
        repo="https://maven.minecraftforge.net"
    elif mc=="1.20.1":
        coord=f"{mc}-{v}"; artifact="forge"; group="net/neoforged/forge"
        repo="https://maven.neoforged.net/releases"
    else:
        coord=v; artifact="neoforge"; group="net/neoforged/neoforge"
        repo="https://maven.neoforged.net/releases"
    installer=download(f"{repo}/{group}/{coord}/{artifact}-{coord}-installer.jar",root/"installer.jar")
    with (log/"installer.log").open("w") as out:
        subprocess.run(["java","-jar",str(installer),"--installServer"],cwd=root,stdout=out,stderr=subprocess.STDOUT,check=True,timeout=420)
    # The official installer writes run.sh and its version-specific argument file.
    args=root/"libraries"/group/coord/"unix_args.txt"
    if args.is_file(): return ["java","-Xmx2G","@"+str(args.relative_to(root)),"nogui"]
    run=root/"run.sh"
    require(run.is_file(),"Installer did not produce a dedicated server launcher")
    (root/"user_jvm_args.txt").write_text("-Xmx2G\n",encoding="utf-8")
    return ["bash","run.sh","nogui"]

def free_port():
    with socket.socket() as s:
        s.bind(("127.0.0.1",0))
        return s.getsockname()[1]

class Rcon:
    def __init__(self,port,password):
        self.sock=socket.create_connection(("127.0.0.1",port),timeout=5)
        self.sock.settimeout(15)
        try:
            self.send(1,3,password)
            i,t,_=self.receive()
            if t!=2 and i!=-1: i,t,_=self.receive()
            require(i==1 and t==2,"RCON auth failed")
        except BaseException:
            self.close()
            raise
    def exact(self,n):
        data=bytearray()
        while len(data)<n:
            chunk=self.sock.recv(n-len(data))
            if not chunk: raise EOFError("RCON closed")
            data.extend(chunk)
        return bytes(data)
    def send(self,i,t,body):
        b=struct.pack("<ii",i,t)+body.encode()+b"\0\0"
        self.sock.sendall(struct.pack("<i",len(b))+b)
    def receive(self):
        n=struct.unpack("<i",self.exact(4))[0]
        require(10<=n<=4*1024*1024,"Bad RCON packet length")
        b=self.exact(n)
        i,t=struct.unpack("<ii",b[:8])
        return i,t,b[8:-2].decode(errors="replace")
    def cmd(self,command):
        self.send(2,2,command)
        i,t,result=self.receive()
        require(i==2,"RCON request ID mismatch")
        return result
    def close(self):
        self.sock.close()

def scenario_steps(scenario,loader):
    require(isinstance(scenario,dict) and scenario.get("schema")==1,"Invalid scenario schema")
    steps=scenario.get("steps")
    require(isinstance(steps,list) and 1<=len(steps)<=250,"Scenario must contain 1..250 steps")
    active=[]
    for step in steps:
        require(isinstance(step,dict),"Invalid scenario step")
        if "loaders" in step and loader not in step["loaders"]: continue
        if "wait" in step:
            require(set(step)<={"wait","loaders"} and isinstance(step["wait"],(int,float)) and 0<=step["wait"]<=30,"Invalid wait")
        else:
            require(isinstance(step.get("command"),str) and 0<len(step["command"])<=1000 and "\n" not in step["command"],"Invalid RCON command")
            require(step["command"].split()[0].lower() not in ("stop","op","deop","ban","pardon","whitelist"),"Command not allowed in QA scenario")
            for key in ("expect","reject"):
                if key in step: re.compile(step[key],re.I)
            require("expect" in step or "reject" in step,"Every command needs an explicit expectation")
        active.append(step)
    require(sum("expect" in s and s.get("assertion",False) for s in active)>=1,"Scenario needs positive gameplay assertions for this loader")
    require(sum(s.get("wait",0) for s in active)<=150,"Scenario wait budget exceeded")
    return active

def play(rcon,steps,report,suite=None):
    start=len(report['checks'])
    for step in steps:
        if "wait" in step:
            time.sleep(step["wait"])
            continue
        result=rcon.cmd(step["command"])
        ok=("expect" not in step or re.search(step["expect"],result,re.I)) and ("reject" not in step or not re.search(step["reject"],result,re.I))
        row={"name":step.get("name",step["command"]),"command":step["command"],"result":result[:1500],"passed":bool(ok),"assertion":bool(step.get("assertion",False))}
        if suite is not None: row["suite"]=suite
        report["checks"].append(row)
        print("QA",row["name"],"=>",result,flush=True)
        require(ok,"Scenario assertion failed: "+row["name"]+" => "+result)
    require(any(c.get("assertion") and c["passed"] for c in report["checks"][start:]),"No scenario assertions executed")

FATAL=re.compile(r"Failed to load registries|Errors in currently selected data packs|Exception in server tick loop|A critical error occurred|MixinApplyError|InvalidInjectionException|Failed to start the minecraft server",re.I)
def run(p,q,jar,work,report,*,extra_jars=(),scenarios=None,after_suites=None):
    work=Path(work).resolve()
    work.mkdir(parents=True,exist_ok=True)
    root=work/"server"
    require(not root.exists(),"QA server directory already exists; use a fresh directory")
    root.mkdir()
    logs=work/"logs"; logs.mkdir()
    if scenarios is None:
        scenarios=[(p["slug"],q["scenario"])]
    suites=[(label,scenario_steps(json.loads(safe_path(ROOT,path).read_text("utf-8")),p["loader"])) for label,path in scenarios]
    require(suites and len({label for label,_ in suites})==len(suites),"Missing or duplicate scenario suites")
    cmd=install(p,q,root,logs)
    mods=root/"mods"; mods.mkdir(exist_ok=True)
    shutil.copy2(jar,mods/Path(jar).name)
    require(sha256(mods/Path(jar).name)==sha256(jar),"Installed JAR checksum mismatch")
    installed={Path(jar).name:sha256(jar)}
    for extra in extra_jars:
        extra=Path(extra)
        require(extra.is_file() and extra.name.endswith(".jar") and extra.name not in installed,"Missing or duplicate pack JAR")
        shutil.copy2(extra,mods/extra.name)
        require(sha256(mods/extra.name)==sha256(extra),"Installed pack JAR checksum mismatch")
        installed[extra.name]=sha256(extra)
    for dep in q.get("dependencies",[]):
        require(dep["name"] not in installed,"Duplicate dependency JAR: "+dep["name"])
        download(dep["url"],mods/dep["name"],dep["sha256"])
        installed[dep["name"]]=dep["sha256"]
    report["installed_jars"]=installed
    password=secrets.token_hex(18); port=free_port(); rport=free_port()
    while port==rport: rport=free_port()
    (root/"eula.txt").write_text("eula=true\n")
    (root/"server.properties").write_text("\n".join([
        "online-mode=false","server-ip=127.0.0.1",f"server-port={port}","enable-rcon=true",f"rcon.port={rport}",f"rcon.password={password}",
        "level-name=qa-world","level-seed=824681","level-type=minecraft:flat","view-distance=3","simulation-distance=3",
        "spawn-protection=0","max-tick-time=-1","white-list=false","motd=MineMods isolated CI world",""
    ]))
    log=logs/"server.log"
    conn=None
    with log.open("w") as out:
        proc=subprocess.Popen(cmd,cwd=root,stdin=subprocess.DEVNULL,stdout=out,stderr=subprocess.STDOUT,start_new_session=True)
        try:
            deadline=time.monotonic()+240
            while time.monotonic()<deadline:
                contents=log.read_text(errors="replace")
                if proc.poll() is not None: raise RuntimeError("Server exited before startup: "+str(proc.returncode))
                if FATAL.search(contents): raise RuntimeError("Fatal Minecraft startup log; inspect server.log")
                if "Done (" in contents:
                    try: conn=Rcon(rport,password);break
                    except (OSError,EOFError): pass
                time.sleep(1)
            require(conn is not None,"Production server or RCON did not start")
            report["qa_level"]=3
            report["runtime"]="production-jar"
            report["installed_sha256"]=sha256(mods/Path(jar).name)
            for label,steps in suites:
                play(conn,steps,report,label)
            if after_suites is not None:
                after_suites(conn,port,proc,report,work)
            require(not FATAL.search(log.read_text(errors="replace")),"Fatal Minecraft error during scenario")
            try: conn.cmd("stop")
            except (OSError,EOFError): pass
            conn.close();conn=None
            proc.wait(timeout=40)
            require(proc.returncode==0,"Server failed clean shutdown")
            require(not FATAL.search(log.read_text(errors="replace")),"Fatal Minecraft error during shutdown")
            report["qa_level"]=4
        finally:
            if conn is not None:
                try: conn.cmd("stop")
                except Exception: pass
                conn.close()
            if proc.poll() is None:
                os.killpg(proc.pid,signal.SIGTERM)
                try: proc.wait(timeout=15)
                except subprocess.TimeoutExpired:
                    os.killpg(proc.pid,signal.SIGKILL);proc.wait()
            print(log.read_text(errors="replace")[-12000:],flush=True)
