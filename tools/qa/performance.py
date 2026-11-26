"""Bounded idle/load server measurements. Not an attribution of mod overhead."""
import re
import statistics
import time
from pathlib import Path
from .common import require

def parse_tick_query(text):
    def get(pattern):
        match=re.search(pattern,text,re.I)
        return float(match.group(1)) if match else None
    return {"mspt":get(r"(?:average time per tick|average tick time|average mspt)\s*:?\s*([\d.]+)\s*ms"),
            "target_tps":get(r"(?:target tick rate|target ticks per second)\s*:?\s*([\d.]+)")}

def game_tick(rcon):
    s=rcon.cmd("time query gametime")
    m=re.search(r"(?:time is|gametime is)\s+(\d+)",s,re.I)
    require(m,"Unrecognized gametime response: "+s)
    return int(m[1])

def process_rss(pid):
    # Sum RSS for the isolated server's process tree (Linux); shared pages can be counted twice.
    found=set();queue=[pid];total=0
    while queue:
        current=queue.pop()
        if current in found:continue
        found.add(current)
        try:
            path=Path("/proc")/str(current)
            s=(path/"status").read_text()
            m=re.search(r"^VmRSS:\s*(\d+)\s*kB",s,re.M)
            if m:total+=int(m[1])
            children=(path/"task"/str(current)/"children").read_text().split()
            queue.extend(int(x) for x in children)
        except (OSError,ValueError):pass
    return round(total/1024,2) if total else None

def sample(rcon,proc,seconds=5):
    first=game_tick(rcon);t0=time.monotonic()
    time.sleep(seconds)
    last=game_tick(rcon);elapsed=time.monotonic()-t0
    tick=rcon.cmd("tick query")
    result={"elapsed_seconds":round(elapsed,3),"game_ticks":last-first,"effective_tps":round((last-first)/elapsed,3),
            "rss_mib_process_tree":process_rss(proc.pid),"tick_query":tick,**parse_tick_query(tick)}
    require(last>first,"Server game ticks did not advance")
    return result

def summarize(samples):
    def median(key):
        data=[s[key] for s in samples if s.get(key) is not None]
        return round(statistics.median(data),3) if data else None
    return {"samples":samples,**{k:median(k) for k in ("effective_tps","mspt","target_tps","rss_mib_process_tree")}}

def benchmark(rcon,port,proc,report,work):
    result={"scope":"same production server, idle versus 100 stationary item entities; no unmodded baseline; GitHub-hosted runner noise",
            "entity_count":100,"sample_duration_seconds":5,"samples_per_phase":3}
    rcon.cmd("forceload add 0 0")
    rcon.cmd("kill @e[tag=qa_perf]")
    time.sleep(3)
    result["idle"]=summarize([sample(rcon,proc) for _ in range(3)])
    try:
        for i in range(100):
            x=1.5+i%10;z=1.5+i//10
            s=rcon.cmd(f'summon minecraft:item {x} 215 {z} {{Item:{{id:"minecraft:stone",count:1}},Tags:["qa_perf"],NoGravity:1b,PickupDelay:32767s,Age:-32768s}}')
            require("Summoned new" in s,"Workload summon failed: "+s)
        # RCON count is checked by a scoreboard, not inferred from number of commands.
        rcon.cmd("scoreboard objectives add qa_perf dummy")
        rcon.cmd("scoreboard players set #count qa_perf 0")
        rcon.cmd("execute as @e[tag=qa_perf] run scoreboard players add #count qa_perf 1")
        count=rcon.cmd("scoreboard players get #count qa_perf")
        require(re.search(r"(?:has|is)\s+100\b",count,re.I),"Expected exactly 100 workload entities: "+count)
        time.sleep(3)
        result["loaded"]=summarize([sample(rcon,proc) for _ in range(3)])
    finally:
        rcon.cmd("kill @e[tag=qa_perf]")
        rcon.cmd("scoreboard objectives remove qa_perf")
    report["performance"]=result
    print("PERFORMANCE",result,flush=True)
