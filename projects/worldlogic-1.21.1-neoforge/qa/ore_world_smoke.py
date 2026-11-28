"""Supplementary Core-only ore QA on actual full generated default-world chunks."""
import json, re, secrets, shutil, subprocess, sys, time
from pathlib import Path
import nbtlib
from tools.qa.server import install, free_port, Rcon, FATAL
from tools.qa.common import require, sha256

PROJECT=Path("projects/worldlogic-1.21.1-neoforge")
def main():
    inputs=Path(sys.argv[1]).resolve()
    work=Path(sys.argv[2]).resolve()
    work.mkdir(parents=True, exist_ok=False)
    logs=work/"logs"; logs.mkdir()
    root=work/"server"; root.mkdir()
    profile=json.loads((PROJECT/"build-profile.json").read_text())
    jars=list(inputs.rglob(profile["jar_glob"]))
    require(len(jars)==1,"Expected exact production JAR")
    jar=jars[0]; digest=sha256(jar)
    reports=[json.loads(f.read_text()) for f in inputs.rglob("qa-report-*.json")]
    valid=[r for r in reports if r.get("jar")==jar.name and r.get("eligible") is True and r.get("sha256")==digest]
    require(len(valid)==1,"Exact eligible Build Hub report required")
    report={"jar":jar.name,"sha256":digest,"seed":1234,"status":"failed","chunks":[]}
    proc=conn=None
    try:
        cmd=install(profile,profile["qa"],root,logs)
        mods=root/"mods"; mods.mkdir(); shutil.copy2(jar,mods/jar.name)
        require(sha256(mods/jar.name)==digest,"Installed JAR mismatch")
        password=secrets.token_hex(18); port,rport=free_port(),free_port()
        while port==rport:rport=free_port()
        (root/"eula.txt").write_text("eula=true\n")
        (root/"server.properties").write_text("\n".join(["online-mode=false","server-ip=127.0.0.1",f"server-port={port}","enable-rcon=true",f"rcon.port={rport}",f"rcon.password={password}","level-name=ore-world","level-seed=1234","level-type=minecraft:normal","view-distance=3","simulation-distance=3","spawn-protection=0","max-tick-time=-1",""]))
        log=logs/"server.log"
        with log.open("w") as stream:
            proc=subprocess.Popen(cmd,cwd=root,stdout=stream,stderr=subprocess.STDOUT,start_new_session=True)
            deadline=time.monotonic()+420
            while time.monotonic()<deadline:
                content=log.read_text(errors="replace")
                require(proc.poll() is None and not FATAL.search(content),"Custom world startup failure")
                if "Done (" in content:
                    try:
                        conn=Rcon(rport,password);conn.sock.settimeout(120);break
                    except (OSError,EOFError):pass
                time.sleep(2)
            require(conn is not None,"Custom world startup timeout")
            conn.cmd("save-all flush")
            level=nbtlib.load(root/"ore-world"/"level.dat")
            generator=level["Data"]["WorldGenSettings"]["dimensions"]["minecraft:overworld"]["generator"]
            require(str(generator["settings"])=="worldlogic:realistic_unified","Default world does not use Realistic geology")
            # Two ordinary 3x3 windows, not selected by ore richness.
            for ax,az in [(0,0),(512,512)]:
                force=f"{ax-16} {az-16} {ax+31} {az+31}"
                conn.cmd("forceload add "+force)
                deadline=time.monotonic()+180
                pending={(ax+dx*16,az+dz*16) for dx in (-1,0,1) for dz in (-1,0,1)}
                while pending and time.monotonic()<deadline:
                    for x,z in sorted(pending):
                        raw=conn.cmd(f"execute positioned {x} 0 {z} run worldlogic ore_audit")
                        if "unavailable:" in raw:continue
                        match=re.search(r"ORE_AUDIT total=(\d+) deep=(\d+) expected=(\d+) matched=(\d+) missedRock=(\d+)",raw)
                        require(match is not None,"Ore audit failed: "+raw)
                        row=dict(zip(("total","deep","expected","matched","missedRock"),map(int,match.groups())))
                        row.update(x=x,z=z,raw=raw);report["chunks"].append(row);pending.remove((x,z))
                        print(json.dumps(row),flush=True)
                    if pending:time.sleep(3)
                require(not pending,"Full chunks not available: "+str(pending))
                conn.cmd("forceload remove "+force)
            expected=sum(c["expected"] for c in report["chunks"])
            matched=sum(c["matched"] for c in report["chunks"])
            deep=sum(c["deep"] for c in report["chunks"])
            missed=sum(c["missedRock"] for c in report["chunks"])
            report.update(expected=expected,matched=matched,deep=deep,missedRock=missed)
            require(len(report["chunks"])==18 and expected>=30 and deep>=30,"Too little actual deep ore for a meaningful test")
            require(matched>=expected*.85 and missed<=expected*.10,"Seeded geological ore missing from actual generated rock")
            # Known climate survey coordinates on seed 1234, all nine ocean classes,
            # coast transitions and ordinary inland controls. Generate real full chunks.
            sites=[("tropical",-28672,-40960),("warm_deep",-4096,-38912),
                   ("subtropical",-26624,-34816),("temperate",-26624,-32768),
                   ("temperate_deep",0,-32768),("cold",-18432,-30720),
                   ("cold_deep",-28672,-28672),("polar",-8192,-26624),
                   ("polar_deep",-10240,-22528),("tropical_coast",-4096,-40960),
                   ("temperate_coast",-18432,-28672),("prairie",-24576,-32768),
                   ("marsh",-12288,-32768),("inland_control",-954,497)]
            # Probe five separated catchments, then require real generated water
            # and neighbor links instead of merely a predicted river line.
            river_sites=[]
            for x,z in [(-24576,-32768),(-12288,-32768),(10240,-40960),(20480,-38912),(6144,14336)]:
                raw=conn.cmd(f"execute positioned {x} 80 {z} run worldlogic river_probe")
                match=re.search(r"RIVER_PROBE x=(-?\d+) z=(-?\d+)",raw)
                if match:
                    rx,rz=(int(v) for v in match.groups())
                    river_sites.append(("river_"+str(len(river_sites)),rx,rz))
            report["river_candidates"]=river_sites
            require(len(river_sites)>=3,"Insufficient independent modeled river catchments")
            sites.extend(river_sites)
            headwaters=[]
            for x,z in [(-24576,-32768),(-12288,-32768),(10240,-40960),(20480,-38912),(6144,14336)]:
                raw=conn.cmd(f"execute positioned {x} 80 {z} run worldlogic river_probe headwater")
                match=re.search(r"RIVER_PROBE x=(-?\d+) z=(-?\d+) discharge=([0-9.]+)",raw)
                if match and 6<=float(match.group(3))<12:
                    rx,rz=map(int,match.groups()[:2])
                    headwaters.append(("headwater_"+str(len(headwaters)),rx,rz))
                    if len(headwaters)==2:break
            require(len(headwaters)>=1,"No independent small-stream candidate")
            report["headwater_candidates"]=headwaters
            sites.extend(headwaters)
            keys=("columns","ocean","wet","dry","inland","unknown","dryQuart","river","links")
            pattern="COAST_AUDIT "+" ".join(k+r"=(\d+)" for k in keys)
            report["coast_windows"]=[]
            for label,x,z in sites:
                ax,az=x//16*16,z//16*16
                # A 5x5 loaded halo surrounds the 3x3 inspected chunks, so dry
                # interior detection can independently check nearby water.
                force=f"{ax-32} {az-32} {ax+47} {az+47}"
                conn.cmd("forceload add "+force)
                pending={(ax+dx*16,az+dz*16) for dx in (-1,0,1) for dz in (-1,0,1)}
                rows=[];deadline=time.monotonic()+240
                while pending and time.monotonic()<deadline:
                    for xx,zz in sorted(pending):
                        raw=conn.cmd(f"execute positioned {xx} 96 {zz} run worldlogic coast_audit")
                        if "unavailable" in raw:continue
                        match=re.search(pattern,raw)
                        require(match is not None,"Coast audit failed: "+raw)
                        row=dict(zip(keys,map(int,match.groups())))
                        if row["unknown"]:continue
                        row.update(x=xx,z=zz,raw=raw);rows.append(row);pending.remove((xx,zz))
                        print(json.dumps(row),flush=True)
                    if pending:time.sleep(3)
                require(not pending,"Survey chunks or halo not fully available: "+label)
                sums={k:sum(c[k] for c in rows) for k in keys}
                report["coast_windows"].append({"site":label,"chunks":rows,**sums})
                conn.cmd("forceload remove "+force)
            windows=report["coast_windows"]
            totals={k:sum(w[k] for w in windows) for k in keys}
            report["coast_totals"]=totals
            require(totals["ocean"]>=1024 and totals["wet"]>=1024,"Insufficient real ocean coverage")
            require(totals["inland"]==0 and totals["dryQuart"]==0,"Dry land labeled ocean; inspect logged coordinates")
            require(sum(w["river"]>=16 and w["links"]>=12 for w in windows if w["site"].startswith("river_"))>=3,"Fewer than three generated connected river catchments")
            require(any(w["river"]>=8 and w["links"]>=4 for w in windows if w["site"].startswith("headwater_")),"No generated connected headwater stream")
            # Retain independent marine substrate, native-water and flora assertions.
            ax,az=-18432,-30720
            force=f"{ax-16} {az-16} {ax+31} {az+31}"
            conn.cmd("forceload add "+force)
            pending={(ax+dx*16,az+dz*16) for dx in (-1,0,1) for dz in (-1,0,1)}
            marine=[];deadline=time.monotonic()+180
            keys=("ocean","wet","wrongLand","sea","stone","sediment","dirt","habitat","plants","nativePlants","vanillaPlantWater")
            pattern="MARINE_AUDIT "+" ".join(k+r"=(\d+)" for k in keys)
            while pending and time.monotonic()<deadline:
                for x,z in sorted(pending):
                    raw=conn.cmd(f"execute positioned {x} 63 {z} run worldlogic marine_audit")
                    if "unavailable" in raw:continue
                    match=re.search(pattern,raw)
                    require(match is not None,"Marine audit failed: "+raw)
                    row=dict(zip(keys,map(int,match.groups())));row.update(x=x,z=z);marine.append(row);pending.remove((x,z))
                    print(json.dumps(row),flush=True)
                if pending:time.sleep(3)
            require(not pending,"Ocean chunks not fully generated")
            totals={k:sum(c[k] for c in marine) for k in keys}
            report["marine"]={"chunks":marine,**totals}
            require(totals["wet"]>=64 and totals["sea"]>=totals["wet"]*.8,"Actual ocean lacks native seawater")
            require(totals["wrongLand"]==0,"Dry land labelled ocean")
            require(totals["stone"]>0 and totals["sediment"]>0 and totals["dirt"]<=totals["wet"]*.1,"Seabed is barren or incorrectly soil-covered")
            require(totals["habitat"]>0,"No viable submerged fish habitat")
            require(totals["plants"]>0 and totals["nativePlants"]==totals["plants"] and totals["vanillaPlantWater"]==0,"Marine plants missing or contain non-native water")
            conn.cmd("forceload remove "+force)
            conn.cmd("save-all flush")
            require(not FATAL.search(log.read_text(errors="replace")),"Fatal generation error")
            try:conn.cmd("stop")
            except (OSError,EOFError):pass
            conn.close();conn=None;proc.wait(timeout=60)
            require(proc.returncode==0,"Unclean server shutdown")
            report["status"]="passed"
    finally:
        if conn is not None:
            try:conn.cmd("stop")
            except Exception:pass
            conn.close()
        if proc is not None and proc.poll() is None:
            proc.terminate()
            try:proc.wait(timeout=20)
            except subprocess.TimeoutExpired:proc.kill();proc.wait()
        (work/"ore-world-report.json").write_text(json.dumps(report,indent=2)+"\n")
        print(json.dumps(report,indent=2),flush=True)
if __name__=="__main__":main()
