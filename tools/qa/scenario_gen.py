"""Generate explicit RCON scenarios from user-defined world fixtures and assertions."""
import argparse
import json
import re
from pathlib import Path
from .common import require,write_json
from .server import scenario_steps
BLOCK=re.compile(r"[a-z0-9_.-]+:[a-z0-9_./-]+(?:\[[a-z0-9_=,.-]+\])?")
def coord(value):
    require(isinstance(value,list) and len(value)==3 and all(type(v) is int and abs(v)<=30000000 for v in value),"Invalid coordinates")
    return " ".join(map(str,value))
def block(value):
    require(isinstance(value,str) and BLOCK.fullmatch(value),"Invalid block ID")
    return value
def generate(spec):
    steps=[]
    for row in spec["operations"]:
        op=row["type"]
        if op=="set_block":
            steps.append({"command":f"setblock {coord(row['pos'])} {block(row['block'])}","expect":"Changed the block|Successfully"})
        elif op=="assert_block":
            steps.append({"command":f"execute if block {coord(row['pos'])} {block(row['block'])}","expect":"Test passed","assertion":True,"name":row["name"]})
        elif op=="wait":steps.append({"wait":row["seconds"]})
        elif op=="command":
            require(not row.get("assertion"),"Use explicit assertion operations")
            steps.append({"command":row["command"],"expect":row["expect"]})
        elif op=="assert_command":
            require(row.get("name") and row.get("expect"),"Explicit assertion name and expectation required")
            steps.append({"command":row["command"],"expect":row["expect"],"assertion":True,"name":row["name"]})
        else:raise ValueError("Unsupported operation: "+op)
    require(any(s.get("assertion") for s in steps),"No gameplay assertions")
    require(any(s.get("name") for s in steps if s.get("assertion")),"Name every gameplay assertion")
    result={"schema":1,"name":spec["name"],"steps":steps}
    for l in ("fabric","neoforge","forge"):scenario_steps(result,l)
    return result
if __name__=="__main__":
    p=argparse.ArgumentParser();p.add_argument("spec");p.add_argument("--out",required=True);a=p.parse_args()
    write_json(a.out,generate(json.loads(Path(a.spec).read_text("utf-8"))))
