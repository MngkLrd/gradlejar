"""Discover actual final-JAR recipes and emit bounded, executable player tests."""
import argparse
import hashlib
import json
import re
import zipfile
from collections import Counter
from pathlib import Path
from .common import write_json
from .player_suite import SUPPORTED, validate

RESOURCE = re.compile(r"^[a-z0-9_.-]+:[a-z0-9_./-]+$")
FILE = re.compile(r"^data/([a-z0-9_.-]+)/(?:recipes|recipe)/([a-z0-9_./-]+)\.json$")
SHAPED = {"minecraft:crafting_shaped", "crafting_shaped"}
SHAPELESS = {"minecraft:crafting_shapeless", "crafting_shapeless"}

def rid(value):
    if not isinstance(value, str): return None
    if ":" not in value: value = "minecraft:" + value
    return value if RESOURCE.fullmatch(value) and ".." not in value.split("/") else None

def ingredient(value):
    if isinstance(value, dict) and set(value) == {"item"}: value = value["item"]
    if isinstance(value, list) and len(value) == 1: return ingredient(value[0])
    if not isinstance(value, str) or value.startswith("#"): return None
    return rid(value)

def normalize(doc, ident):
    row = {"id":ident, "type":doc.get("type"), "status":"ADAPTER_REQUIRED"}
    raw = doc.get("result")
    result = rid(raw.get("id",raw.get("item"))) if isinstance(raw,dict) else rid(raw)
    count = raw.get("count",1) if isinstance(raw,dict) else 1
    row.update(result=result,count=count if type(count) is int else None)
    if any(k=="conditions" or k.endswith(":conditions") or k.endswith(":load_conditions") for k in doc):
        row["reason"]="Conditional recipe; runtime activation must be resolved."
        return row
    if not isinstance(raw,(str,dict)) or not result or type(count) is not int or not 1<=count<=64:
        row["reason"]="Unknown result schema or stack count.";return row
    if isinstance(raw,dict) and set(raw)-{"id","item","count"}:
        row["reason"]="Result has components, NBT or custom data.";return row
    typ=doc.get("type")
    cells=[]
    if typ in SHAPED:
        pattern,key=doc.get("pattern"),doc.get("key")
        if (not isinstance(pattern,list) or not 1<=len(pattern)<=3 or
            not all(isinstance(s,str) and 1<=len(s)<=3 for s in pattern) or
            len({len(s) for s in pattern})!=1 or not isinstance(key,dict)):
            row["reason"]="Unsupported shaped layout.";return row
        if set("".join(pattern))-{" "}!=set(key):
            row["reason"]="Invalid or unused pattern keys.";return row
        # Trim empty outer rows/columns, preserving internal holes and orientation.
        positions=[(x,y) for y,s in enumerate(pattern) for x,c in enumerate(s) if c!=" "]
        if not positions:
            row["reason"]="Empty pattern.";return row
        lo_x,hi_x=min(x for x,y in positions),max(x for x,y in positions)
        lo_y,hi_y=min(y for x,y in positions),max(y for x,y in positions)
        width,height=hi_x-lo_x+1,hi_y-lo_y+1
        for y in range(lo_y,hi_y+1):
            cells.append([None if pattern[y][x]==" " else ingredient(key[pattern[y][x]]) for x in range(lo_x,hi_x+1)])
        if any(c is None for (x,y) in positions for c in [cells[y-lo_y][x-lo_x]]):
            row["reason"]="Tag, alternative or custom ingredient needs resolution.";return row
        size=2 if width<=2 and height<=2 else 3
        cells=[r+[None]*(size-len(r)) for r in cells]+[[None]*size for _ in range(size-height)]
    elif typ in SHAPELESS:
        raw_ingredients=doc.get("ingredients")
        if not isinstance(raw_ingredients,list) or not 1<=len(raw_ingredients)<=9:
            row["reason"]="Unsupported shapeless ingredients.";return row
        values=[ingredient(v) for v in raw_ingredients]
        if any(v is None for v in values):
            row["reason"]="Tag, alternative or custom ingredient needs resolution.";return row
        size=2 if len(values)<=4 else 3
        values += [None]*(size*size-len(values))
        cells=[values[i:i+size] for i in range(0,len(values),size)]
    else:
        row["reason"]="Non-crafting-grid recipe (cooking, smithing, custom serializer, etc.).";return row
    inputs=Counter(c for r in cells for c in r if c is not None)
    row.update(grid=cells,ingredients=dict(sorted(inputs.items())),grid_size=size)
    if result in inputs:
        row["reason"]="Input and output share an ID; explicit net-count/remainder test required.";return row
    if not all(x.startswith("minecraft:") and re.fullmatch(r"[a-z0-9_]+",x.split(":")[1]) for x in [result,*inputs]):
        row.update(status="MODDED_CLIENT_REQUIRED",reason="Protocol bot cannot safely resolve modded registry IDs.")
        return row
    row.update(status="GRID_CANDIDATE",reason="Explicit vanilla-ID grid; actual result must be verified in game.")
    return row

def discover(jar, minecraft, loader):
    rows=[];seen=set()
    with zipfile.ZipFile(jar) as z:
        for info in sorted(z.infolist(),key=lambda i:i.filename):
            match=FILE.fullmatch(info.filename)
            if not match: continue
            ident=match[1]+":"+match[2]
            if ident in seen:
                rows.append({"id":ident,"file":info.filename,"status":"INVALID","reason":"Duplicate recipe ID."});continue
            seen.add(ident)
            try:
                if info.file_size>1_000_000: raise ValueError("Recipe JSON exceeds budget")
                doc=json.loads(z.read(info))
                if not isinstance(doc,dict): raise ValueError("Recipe must be a JSON object")
                row=normalize(doc,ident)
            except (ValueError,TypeError,KeyError,UnicodeError) as e:
                row={"id":ident,"status":"INVALID","reason":str(e)}
            if row["status"]=="GRID_CANDIDATE" and (loader,minecraft) not in SUPPORTED:
                row.update(status="BACKEND_UNSUPPORTED",reason="No verified protocol bot for target; use actual modded client or GameTest.")
            rows.append(dict(row,file=info.filename))
    with Path(jar).open("rb") as f: digest=hashlib.file_digest(f,"sha256").hexdigest()
    return {"schema":1,"jar":Path(jar).name,"sha256":digest,"minecraft":minecraft,"loader":loader,
            "scope":"Actual packaged recipe JSON; not runtime registration or successful crafting.",
            "counts":dict(sorted(Counter(r["status"] for r in rows).items())),"recipes":rows}

def generate(row):
    if row["status"]!="GRID_CANDIDATE": raise ValueError("Recipe has no supported automatic grid test")
    rid_=row["id"]; output=row["result"]; count=row["count"]
    def cmd(c,e,name=None):
        d={"command":c,"expect":e}
        if name: d.update(name=rid_+" / "+name,assertion=True)
        return d
    def absent(item,name):
        return cmd('execute unless data entity MineModsQA Inventory[{id:"'+item+'"}]',"Test passed",name)
    steps=[
        cmd("execute if entity @a[name=MineModsQA,gamemode=survival]","Test passed","Survival player"),
        cmd("fill 0 199 0 7 199 7 minecraft:stone","Successfully filled|Filled|No blocks"),
        cmd("tp MineModsQA 3.5 200 1.5","Teleported"),
        {"action":"position","args":{"x":3.5,"y":200,"z":1.5}},
        cmd("clear MineModsQA","Removed|No items|^$"),
        absent(output,"Output absent before crafting"),
    ]
    args={"item":output.split(":")[1],"count":count,"grid":[[v.split(":")[1] if v else None for v in line] for line in row["grid"]]}
    if row["grid_size"]==3:
        steps.append(cmd("setblock 3 200 3 minecraft:crafting_table","Changed the block|No blocks"))
        args["table"]={"x":3,"y":200,"z":3,"block":"crafting_table"}
    for item,n in row["ingredients"].items():
        steps.append(cmd(f"give MineModsQA {item} {n}","Gave"))
        steps.append({"action":"inventory","args":{"item":item.split(":")[1],"count":n}})
        steps.append(cmd(f"clear MineModsQA {item} 0",rf"Found {n} matching item","Input count "+item))
    steps += [{"action":"craftGrid","args":args},{"wait":0.5},
              cmd(f"clear MineModsQA {output} 0",rf"Found {count} matching item","Crafted output count")]
    for item in row["ingredients"]: steps.append(absent(item,"Input consumed "+item))
    suite={"schema":1,"name":"Generated crafting test "+rid_,"steps":steps}
    validate(suite)
    return suite

def write_candidates(report,out):
    out.mkdir(parents=True,exist_ok=True)
    for row in report["recipes"]:
        if row["status"]!="GRID_CANDIDATE":continue
        filename=hashlib.sha256(row["id"].encode()).hexdigest()[:20]+".json"
        write_json(out/filename,generate(row))
        row["scenario"]=filename
        row["execution"]="NOT_RUN"
    write_json(out/"recipe-report.json",report)

def main():
    p=argparse.ArgumentParser()
    p.add_argument("jar");p.add_argument("--minecraft",required=True);p.add_argument("--loader",required=True)
    p.add_argument("--out",required=True,type=Path);a=p.parse_args()
    report=discover(a.jar,a.minecraft,a.loader)
    write_candidates(report,a.out)
    print("Recipe discovery:",report["counts"],"| generated candidates are NOT_RUN until executed.")
    return int(any(r["status"]=="INVALID" for r in report["recipes"]))

if __name__=="__main__": raise SystemExit(main())
