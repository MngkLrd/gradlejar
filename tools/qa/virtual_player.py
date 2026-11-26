"""Production server plus an actual localhost Minecraft protocol connection."""
import json
import queue
import subprocess
import threading
import time
from pathlib import Path
from .common import ROOT, require

class Bot:
    def __init__(self,port,version,work):
        self.log=(Path(work)/"logs"/"bot.log").open("w",encoding="utf-8")
        self.proc=subprocess.Popen(["node",str(ROOT/"tools/qa/bot/player.cjs"),str(port),version],
            stdin=subprocess.PIPE,stdout=subprocess.PIPE,stderr=subprocess.STDOUT,text=True,bufsize=1)
        self.messages=queue.Queue();self.id=0
        def reader():
            for line in self.proc.stdout:
                self.log.write(line);self.log.flush()
                try:self.messages.put(json.loads(line))
                except ValueError:pass
            self.messages.put({"event":"eof"})
        self.thread=threading.Thread(target=reader,daemon=True);self.thread.start()
    def wait(self,predicate,timeout=20):
        end=time.monotonic()+timeout
        while time.monotonic()<end:
            try:r=self.messages.get(timeout=max(.01,end-time.monotonic()))
            except queue.Empty:break
            if r.get("event") in ("error","end","eof"):
                raise RuntimeError("Virtual player connection failed: "+str(r))
            if predicate(r):return r
        raise TimeoutError("Virtual player response timed out")
    def action(self,action,**kwargs):
        self.id+=1
        self.proc.stdin.write(json.dumps({"id":self.id,"action":action,**kwargs})+"\n");self.proc.stdin.flush()
        result=self.wait(lambda r:r.get("id")==self.id)
        require(result.get("ok") is True,"Virtual player action failed: "+str(result))
        return result
    def close(self):
        if self.proc.poll() is None:
            try:self.action("quit")
            except Exception:pass
            try:self.proc.wait(timeout=5)
            except subprocess.TimeoutExpired:self.proc.kill();self.proc.wait()
        self.thread.join(timeout=3);self.log.close()

def run_player(rcon,port,proc,report,work):
    bot=Bot(port,"1.21.1",work)
    checks=[]
    report["virtual_player"]={"protocol":"1.21.1","loader":"fabric","client":"Mineflayer 4.26.0","scope":"actual network packets; no rendering","checks":checks}
    def check(name,cmd,expect="Test passed",response=None):
        result=rcon.cmd(cmd)
        ok=expect.lower() in result.lower()
        checks.append({"name":name,"command":cmd,"server_response":result,"client_action":response,"passed":ok})
        print("PLAYER",name,"=>",result,flush=True)
        require(ok,"Virtual player assertion failed: "+name+" => "+result)
    def act(name,action,cmd,expect="Test passed",**kwargs):
        response=bot.action(action,**kwargs)
        time.sleep(.65)
        check(name,cmd,expect,response)
    try:
        bot.wait(lambda r:r.get("event")=="ready",45)
        rcon.cmd("forceload add 0 0")
        rcon.cmd("fill 0 199 0 10 199 10 minecraft:stone")
        rcon.cmd("setblock 3 200 3 minecraft:glass")
        rcon.cmd("setblock 4 200 3 minecraft:stone")
        rcon.cmd("setblock 5 200 3 minecraft:lever[face=floor,facing=north,powered=false]")
        rcon.cmd("gamemode survival MineModsQA")
        rcon.cmd("clear MineModsQA")
        rcon.cmd("tp MineModsQA 3.5 200 1.5")
        bot.action("position",x=3.5,y=200,z=1.5)
        check("Survival player connected","execute if entity @a[name=MineModsQA,gamemode=survival]")
        check("Glass present before action","execute if block 3 200 3 minecraft:glass")
        # Check controls first, so a later glass bug does not hide protocol coverage.
        act("Stone does not instamine from one start packet","startDig","execute if block 4 200 3 minecraft:stone",x=4,y=200,z=3,block="stone")
        bot.action("abortDig",x=4,y=200,z=3,block="stone")
        act("Right click toggles lever","activate","execute if block 5 200 3 minecraft:lever[powered=true]",x=5,y=200,z=3,block="lever")
        rcon.cmd("give MineModsQA minecraft:stick 2")
        check("Inventory synchronized","clear MineModsQA minecraft:stick 0","2",bot.action("inventory",item="stick",count=2))
        act("Q packet drops one item","drop","clear MineModsQA minecraft:stick 0","1",item="stick")
        check("Dropped entity exists",'execute if entity @e[type=minecraft:item,nbt={Item:{id:"minecraft:stick"}}]')
        rcon.cmd("clear MineModsQA")
        act("One start-digging packet breaks glass","startDig","execute if block 3 200 3 minecraft:air",x=3,y=200,z=3,block="glass")
        report["virtual_player"]["status"]="passed"
    finally:
        bot.close()
