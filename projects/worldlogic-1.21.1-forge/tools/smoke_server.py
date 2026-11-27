"""Run the production Forge JAR; if official installer download is blocked, run a clearly labeled dev server instead."""
import pathlib, sys, subprocess, urllib.request, urllib.error, shutil, time, os, signal, zipfile
project=pathlib.Path(sys.argv[1]).resolve()
root=project/"build"/"worldlogic-smoke"
root.mkdir(parents=True,exist_ok=True)
version="1.21.1-52.1.0"
installer=root/"forge-installer.jar"
url=f"https://maven.minecraftforge.net/net/minecraftforge/forge/{version}/forge-{version}-installer.jar"
production=True
try:
    req=urllib.request.Request(url,headers={"User-Agent":"Gradle/8.10.2 (World Logic build validation)"})
    with urllib.request.urlopen(req,timeout=30) as response, installer.open("wb") as out:shutil.copyfileobj(response,out)
    if not zipfile.is_zipfile(installer):raise OSError("Installer response was not a jar")
except (urllib.error.URLError,OSError,TimeoutError) as e:
    print(f"::warning::Official installer unavailable ({e}); running Forge development-server smoke test instead.")
    production=False
if production:
    subprocess.run(["java","-jar",str(installer),"--installServer"],cwd=root,check=True,timeout=300)
    mods=root/"mods";mods.mkdir(exist_ok=True)
    jars=[p for p in (project/"build"/"libs").glob("worldlogic-*.jar") if not any(k in p.name for k in ("sources","dev","javadoc"))]
    assert len(jars)==1,jars
    shutil.copy2(jars[0],mods/jars[0].name)
    args=root/"libraries"/"net"/"minecraftforge"/"forge"/version/"unix_args.txt"
    assert args.is_file(),args
    command=["java","-Xmx2G","-Dworldlogic.ci=true","@"+str(args),"nogui"]
    cwd=root
else:
    root=project/"run-server";root.mkdir(exist_ok=True)
    command=["gradle","--no-daemon","-PworldlogicCi","runServer"]
    cwd=project
(root/"eula.txt").write_text("eula=true\n")
(root/"server.properties").write_text("online-mode=false\nserver-ip=127.0.0.1\nlevel-name=worldlogic-ci\nlevel-seed=1234\nlevel-type=worldlogic:realistic\nview-distance=3\nsimulation-distance=3\nmax-tick-time=-1\n")
log=root/"server-smoke.log"
with log.open("w") as out:
    process=subprocess.Popen(command,cwd=cwd,stdin=subprocess.PIPE,stdout=out,stderr=subprocess.STDOUT,text=True,start_new_session=True)
    try:
        for _ in range(210):
            text=log.read_text(errors="replace")
            if "Done (" in text and "WORLDLOGIC_CORE_TESTS_PASSED" in text and "WORLDLOGIC_TERRAIN_TEST_PASSED" in text:
                print(("Production" if production else "Development")+" Forge server and core regression checks passed.")
                if production:
                    process.stdin.write("stop\n");process.stdin.flush()
                    process.wait(timeout=40)
                    assert process.returncode==0,process.returncode
                break
            if process.poll() is not None:raise RuntimeError("Server exited before startup and tests")
            time.sleep(1)
        else:raise TimeoutError("Server did not reach startup and test marker")
    finally:
        if process.poll() is None:
            os.killpg(process.pid,signal.SIGTERM)
            try:process.wait(timeout=15)
            except subprocess.TimeoutExpired:os.killpg(process.pid,signal.SIGKILL)
        print(log.read_text(errors="replace")[-25000:])
