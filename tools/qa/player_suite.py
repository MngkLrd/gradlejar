"""Data-driven localhost protocol gameplay, with independent server assertions."""
import json
import re
import time
from .common import ROOT, safe_path, require
from .virtual_player import Bot

SUPPORTED={("fabric","1.20.1"),("fabric","1.21.1"),("fabric","1.21.4")}
ACTIONS={"position","startDig","abortDig","activate","inventory","drop","equip","move","jump","craft","place","dig","attack","interact","use"}

def validate_args(action, a):
    # Existing packet fixtures retain their schema. New actions have bounded inputs.
    def name(k):
        return isinstance(a.get(k),str) and bool(re.fullmatch(r"[a-z0-9_]+",a[k]))
    def coords(x):
        return isinstance(x,dict) and all(type(x.get(k)) in (int,float) and abs(x[k])<=30_000_000 for k in ("x","y","z")) and isinstance(x.get("block"),str)
    if action in ("equip","craft","place","use"):
        require(name("item"),"Action requires item name")
    if action in ("attack","interact"):
        require(name("entity"),"Action requires entity name")
    if action in ("place","dig"):
        require(coords(a),"Action requires coordinates and block name")
    if action in ("activate","place"):
        require(type(a.get("face",1)) is int and 0<=a.get("face",1)<=5,"Invalid face")
    if action=="equip":
        require(a.get("destination","hand") in ("hand","off-hand","head","torso","legs","feet"),"Invalid equip slot")
    if action=="craft":
        require(type(a.get("count",1)) is int and 1<=a.get("count",1)<=16,"Invalid craft count")
        require("table" not in a or coords(a["table"]),"Invalid crafting table")
    if action in ("move","jump","use"):
        require(type(a.get("ms",800)) is int and 100<=a.get("ms",800)<=4000,"Invalid action duration")
    if action in ("move","jump"):
        require(a.get("direction","forward") in ("forward","back","left","right"),"Invalid movement direction")
        require(type(a.get("yaw",0)) in (int,float) and abs(a.get("yaw",0))<=360,"Invalid yaw")

def validate(suite):
    require(isinstance(suite,dict) and suite.get("schema")==1,"Invalid player suite")
    steps=suite.get("steps")
    require(isinstance(steps,list) and 1<=len(steps)<=100,"Player suite needs 1..100 steps")
    assertions=0
    for s in steps:
        require(isinstance(s,dict),"Invalid player step")
        if "action" in s:
            require(s["action"] in ACTIONS and isinstance(s.get("args",{}),dict),"Unsupported player action")
            validate_args(s["action"],s.get("args",{}))
        elif "command" in s:
            cmd=s["command"]
            require(isinstance(cmd,str) and len(cmd)<=1000 and "\n" not in cmd and cmd.split() and cmd.split()[0].lower() not in ("stop","op","deop","ban","pardon","whitelist"),"Invalid player fixture command")
            require(isinstance(s.get("expect"),str) and s["expect"],"Command needs expectation")
        elif "wait" in s:
            require(type(s["wait"]) in (int,float) and 0<=s["wait"]<=10,"Invalid player wait")
        else:raise ValueError("Unknown player step")
        if s.get("assertion"):
            require("command" in s and isinstance(s.get("name"),str) and s["name"],"Assertions must be named independent server commands")
            assertions+=1
        if "expect" in s:re.compile(s["expect"],re.I)
    require(assertions>=1 and any("action" in s for s in steps),"Player suite requires network actions and independent assertions")
    require(sum(s.get("wait",0) for s in steps)<=40,"Player wait budget exceeded")
    require(sum(s.get("args",{}).get("ms",800) for s in steps if s.get("action") in ("move","jump","use"))<=25_000,"Player action duration budget exceeded")
    return steps

def callback(p,q):
    require((p["loader"],p["minecraft"]) in SUPPORTED,"Protocol backend not verified for this target")
    steps=validate(json.loads(safe_path(ROOT,q["player_scenario"]).read_text()))
    def run(rcon,port,proc,report,work):
        checks=[]
        row={"backend":"mineflayer","version":p["minecraft"],"loader":p["loader"],"status":"failed","checks":checks,"scope":"network actions; no rendering"}
        report["virtual_player"]=row
        bot=Bot(port,p["minecraft"],work)
        try:
            ready=bot.wait(lambda x:x.get("event")=="ready",45)
            require(ready.get("version")==p["minecraft"],"Client protocol mismatch")
            for s in steps:
                if "wait" in s:time.sleep(s["wait"]);continue
                if "action" in s:
                    try:
                        result=bot.action(s["action"],**s.get("args",{}))
                    except Exception as exc:
                        checks.append({"name":s["action"],"error":str(exc),"passed":False,"assertion":False})
                        raise
                    checks.append({"name":s["action"],"client_response":result,"passed":True,"assertion":False})
                else:
                    result=rcon.cmd(s["command"]);passed=bool(re.search(s["expect"],result,re.I))
                    checks.append({"name":s.get("name",s["command"]),"response":result,"passed":passed,"assertion":bool(s.get("assertion"))})
                    print("PLAYER",s.get("name",s["command"]),"=>",result,flush=True)
                    require(passed,"Player scenario assertion failed: "+s.get("name",s["command"])+" => "+result)
            require(any(c["passed"] and c["assertion"] for c in checks),"No player assertions")
            row["status"]="passed"
        finally:bot.close()
    return run
