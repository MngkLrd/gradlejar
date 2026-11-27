"""Production-only RC20 regression: fresh geography worlds on baseline and reported Forge."""
import hashlib, json, os, pathlib, re, shutil, signal, subprocess, sys, time, urllib.request, zipfile

project = pathlib.Path(sys.argv[1]).resolve()
base = project / "build" / "worldlogic-smoke"
base.mkdir(parents=True, exist_ok=True)
jars = [p for p in (project/"build"/"libs").glob("worldlogic-*.jar") if not any(k in p.name for k in ("sources","dev","javadoc"))]
assert len(jars)==1, jars
jar = jars[0]
digest=hashlib.sha256(jar.read_bytes()).hexdigest()
print("WORLDLOGIC_EXACT_JAR", jar.name, digest, flush=True)
with zipfile.ZipFile(jar) as z:
    tag=json.loads(z.read("data/minecraft/tags/worldgen/world_preset/normal.json"))
    assert tag == {"replace":False,"values":["worldlogic:earthlike_climate"]}, tag
    for name in ("realistic","earthlike","earthlike_climate"):
        z.read(f"data/worldlogic/worldgen/world_preset/{name}.json")
        z.read(f"data/worldlogic/worldgen/noise_settings/{name}.json")
    old=json.loads(z.read("data/worldlogic/worldgen/noise_settings/earthlike.json"))
    new=json.loads(z.read("data/worldlogic/worldgen/noise_settings/earthlike_climate.json"))
    assert new["noise_router"]["temperature"]=="worldlogic:earthlike_climate/temperature"
    assert new["noise_router"]["vegetation"]=="worldlogic:earthlike_climate/humidity"
    target=new["spawn_target"]
    new["noise_router"]["temperature"]=old["noise_router"]["temperature"]
    new["noise_router"]["vegetation"]=old["noise_router"]["vegetation"]
    assert new==old, "unexpected change to terrain settings"
    assert len(target)==1 and target[0]["weirdness"]==[-1,1] and target[0]["continentalness"][0]>0, target

def install(loader):
    root=base/loader
    root.mkdir(exist_ok=True)
    version="1.21.1-"+loader
    installer=root/"forge-installer.jar"
    url=f"https://maven.minecraftforge.net/net/minecraftforge/forge/{version}/forge-{version}-installer.jar"
    req=urllib.request.Request(url,headers={"User-Agent":"Gradle/8.10.2 World Logic QA"})
    with urllib.request.urlopen(req,timeout=30) as response, installer.open("wb") as out:
        shutil.copyfileobj(response,out)
    assert zipfile.is_zipfile(installer), "invalid installer"
    subprocess.run(["java","-jar",str(installer),"--installServer"],cwd=root,check=True,timeout=180)
    mods=root/"mods"
    mods.mkdir(exist_ok=True)
    shutil.copy2(jar,mods/jar.name)
    assert hashlib.sha256((mods/jar.name).read_bytes()).hexdigest()==digest
    args=root/"libraries"/"net"/"minecraftforge"/"forge"/version/"unix_args.txt"
    assert args.is_file(),args
    (root/"eula.txt").write_text("eula=true\n")
    return root,args

def run(root,args,seed):
    world=f"worldlogic-{seed}"
    shutil.rmtree(root/world,ignore_errors=True)
    (root/"server.properties").write_text(f"online-mode=false\nserver-ip=127.0.0.1\nlevel-name={world}\nlevel-seed={seed}\nlevel-type=worldlogic:earthlike_climate\nview-distance=6\nsimulation-distance=6\nmax-tick-time=-1\n")
    log=root/f"seed-{seed}.log"
    start=time.monotonic()
    with log.open("w") as out:
        process=subprocess.Popen(["java","-Xmx2G","-Dworldlogic.ci=true","@"+str(args),"nogui"],cwd=root,stdin=subprocess.PIPE,stdout=out,stderr=subprocess.STDOUT,text=True,start_new_session=True)
        try:
            while time.monotonic()-start<100:
                content=log.read_text(errors="replace")
                if all(marker in content for marker in ("Done (","WORLDLOGIC_CORE_TESTS_PASSED","WORLDLOGIC_GEOGRAPHY_AGGREGATE","WORLDLOGIC_GENERATED_SPAWN","WORLDLOGIC_SPAWN_DRY","WORLDLOGIC_CACHE_EXACT_PASSED","WORLDLOGIC_CLIMATE_FIXTURES_PASSED","WORLDLOGIC_CLIMATE_SURVEY_PASSED","WORLDLOGIC_CLIMATE_PHASE","WORLDLOGIC_API_CHECKS_PASSED")):
                    duration=re.search(r"Done \(([0-9.]+)s\)",content)
                    assert duration, "missing startup duration"
                    print("WORLDLOGIC_FRESH_WORLD",root.name,seed,"startup_seconds",duration.group(1),"elapsed",round(time.monotonic()-start,2),flush=True)
                    process.stdin.write("stop\n")
                    process.stdin.flush()
                    process.wait(timeout=30)
                    assert process.returncode==0, process.returncode
                    return
                if process.poll() is not None:
                    raise RuntimeError(f"server exited before terrain assertions: {root.name}/{seed}")
                time.sleep(1)
            # Capture actual Java stacks before terminating a stalled generation.
            subprocess.run(["jcmd",str(process.pid),"Thread.print"],timeout=10,check=False)
            raise TimeoutError(f"world generation exceeded 100s: {root.name}/{seed}")
        finally:
            if process.poll() is None:
                os.killpg(process.pid,signal.SIGTERM)
                try: process.wait(timeout=10)
                except subprocess.TimeoutExpired: os.killpg(process.pid,signal.SIGKILL)
            print(log.read_text(errors="replace")[-25000:],flush=True)

for loader,seeds in (("52.1.0",(1234,)),("52.1.20",(-987654321,-6487130996193510395))):
    root,args=install(loader)
    for seed in seeds:
        run(root,args,seed)
