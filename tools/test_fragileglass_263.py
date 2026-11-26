"""Run Minecraft 26.3 itself; check data tags and projectile/fall behavior via console."""
import os, pathlib, subprocess, threading, time, sys
from collections import deque
root=pathlib.Path(sys.argv[1]).resolve()
for folder in ("run","runs/server"):
    p=root/folder
    p.mkdir(parents=True,exist_ok=True)
    (p/"eula.txt").write_text("eula=true\n")
    (p/"server.properties").write_text("online-mode=false\nwhite-list=false\nserver-ip=127.0.0.1\nserver-port=25566\nview-distance=3\nsimulation-distance=3\nspawn-protection=0\nlevel-name=fg-ci-world\nmax-tick-time=-1\n")
log=deque(maxlen=300)
p=subprocess.Popen(["gradle","--no-daemon","runServer"],cwd=root,stdin=subprocess.PIPE,stdout=subprocess.PIPE,stderr=subprocess.STDOUT,text=True,bufsize=1,start_new_session=True)
def reader():
    for line in p.stdout:
        print(line,end="",flush=True)
        log.append(line)
threading.Thread(target=reader,daemon=True).start()
def send(s):
    p.stdin.write(s+"\n");p.stdin.flush()
def wait_marker(s,timeout=20):
    until=time.monotonic()+timeout
    while time.monotonic()<until:
        if any(s in line for line in log): return
        if p.poll() is not None: break
        time.sleep(.2)
    raise RuntimeError("Missing runtime marker: "+s)
try:
    wait_marker("Done (",180)
    send("forceload add 0 0")
    time.sleep(2)
    send("setblock 2 200 2 minecraft:glass")
    send("execute if block 2 200 2 #fragileglass:fragile_blocks run say FG_TAG_OK")
    wait_marker("FG_TAG_OK")
    send("setblock 5 200 5 minecraft:glass")
    send("setblock 8 200 5 minecraft:glass_pane")
    send("setblock 11 200 5 minecraft:glass")
    send("summon minecraft:arrow 5.5 200.7 2.5 {Motion:[0.0d,0.0d,1.8d]}")
    send("summon minecraft:snowball 8.5 200.7 2.5 {Motion:[0.0d,0.0d,1.8d]}")
    send("summon minecraft:pig 11.5 208 5.5")
    time.sleep(6)
    for x,label in [(5,"ARROW"),(8,"SNOWBALL"),(11,"FALL")]:
        send(f"execute if block {x} 200 5 minecraft:air run say FG_{label}_OK")
        wait_marker(f"FG_{label}_OK")
    print("FRAGILE_GLASS_RUNTIME_PASSED",flush=True)
finally:
    try: send("stop")
    except Exception: pass
    try: p.wait(timeout=20)
    except subprocess.TimeoutExpired:
        import signal
        os.killpg(p.pid,signal.SIGTERM)
        p.wait(timeout=10)
