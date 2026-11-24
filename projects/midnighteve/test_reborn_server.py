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
command("save-all flush")
print("PASS: structure placement, roof, doors, clear entrance, floor and resident merchant data")
try: command("stop")
except (RuntimeError, OSError): pass
s.close()
