"""Live server regression for both Minecraft 26.3 ports using RCON."""
import os, pathlib, subprocess, threading, time, sys, socket, struct, signal
from collections import deque
root=pathlib.Path(sys.argv[1]).resolve()
password="fg-ci-local-263"
for folder in ("run","runs/server","run/server"):
    p=root/folder
    p.mkdir(parents=True,exist_ok=True)
    (p/"eula.txt").write_text("eula=true\n")
    (p/"server.properties").write_text("online-mode=false\nwhite-list=false\nserver-ip=127.0.0.1\nserver-port=25566\nview-distance=3\nsimulation-distance=3\nspawn-protection=0\nlevel-name=fg-ci-world\nmax-tick-time=-1\nenable-rcon=true\nrcon.port=25576\nrcon.password="+password+"\n")
log=deque(maxlen=300)
p=subprocess.Popen(["gradle","--no-daemon","runServer"],cwd=root,stdin=subprocess.DEVNULL,stdout=subprocess.PIPE,stderr=subprocess.STDOUT,text=True,bufsize=1,start_new_session=True)
def reader():
    for line in p.stdout:
        print(line,end="",flush=True)
        log.append(line)
threading.Thread(target=reader,daemon=True).start()
def read_exact(s,n):
    data=b""
    while len(data)<n:
        chunk=s.recv(n-len(data))
        if not chunk: raise EOFError("RCON closed")
        data+=chunk
    return data
def packet(s,i,t,body):
    b=struct.pack("<ii",i,t)+body.encode()+b"\0\0"
    s.sendall(struct.pack("<i",len(b))+b)
def receive(s):
    n=struct.unpack("<i",read_exact(s,4))[0]
    if not 10<=n<=4194304: raise RuntimeError("Invalid RCON packet size")
    b=read_exact(s,n)
    i,t=struct.unpack("<ii",b[:8])
    return i,t,b[8:-2].decode(errors="replace")
def command(text):
    packet(conn,2,2,text)
    i,t,result=receive(conn)
    if i!=2: raise RuntimeError("RCON command rejected")
    print("FG COMMAND",text,"=>",result,flush=True)
    return result
conn=None
try:
    deadline=time.monotonic()+180
    while time.monotonic()<deadline:
        if p.poll() is not None: raise RuntimeError("Minecraft exited before startup")
        if any("Done (" in line for line in log):
            try:
                conn=socket.create_connection(("127.0.0.1",25576),timeout=3)
                break
            except OSError: pass
        time.sleep(1)
    if conn is None: raise RuntimeError("Minecraft or RCON did not start")
    conn.settimeout(10)
    packet(conn,1,3,password)
    i,t,_=receive(conn)
    if i==-1: raise RuntimeError("RCON auth failed")
    if t!=2:
        i,t,_=receive(conn)
    if i!=1 or t!=2: raise RuntimeError("RCON auth failed")
    command("forceload add 0 0")
    time.sleep(3)
    command("setblock 2 200 2 minecraft:glass")
    result=command("execute if block 2 200 2 #fragileglass:fragile_blocks")
    if "passed" not in result.lower(): raise RuntimeError("Fragile Glass data tags missing: "+result)
    print("FG_TAG_OK",flush=True)
    command("setblock 5 200 5 minecraft:glass")
    command("setblock 8 200 5 minecraft:glass_pane")
    command("setblock 11 200 5 minecraft:glass")
    command("summon minecraft:arrow 5.5 200.7 2.5 {Motion:[0.0d,0.0d,1.8d]}")
    command("summon minecraft:snowball 8.5 200.7 2.5 {Motion:[0.0d,0.0d,1.8d]}")
    command("summon minecraft:pig 11.5 208 5.5")
    time.sleep(7)
    for x,label in [(5,"ARROW"),(8,"SNOWBALL"),(11,"FALL")]:
        result=command(f"execute if block {x} 200 5 minecraft:air")
        if "passed" not in result.lower(): raise RuntimeError(label+" did not break glass: "+result)
        print("FG_"+label+"_OK",flush=True)
    print("FRAGILE_GLASS_RUNTIME_PASSED",flush=True)
finally:
    if conn is not None:
        try: command("stop")
        except Exception: pass
        conn.close()
    try: p.wait(timeout=20)
    except subprocess.TimeoutExpired:
        os.killpg(p.pid,signal.SIGTERM)
        p.wait(timeout=10)
