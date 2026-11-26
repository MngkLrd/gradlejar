"""Data-driven localhost protocol gameplay, with independent server assertions."""
import json
import re
import time
from .common import ROOT, safe_path, require
from .virtual_player import Bot

SUPPORTED={("fabric","1.20.1"),("fabric","1.21.1"),("fabric","1.21.4")}
ACTIONS={"position","startDig","abortDig","activate","inventory","drop"}

def validate(suite):
    require(isinstance(suite,dict) and suite.get("schema")==1,"Invalid player suite")
    steps=suite.get("steps")
    require(isinstance(steps,list) and 1<=len(steps)<=100,"Player suite needs 1..100 steps")
    assertions=0
    for s in steps:
        require(isinstance(s,dict),"Invalid player step")
        if "action" in s:
            require(s["action"] in ACTIONS and isinstance(s.get("args",{}),dict),"Unsupported player action")
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
                    result=bot.action(s["action"],**s.get("args",{}))
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
