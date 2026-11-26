"""Compare real public/protected class signatures in official named server JARs, no source rewriting."""
import argparse
import hashlib
import io
import json
import re
import struct
import urllib.request
import zipfile
from pathlib import Path
from .common import require, write_json

MANIFEST="https://piston-meta.mojang.com/mc/game/version_manifest_v2.json"
DEFAULT_CLASSES=[
 "net.minecraft.resources.Identifier","net.minecraft.resources.ResourceLocation",
 "net.minecraft.world.entity.projectile.Projectile","net.minecraft.world.entity.projectile.arrow.AbstractArrow",
 "net.minecraft.world.entity.projectile.AbstractArrow",
 "net.minecraft.world.entity.projectile.throwableitemprojectile.AbstractThrownPotion",
 "net.minecraft.world.entity.LivingEntity","net.minecraft.world.entity.player.Player",
 "net.minecraft.world.entity.item.ItemEntity","net.minecraft.world.level.block.Block",
 "net.minecraft.world.level.block.state.BlockBehaviour$BlockStateBase",
 "net.minecraft.server.level.ServerLevel","net.minecraft.world.item.ItemStack",
 "net.minecraft.world.level.gamerules.GameRules","net.minecraft.world.level.GameRules"
]
def fetch(url):
    require(url.startswith("https://"),"Official download requires HTTPS")
    with urllib.request.urlopen(urllib.request.Request(url,headers={"User-Agent":"MineMods-QA/2.0"}),timeout=90) as r:return r.read()

def checked(url,sha):
    b=fetch(url)
    require(hashlib.sha1(b).hexdigest()==sha,"Official Mojang SHA-1 mismatch")
    return b

def official(version,cache):
    cache=Path(cache);cache.mkdir(parents=True,exist_ok=True)
    require(re.fullmatch(r"\d+\.\d+(?:\.\d+)?",version),"Invalid version")
    require(version.startswith("26."),"Named-server comparison currently supports 26.x only; older obfuscated versions need mapping normalization")
    manifest=json.loads(fetch(MANIFEST))
    matches=[v for v in manifest["versions"] if v["id"]==version]
    require(len(matches)==1,"Version absent from official manifest: "+version)
    m=matches[0]
    meta=json.loads(checked(m["url"],m["sha1"]))
    d=meta["downloads"]["server"];f=cache/(version+"-server.jar")
    if not f.is_file() or hashlib.sha1(f.read_bytes()).hexdigest()!=d["sha1"]:
        f.write_bytes(checked(d["url"],d["sha1"]))
    return f,d["sha1"]

class Reader:
    def __init__(self,data):self.s=io.BytesIO(data)
    def take(self,n):
        b=self.s.read(n)
        if len(b)!=n:raise ValueError("Truncated classfile")
        return b
    def u1(self):return self.take(1)[0]
    def u2(self):return struct.unpack(">H",self.take(2))[0]
    def u4(self):return struct.unpack(">I",self.take(4))[0]

def class_api(data):
    r=Reader(data)
    require(r.u4()==0xcafebabe,"Not a class file")
    r.u2();major=r.u2();count=r.u2()
    cp=[None]*count;i=1
    while i<count:
        t=r.u1()
        if t==1:cp[i]=r.take(r.u2()).decode("utf-8",errors="replace")
        elif t in (7,8,16,19,20):cp[i]=r.u2()
        elif t in (3,4,9,10,11,12,17,18):r.take(4)
        elif t==15:r.take(3)
        elif t in (5,6):r.take(8);i+=1
        else:raise ValueError("Unknown class constant tag "+str(t))
        i+=1
    access=r.u2();this=r.u2();superclass=r.u2()
    interfaces=[cp[cp[r.u2()]] for _ in range(r.u2())]
    def members():
        out=[]
        for _ in range(r.u2()):
            flags=r.u2();name=cp[r.u2()];desc=cp[r.u2()]
            for _ in range(r.u2()):
                r.u2();r.take(r.u4())
            if flags&5:out.append({"name":name,"descriptor":desc,"access":flags})
        return sorted(out,key=lambda x:(x["name"],x["descriptor"],x["access"]))
    fields=members();methods=members()
    return {"major":major,"access":access,"name":cp[cp[this]],"super":cp[cp[superclass]] if superclass else None,"interfaces":sorted(interfaces),"fields":fields,"methods":methods}

def named_jar(path):
    with zipfile.ZipFile(path) as z:
        if "net/minecraft/server/MinecraftServer.class" in z.namelist():return Path(path).read_bytes()
        nested=[n for n in z.namelist() if n.startswith("META-INF/versions/") and n.endswith(".jar")]
        require(len(nested)==1,"Expected one official server payload")
        raw=z.read(nested[0])
    with zipfile.ZipFile(io.BytesIO(raw)) as z:
        require("net/minecraft/server/MinecraftServer.class" in z.namelist(),"Server payload not named; mappings required")
    return raw

def inventory(raw,classes):
    with zipfile.ZipFile(io.BytesIO(raw)) as z:
        names=set(z.namelist());out={}
        for c in classes:
            n=c.replace(".","/")+".class"
            out[c]=class_api(z.read(n)) if n in names else None
    return out

def compare(a,b):
    result=[]
    for c in sorted(set(a)|set(b)):
        old,new=a.get(c),b.get(c)
        if old is None and new is None:continue
        if old is None or new is None:
            result.append({"class":c,"status":"added" if old is None else "removed"})
            continue
        row={"class":c,"status":"unchanged","changes":{}}
        for k in ("super","interfaces","access"):
            if old[k]!=new[k]:row["changes"][k]={"from":old[k],"to":new[k]}
        for k in ("fields","methods"):
            enc=lambda m:(m["name"],m["descriptor"],m["access"])
            x={enc(m) for m in old[k]};y={enc(m) for m in new[k]}
            if x!=y:row["changes"][k]={"removed":sorted(x-y),"added":sorted(y-x)}
        if row["changes"]:row["status"]="changed"
        result.append(row)
    return result

def main():
    ap=argparse.ArgumentParser();ap.add_argument("source");ap.add_argument("target");ap.add_argument("--cache",default=".qa-api-cache");ap.add_argument("--out",required=True)
    a=ap.parse_args()
    f,fs=official(a.source,a.cache);t,ts=official(a.target,a.cache)
    old=inventory(named_jar(f),DEFAULT_CLASSES);new=inventory(named_jar(t),DEFAULT_CLASSES)
    require(any(old.values()) and any(new.values()),"No named classes found")
    changes=compare(old,new)
    report={"schema":1,"kind":"api-diff","source":a.source,"target":a.target,"source_sha1":fs,"target_sha1":ts,"scope":"selected public/protected server classes; not loader APIs or client mappings","classes":changes}
    write_json(a.out,report)
    print("API diff:",len(changes),"classes,",sum(r["status"]!="unchanged" for r in changes),"changed")
if __name__=="__main__":main()
