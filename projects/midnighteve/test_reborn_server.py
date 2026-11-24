#!/usr/bin/env python3
"""Check the game, not merely the presence of NBT in a ZIP."""
import socket,struct,time,sys
def readn(s,n):
    out=b""
    while len(out)<n:
        b=s.recv(n-len(out))
        if not b:raise RuntimeError("RCON closed")
        out+=b
    return out
def packet(s,typ,body,id=1):
    b=struct.pack("<ii",id,typ)+body.encode()+b"\0\0"
    s.sendall(struct.pack("<i",len(b))+b)
    size=struct.unpack("<i",readn(s,4))[0]
    out=readn(s,size)
    return struct.unpack("<ii",out[:8]),out[8:-2].decode(errors="replace")
s=None
for _ in range(25):
    try:
        s=socket.create_connection(("127.0.0.1",25575),timeout=6);break
    except OSError:time.sleep(1)
if s is None:raise RuntimeError("No test server RCON")
s.settimeout(90)
head,response=packet(s,3,"midnight-ci-local-050")
if head[0]<0:raise RuntimeError("RCON auth failed")
def command(cmd):
    head,out=packet(s,2,cmd)
    print(cmd, "=>",out,flush=True)
    return out
# Regression: actual locate from spawn in a native world, before placing templates.
# Fail fast rather than waiting minutes for a broken synchronous search.
s.settimeout(25)
for structure in ("everlight_hamlet","hollycroft_farm"):
    begin=time.monotonic()
    out=command("execute positioned 0 100 0 run locate structure midnighteve:"+structure)
    elapsed=time.monotonic()-begin
    if not ("nearest" in out.lower() and "blocks away" in out.lower()):raise RuntimeError("Native locate failed: "+structure+" => "+out)
    if elapsed>20:raise RuntimeError("Native locate exceeded 20 seconds: "+structure)
    print("PASS: natural locate",structure,round(elapsed,2),"seconds",out,flush=True)
s.settimeout(90)
command("forceload add 0 0 47 47")
time.sleep(12)
out=command("place template midnighteve:frostmarket 0 180 0")
if not any(word in out.lower() for word in ("placed", "loaded template")):raise RuntimeError("Template placement failed: "+out)
time.sleep(4)
for cmd in [
 "execute if block 8 184 4 minecraft:spruce_door[half=lower]",
 "execute if block 8 185 4 minecraft:spruce_door[half=upper]",
 "execute if block 8 184 5 minecraft:air",
 "execute if block 8 195 8 minecraft:dark_oak_slab",
 "execute if block 8 183 5 minecraft:spruce_planks",
 "execute if entity @e[type=minecraft:villager,tag=midnighteve_resident]"
]:
    out=command(cmd)
    if "test passed" not in out.lower():raise RuntimeError("Structure assertion failed: "+cmd)
out=command("data get entity @e[type=minecraft:villager,tag=midnighteve_resident,limit=1] Offers.Recipes")
if "midnighteve:wintermark" not in out:raise RuntimeError("Resident trade data missing")
# Native farm template, two distinct entity types and persisted egg timer.
out=command("place template midnighteve:hollycroft_farm 0 220 0")
if not any(w in out.lower() for w in ("placed","loaded template")):raise RuntimeError("Farm placement failed: "+out)
time.sleep(2)
for cmd in [
 "execute if block 35 224 9 minecraft:spruce_door[half=lower]",
 "execute if block 36 225 9 minecraft:spruce_door[half=upper]",
 "execute if block 35 224 10 minecraft:air",
 "execute if block 36 239 18 minecraft:dark_oak_slab",
 "execute if block 33 224 19 minecraft:spruce_fence_gate",
 "execute if entity @e[type=midnighteve:snowstag,x=31,y=224,z=20,distance=..12]",
 "execute if entity @e[type=midnighteve:snowgrouse,x=41,y=224,z=20,distance=..12]"
]:
    out=command(cmd)
    if "test passed" not in out.lower():raise RuntimeError("Farm assertion failed: "+cmd+" => "+out)
command('summon midnighteve:snowgrouse 25 225 25 {NoAI:1b,EggTime:3000,Tags:["midnight_grouse_ci"]}')
out=command("data get entity @e[type=midnighteve:snowgrouse,tag=midnight_grouse_ci,limit=1] EggTime")
if "299" not in out and "3000" not in out and "298" not in out:raise RuntimeError("Grouse egg timer not retained: "+out)
print("PASS: farm roof, doors, gate, both resident species and saved egg timer")
command("save-all flush")
print("PASS: structure placement, roof, doors, clear entrance, floor and resident merchant data")
try: command("stop")
except (RuntimeError, OSError): pass
s.close()
