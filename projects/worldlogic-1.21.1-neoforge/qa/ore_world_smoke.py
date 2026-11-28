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
            river_regions=[(-24576,-32768),(-12288,-32768),(10240,-40960),(20480,-38912),(6144,14336),
                           (-18432,-30720),(-20480,-30720),(-10240,-32768),(8192,-38912),(18432,-38912),
                           (-32256,-18432),(-23040,-9216),(-13824,-4608),(-4608,4608),
                           (4608,9216),(13824,18432),(23040,27648),(32256,36864),
                           (-27648,23040),(-18432,32256),(18432,-23040),(27648,-13824)]
            for x,z in river_regions:
                raw=conn.cmd(f"execute positioned {x} 80 {z} run worldlogic river_probe")
                match=re.search(r"RIVER_PROBE x=(-?\d+) z=(-?\d+)",raw)
                if match:
                    rx,rz=(int(v) for v in match.groups())
                    if all((rx-x0)**2+(rz-z0)**2>=384**2 for _,x0,z0 in river_sites):
                        river_sites.append(("river_"+str(len(river_sites)),rx,rz))
                    if len(river_sites)>=5:break
            report["river_candidates"]=river_sites
            require(len(river_sites)>=3,"Insufficient independent modeled river catchments")
            sites.extend(river_sites)
            headwaters=[]
            for x,z in river_regions:
                raw=conn.cmd(f"execute positioned {x} 80 {z} run worldlogic river_probe headwater")
                match=re.search(r"RIVER_PROBE x=(-?\d+) z=(-?\d+) discharge=([0-9.]+)",raw)
                if match and 6<=float(match.group(3))<12:
                    rx,rz=map(int,match.groups()[:2])
                    headwaters.append(("headwater_"+str(len(headwaters)),rx,rz))
                    if len(headwaters)==2:break
            require(len(headwaters)>=1,"No independent small-stream candidate")
            report["headwater_candidates"]=headwaters
            sites.extend(headwaters)
            keys=("columns","ocean","wet","dry","inland","unknown","dryQuart","river","links","deep","cross")
            pattern="COAST_AUDIT "+" ".join(k+r"=(\d+)" for k in keys)
            report["coast_windows"]=[]
            report["biome_patches"]=[]
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
                        patch=conn.cmd(f"execute positioned {xx} 96 {zz} run worldlogic biome_patch_audit")
                        if "unavailable" in patch:continue
                        pm=re.search(r"BIOME_PATCH land=(\d+) links=(\d+) same=(\d+) extreme=(\d+) isolated=(\d+) classes=(\d+)",patch)
                        require(pm is not None,"Biome patch audit failed: "+patch)
                        report["biome_patches"].append(dict(zip(("land","links","same","extreme","isolated","classes"),map(int,pm.groups())),x=xx,z=zz,site=label))
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
            patches=report["biome_patches"]
            patch_totals={k:sum(v[k] for v in patches) for k in ("land","links","same","extreme","isolated")}
            report["biome_patch_totals"]=patch_totals
            require(patch_totals["land"]>=64 and patch_totals["links"]>=128,"Insufficient actual inland biome adjacency coverage")
            require(patch_totals["extreme"]<=patch_totals["links"]*.02,"Excessive direct arid/wet biome contacts")
            require(patch_totals["same"]>=patch_totals["links"]*.65,"Excessively fragmented inland biome patches")
            require(sum(w["river"]>=16 and w["links"]>=12 and w["deep"]>=4 and w["cross"]>=2 for w in windows if w["site"].startswith("river_"))>=3,"Fewer than three generated connected river catchments")
            require(any(w["river"]>=8 and w["links"]>=4 for w in windows if w["site"].startswith("headwater_")),"No generated connected headwater stream")
            # Natural pools: verify actual generated water and connected surface, not
            # just a closed-rim planning fixture. Search bounded separated land tiles.
            report["basin_candidates"]=[]
            report["basin_windows"]=[]
            basin_anchors=[]
            for _,x,z in river_sites+headwaters:
                for dx,dz in [(0,0),(512,0),(-512,0),(0,512),(0,-512),(512,512),(-512,-512)]:
                    k=((x+dx)//512*512,(z+dz)//512*512)
                    if k not in basin_anchors:basin_anchors.append(k)
            for x,z in basin_anchors[:42]:
                raw=conn.cmd(f"execute positioned {x} 96 {z} run worldlogic basin_probe")
                m=re.search(r"BASIN_PROBE x=(-?\d+) z=(-?\d+) radius=(\d+) stage=(-?\d+) kind=(\w+)",raw)
                if not m:continue
                xx,zz,r,y=map(int,m.groups()[:4]);kind=m.group(5)
                report["basin_candidates"].append(dict(x=xx,z=zz,radius=r,stage=y,kind=kind))
                force=f"{xx-r-16} {zz-r-16} {xx+r+16} {zz+r+16}"
                conn.cmd("forceload add "+force)
                deadline=time.monotonic()+240
                while time.monotonic()<deadline:
                    raw=conn.cmd(f"execute positioned {xx} {y} {zz} run worldlogic basin_audit")
                    m=re.search(r"BASIN_AUDIT wet=(\d+) connected=(\d+) deep=(\d+) rimLeak=(\d+) kind=(\w+)",raw)
                    if m:break
                    time.sleep(3)
                conn.cmd("forceload remove "+force)
                require(m is not None,"Basin survey chunks not ready")
                wet,connected,deep,leak=map(int,m.groups()[:4])
                report["basin_windows"].append(dict(x=xx,z=zz,wet=wet,connected=connected,deep=deep,rimLeak=leak,kind=kind))
                if wet>=12 and connected>=wet*.80 and deep>=3 and leak==0:break
                if len(report["basin_windows"])>=3:break
            require(any(w["wet"]>=12 and w["connected"]>=w["wet"]*.80 and w["deep"]>=3 and w["rimLeak"]==0 for w in report["basin_windows"]),"No confirmed natural connected closed basin")
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
            # Warm coastal reef audit: actual living coral blocks and surrounding
            # native seawater, not the predicted habitat alone.
            report["reef_candidates"]=[]
            report["reef_windows"]=[]
            reef_anchors=[(x,z) for label,x,z in sites if label in ("tropical","subtropical","warm_deep","tropical_coast")]
            reef_anchors.extend([(-28672,-39936),(-26624,-35840),(-4096,-39936),(-5120,-40960),(10240,-40960),(20480,-38912)])
            for x,z in reef_anchors:
                raw=conn.cmd(f"execute positioned {x} 63 {z} run worldlogic reef_probe")
                m=re.search(r"REEF_PROBE x=(-?\d+) z=(-?\d+) zone=(\w+)",raw)
                if not m:continue
                xx,zz=map(int,m.groups()[:2]);zone=m.group(3)
                if any((xx-v["x"])**2+(zz-v["z"])**2<128**2 for v in report["reef_candidates"]):continue
                report["reef_candidates"].append(dict(x=xx,z=zz,zone=zone))
                ax,az=xx//16*16,zz//16*16
                force=f"{ax-16} {az-16} {ax+31} {az+31}"
                conn.cmd("forceload add "+force)
                pending={(ax+dx*16,az+dz*16) for dx in (-1,0,1) for dz in (-1,0,1)}
                rows=[];deadline=time.monotonic()+240
                while pending and time.monotonic()<deadline:
                    for qx,qz in sorted(pending):
                        raw=conn.cmd(f"execute positioned {qx} 63 {qz} run worldlogic reef_audit")
                        if "unavailable" in raw:continue
                        m=re.search(r"REEF_AUDIT colonies=(\d+) live=(\d+) species=(\d+) wrong=(\d+) nativeAbove=(\d+)",raw)
                        require(m is not None,"Reef audit failed: "+raw)
                        row=dict(zip(("colonies","live","species","wrong","nativeAbove"),map(int,m.groups())),x=qx,z=qz)
                        rows.append(row);pending.remove((qx,qz))
                    if pending:time.sleep(2)
                require(not pending,"Reef survey not fully loaded")
                conn.cmd("forceload remove "+force)
                sums={k:sum(v[k] for v in rows) for k in ("colonies","live","wrong","nativeAbove")}
                report["reef_windows"].append(dict(x=xx,z=zz,zone=zone,chunks=rows,**sums,species=max(v["species"] for v in rows)))
                if len(report["reef_windows"])>=3:break
            require(any(w["colonies"]>=12 and w["live"]>=12 and w["species"]>=2 and w["nativeAbove"]>=w["colonies"]*.8 for w in report["reef_windows"]),"No confirmed living warm-water reef in generated chunks")
            require(all(w["wrong"]==0 for w in report["reef_windows"]),"Coral in unsuitable biome or exposed column")
            conn.cmd("save-all flush")
            require(not FATAL.search(log.read_text(errors="replace")),"Fatal generation error")
            try:conn.cmd("stop")
            except (OSError,EOFError):pass
            conn.close();conn=None;proc.wait(timeout=60)
            require(proc.returncode==0,"Unclean server shutdown")
            # Compare the final ground profiles, including all carvers and decorations,
            # against the retained immutable RC22 settings at identical coordinates.
            conn=None;proc=None
            props=root/"server.properties"
            original=props.read_text()
            # The legacy preset is intentionally hidden in the default preset list.
            # DedicatedServerProperties can fall back to normal for its ID. Seed a
            # separate EMPTY reference save with the retained preset's exact dimension
            # and generator NBT instead; never copy generated regions or player data.
            legacy=json.loads((PROJECT/"src/main/resources/data/worldlogic/worldgen/world_preset/earthlike_climate.json").read_text())["dimensions"]["minecraft:overworld"]
            reference=root/"rc22-reference"
            reference.mkdir(exist_ok=False)
            legacy_level=nbtlib.load(root/"ore-world"/"level.dat")
            data=legacy_level["Data"]
            data["LevelName"]=nbtlib.String("rc22-reference")
            data.pop("Player",None)
            data["WorldGenSettings"]["dimensions"]["minecraft:overworld"]=nbtlib.Compound({
                "type":nbtlib.String(legacy["type"]),
                "generator":nbtlib.Compound({
                    "type":nbtlib.String(legacy["generator"]["type"]),
                    "settings":nbtlib.String(legacy["generator"]["settings"]),
                    "biome_source":nbtlib.Compound({k:nbtlib.String(v) for k,v in legacy["generator"]["biome_source"].items()})
                })
            })
            legacy_level.save(reference/"level.dat",gzipped=True)
            require(not (reference/"region").exists(),"Reference must have no generated regions")
            props.write_text(original.replace("level-name=ore-world","level-name=rc22-reference").replace("level-type=minecraft:normal","level-type=worldlogic:earthlike_climate"))
            def snapshot(connection,positions):
                result={}
                for x,z in positions:
                    force=f"{x} {z} {x+15} {z+15}"
                    connection.cmd("forceload add "+force)
                    deadline=time.monotonic()+120
                    while time.monotonic()<deadline:
                        raw=connection.cmd(f"execute positioned {x} 96 {z} run worldlogic surface_audit")
                        if "unavailable" not in raw:break
                        time.sleep(2)
                    require(raw.startswith("SURFACE_AUDIT ") and "unavailable" not in raw,"Surface audit unavailable "+str((x,z)))
                    entries=[v.split(",") for v in raw.split("SURFACE_AUDIT ",1)[1].strip().split(";")]
                    require(len(entries)==64 and all(len(v)==4 for v in entries),"Incomplete final surface profile")
                    result[f"{x},{z}"]=entries
                    connection.cmd("forceload remove "+force)
                return result
            positions=[(0,0),(512,512)]+[(x//16*16,z//16*16) for _,x,z in sites[:6]]
            positions=list(dict.fromkeys(positions))
            comparisons={}
            for name,properties in [("rc22",props.read_text()),("current",original)]:
                props.write_text(properties)
                with (logs/f"surface-{name}.log").open("w") as profile_log:
                    proc=subprocess.Popen(cmd,cwd=root,stdout=profile_log,stderr=subprocess.STDOUT,start_new_session=True)
                    deadline=time.monotonic()+420
                    while time.monotonic()<deadline:
                        require(proc.poll() is None,"Surface reference startup failed "+name)
                        if "Done (" in (logs/f"surface-{name}.log").read_text(errors="replace"):
                            try:conn=Rcon(rport,password);conn.sock.settimeout(120);break
                            except (OSError,EOFError):pass
                        time.sleep(2)
                    require(conn is not None,"Surface reference startup timeout "+name)
                    conn.cmd("save-all flush")
                    folder="rc22-reference" if name=="rc22" else "ore-world"
                    actual=nbtlib.load(root/folder/"level.dat")["Data"]["WorldGenSettings"]["dimensions"]["minecraft:overworld"]["generator"]
                    expected="worldlogic:earthlike_climate" if name=="rc22" else "worldlogic:realistic_unified"
                    require(str(actual["settings"])==expected,"Wrong surface reference generator "+name)
                    comparisons[name]=snapshot(conn,positions)
                    try:conn.cmd("stop")
                    except (OSError,EOFError):pass
                    conn.close();conn=None;proc.wait(timeout=60);require(proc.returncode==0,"Unclean comparison shutdown");proc=None
            deltas=[];all_deltas=[];sand={"rc22":0,"current":0};excluded=0
            for key,base in comparisons["rc22"].items():
                for a,b in zip(base,comparisons["current"][key]):
                    if a[1]=="?" or b[1]=="?":excluded+=1;continue
                    delta=abs(int(a[0])-int(b[0]));all_deltas.append(delta)
                    if int(a[2])==-999 and int(b[2])==-999:deltas.append(delta)
                    for name,v in [("rc22",a),("current",b)]:
                        if v[1]=="s" and int(v[0])>70:sand[name]+=1
            require(len(all_deltas)>=len(positions)*48,"Insufficient observed final terrain columns")
            report["surface_comparison"]={"positions":positions,"profiles":comparisons,"all_columns":len(all_deltas),"dry_columns":len(deltas),"unknown":excluded,"mean_absolute_height_delta":sum(all_deltas)/len(all_deltas),"dry_columns_within_8":sum(d<=8 for d in deltas),"high_sand_columns":sand}
            # Guard broad dry relief; intended caves, water channels and different surface
            # features may differ. Report rather than hide every material/height delta.
            require(not deltas or sum(d<=8 for d in deltas)>=len(deltas)*.80,"Broad final dry terrain regression versus RC22")
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
