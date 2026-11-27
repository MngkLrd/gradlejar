"""Product-specific natural-generation regression against the exact Build Hub JAR.

No block placement commands: inspect saved Anvil chunks from a real fresh
WorldLogic Earthlike Climate server. Reuse shared installer and RCON utilities.
This extra test complements, never replaces, the standard release gate.
"""
import gzip
import io
import json
import re
import secrets
import shutil
import signal
import subprocess
import sys
import time
import zlib
from collections import Counter
from pathlib import Path

import nbtlib
from tools.qa.server import install, download, free_port, Rcon, FATAL
from tools.qa.common import require, sha256

PROJECT = Path("projects/worldlogic-terrain-1.21.1-neoforge")
PRESET = "worldlogic:earthlike_climate"
CORE_RC26_JAR = "worldlogic-neoforge-mc1.21.1-1.0.0-rc26.jar"
SPECIES = {
    "plains": {"meadow_grass", "dense_meadow_grass", "wild_chamomile", "wild_cornflower", "wild_clover"},
    "desert": {"dry_grass", "tall_dry_grass", "desert_shrub"},
    "snowy_plains": {"frosted_grass"},
    "forest": {"forest_shrub", "low_fern", "forest_litter", "fallen_leaves"},
    "jungle": {"forest_shrub", "dense_underbrush", "low_fern", "sedge", "forest_litter", "fallen_leaves"},
    "mangrove_swamp": {"forest_shrub", "dense_underbrush", "low_fern", "sedge", "reed", "cattail", "forest_litter", "fallen_leaves"},
}

NEW_FORAGE = {
    "plains": {"wild_strawberry", "wild_radish", "wild_onion", "wild_cabbage", "wild_tomato", "wild_cucumber", "wild_bell_pepper"},
    "forest": {"wild_blueberry", "wild_raspberry", "wild_blackberry", "wild_grape"},
    "jungle": {"wild_grape", "wild_pineapple", "wild_tomato", "wild_cucumber", "wild_bell_pepper"},
    "mangrove_swamp": {"wild_pineapple"},
}

CORE_SPECIES = {
    "prairie": {"meadow_grass", "dense_meadow_grass", "wild_chamomile", "wild_cornflower", "wild_clover"},
    "hot_desert": {"dry_grass", "tall_dry_grass", "desert_shrub"},
    "shrub_tundra": {"frosted_grass", "dwarf_shrub", "wild_heather"},
    "mixed_forest": {"forest_shrub", "low_fern", "forest_litter", "fallen_leaves"},
    "continental_taiga": {"low_fern", "forest_shrub", "forest_litter", "bracken"},
    "temperate_marsh": {"reed", "cattail", "sedge", "marsh_marigold"},
    "equatorial_rainforest": {"forest_shrub", "dense_underbrush", "low_fern", "forest_litter"},
}
CORE_FORAGE = {
    "prairie": {"wild_strawberry", "wild_radish", "wild_onion", "wild_cabbage"},
    "mixed_forest": {"wild_blueberry", "wild_raspberry", "wild_blackberry"},
    "continental_taiga": {"wild_blueberry"},
    "equatorial_rainforest": {"wild_pineapple"},
}
TREE_EXPECTED = {
    "forest": {"oak","birch","dark_oak"},
    "jungle": {"jungle"},
    "mangrove_swamp": {"mangrove"},
    "mixed_forest": {"oak","birch","dark_oak","willow"},
    "continental_taiga": {"larch","spruce"},
    "temperate_marsh": {"willow"},
    "equatorial_rainforest": {"jungle"},
}
MEADOW = SPECIES["plains"]

def unpack(data, index, bits):
    per = 64 // bits
    return (int(data[index // per]) >> ((index % per) * bits)) & ((1 << bits) - 1)

def surface_biomes(chunk):
    sections = {int(sec["Y"]): sec for sec in chunk.get("sections", [])}
    if not sections:
        return Counter()
    base_section = int(chunk.get("yPos", min(sections)))
    heightmaps = chunk.get("Heightmaps", {})
    data = heightmaps.get("MOTION_BLOCKING_NO_LEAVES", heightmaps.get("WORLD_SURFACE"))
    if data is None:
        return Counter()
    candidates = [b for b in range(1, 17) if (256 + 64//b - 1)//(64//b) == len(data)]
    if not candidates:
        return Counter()
    expected = ((max(sections) - base_section + 1) * 16).bit_length()
    bits = min(candidates, key=lambda b: abs(b-expected))
    counts = Counter()
    for i in range(256):
        y = base_section * 16 + unpack(data, i, bits)
        sec = sections.get(y//16)
        if sec is None:
            continue
        biomes = sec.get("biomes", {})
        palette = biomes.get("palette", [])
        if not palette:
            continue
        index = 0 if len(palette)==1 else unpack(biomes["data"], ((y%16)//4)*16+((i//16)//4)*4+(i%16)//4, max(1,(len(palette)-1).bit_length()))
        counts[str(palette[index])] += 1
    return counts

def read_chunk(region, cx, cz):
    file = region / f"r.{cx // 32}.{cz // 32}.mca"
    if not file.exists():
        return None
    with file.open("rb") as f:
        f.seek(4 * ((cx & 31) + 32 * (cz & 31)))
        entry = f.read(4)
        sector = int.from_bytes(entry[:3], "big")
        if not sector:
            return None
        f.seek(4096 * sector)
        length = int.from_bytes(f.read(4), "big")
        require(2 <= length <= 4_000_000, "Invalid region payload")
        codec = f.read(1)[0]
        data = f.read(length - 1)
    if codec == 2:
        data = zlib.decompress(data)
    elif codec == 1:
        data = gzip.decompress(data)
    elif codec != 3:
        raise ValueError(f"Unsupported region codec {codec}")
    return nbtlib.File.parse(io.BytesIO(data))

def scan(region, positions):
    counts, biomes, full = Counter(), set(), 0
    addon_columns, grass_columns, meadow_columns = set(), set(), set()
    surface = Counter()
    for cx, cz in positions:
        chunk = read_chunk(region, cx, cz)
        if chunk is None or str(chunk.get("Status", "")) not in {"minecraft:full", "full"}:
            continue
        full += 1
        surface.update(surface_biomes(chunk))
        for section in chunk.get("sections", []):
            biomes.update(map(str, section.get("biomes", {}).get("palette", [])))
            states = section.get("block_states", {})
            palette = [str(x["Name"]) for x in states.get("palette", [])]
            targets = {i: name.split(":", 1)[1] for i, name in enumerate(palette)
                       if name.startswith("worldlogicterrain:") or name == "minecraft:short_grass" or (name.startswith("minecraft:") and name.endswith(("_log","_leaves")))}
            if not targets:
                continue
            if len(palette) == 1:
                counts[targets[0]] += 4096
                for i in range(256):
                    if palette[0]=="minecraft:short_grass": grass_columns.add((cx,cz,i))
                    elif palette[0].startswith("worldlogicterrain:") and not targets[0].endswith(("_log","_leaves","_planks","_sapling")): addon_columns.add((cx,cz,i))
                    if targets[0] in MEADOW:
                        meadow_columns.add((cx*16 + i%16, cz*16 + i//16))
                continue
            bits = max(4, (len(palette) - 1).bit_length())
            per = 64 // bits
            mask = (1 << bits) - 1
            packed = states["data"]
            for i in range(4096):
                index = (int(packed[i // per]) >> ((i % per) * bits)) & mask
                if index in targets:
                    counts[targets[index]] += 1
                    if palette[index]=="minecraft:short_grass": grass_columns.add((cx,cz,i%256))
                    elif palette[index].startswith("worldlogicterrain:") and not targets[index].endswith(("_log","_leaves","_planks","_sapling")): addon_columns.add((cx,cz,i%256))
                    if targets[index] in MEADOW:
                        meadow_columns.add((cx*16 + i%16, cz*16 + (i%256)//16))
    # Connected 4-neighbour components from actual stored block positions.
    remaining = set(meadow_columns)
    clusters = []
    while remaining:
        stack = [remaining.pop()]
        size = 0
        while stack:
            x, z = stack.pop()
            size += 1
            for point in ((x+1,z),(x-1,z),(x,z+1),(x,z-1)):
                if point in remaining:
                    remaining.remove(point)
                    stack.append(point)
        clusters.append(size)
    # 8x8 areas containing ordinary grass but no Terrain vegetation.
    def tile(point):
        cx, cz, i = point
        return (cx*2+(i%16)//8, cz*2+(i//16)//8)
    gap_tiles = len({tile(p) for p in grass_columns} - {tile(p) for p in addon_columns})
    distribution = {"meadow_clusters_ge4": sum(n >= 4 for n in clusters),
                    "largest_meadow_cluster": max(clusters, default=0),
                    "grass_only_gap_tiles_8x8": gap_tiles}
    return full, biomes, counts, len(addon_columns), len(grass_columns), distribution, surface

def main():
    global PRESET
    rc26 = len(sys.argv)>3
    native_water = len(sys.argv)>4 and sys.argv[4]=="rc30"
    core_name = "worldlogic-neoforge-mc1.21.1-1.0.0-rc30.jar" if native_water else CORE_RC26_JAR
    species = CORE_SPECIES if rc26 else SPECIES
    forage = CORE_FORAGE if rc26 else NEW_FORAGE
    namespace = 'worldlogic' if rc26 else 'minecraft'
    if rc26:
        PRESET = 'worldlogic:realistic_unified'
    inputs = Path(sys.argv[1]).resolve()
    work = Path(sys.argv[2]).resolve()
    work.mkdir(parents=True, exist_ok=False)
    logs = work / "logs"
    logs.mkdir()
    root = work / "server"
    root.mkdir()
    profile = json.loads((PROJECT / "build-profile.json").read_text())
    qa = profile["qa"]
    jars = list(inputs.rglob(profile["jar_glob"]))
    require(len(jars) == 1, "Expected one production JAR")
    jar = jars[0]
    reports = [json.loads(f.read_text()) for f in inputs.rglob("qa-report-*.json")]
    reports = [r for r in reports if r.get("jar") == jar.name and r.get("eligible") is True]
    require(len(reports) == 1, "No unique eligible Build Hub report for candidate")
    digest = sha256(jar)
    require(reports[0].get("sha256") == digest, "Build Hub artifact hash mismatch")
    report = {"preset": PRESET, "seed": 824681, "jar": jar.name, "sha256": digest, "results": [], "status": "failed"}
    outpath = work / "worldgen-report.json"
    proc, conn = None, None
    try:
        cmd = install(profile, qa, root, logs)
        mods = root / "mods"
        mods.mkdir()
        shutil.copy2(jar, mods / jar.name)
        require(sha256(mods / jar.name) == digest, "Installed terrain hash mismatch")
        if rc26:
            core_inputs=Path(sys.argv[3]).resolve()
            cores=list(core_inputs.rglob(core_name))
            require(len(cores)==1,"Expected one exact selected core production JAR")
            core_reports=[json.loads(f.read_text()) for f in core_inputs.rglob("qa-report-*.json")]
            valid=[r for r in core_reports if r.get("jar")==core_name and r.get("eligible") is True]
            require(len(valid)==1 and valid[0].get("sha256")==sha256(cores[0]),"Selected core artifact report/hash mismatch")
            shutil.copy2(cores[0],mods/core_name)
            report["core_sha256"]=sha256(cores[0])
        else:
            for dep in qa["dependencies"]:
                download(dep["url"], mods / dep["name"], dep["sha256"])
        password = secrets.token_hex(18)
        port, rport = free_port(), free_port()
        while port == rport:
            rport = free_port()
        (root / "eula.txt").write_text("eula=true\n")
        (root / "server.properties").write_text("\n".join([
            "online-mode=false", "server-ip=127.0.0.1", f"server-port={port}",
            "enable-rcon=true", f"rcon.port={rport}", f"rcon.password={password}",
            "level-name=natural-world", "level-seed=824681", f"level-type={PRESET}",
            "view-distance=3", "simulation-distance=3", "spawn-protection=0", "max-tick-time=-1", ""
        ]))
        log = logs / "server.log"
        with log.open("w") as stream:
            proc = subprocess.Popen(cmd, cwd=root, stdout=stream, stderr=subprocess.STDOUT, start_new_session=True)
            deadline = time.monotonic() + 420
            while time.monotonic() < deadline:
                content = log.read_text(errors="replace")
                require(proc.poll() is None and not FATAL.search(content), "Custom world startup failure")
                if "Done (" in content:
                    try:
                        conn = Rcon(rport, password)
                        conn.sock.settimeout(120)
                        break
                    except (OSError, EOFError):
                        pass
                time.sleep(2)
            require(conn is not None, "Custom world startup timeout")
            # Verify selected settings from the saved world, not just server.properties.
            conn.cmd("save-all flush")
            level = nbtlib.load(root / "natural-world" / "level.dat")
            generator = level["Data"]["WorldGenSettings"]["dimensions"]["minecraft:overworld"]["generator"]
            require(str(generator["settings"]) == PRESET, "Actual saved generator is not the WorldLogic preset")
            check = conn.cmd("worldlogicterrain worldgen_check")
            require("biome attachment verified" in check, check)
            if rc26:
                sample=conn.cmd("worldlogicterrain habitat_check")
                require("Core habitat sampled:" in sample,sample)
                report["core_habitat_sample"]=sample
                print(sample,flush=True)
            if native_water:
                aquatic=conn.cmd("worldlogicterrain aquatic_check")
                require("Aquatic checks passed; native fluids=8" in aquatic,aquatic)
                report["native_aquatic_checks"]=aquatic
                print(aquatic,flush=True)
            region = root / "natural-world" / "region"
            for biome, expected in species.items():
                target=namespace+":"+biome
                # A locate result may refer only to an underground/vertical biome.
                # Select a window on measured surface biome membership, not on plant counts.
                row=None
                seen=set()
                for anchor in (None,(2048,160,2048),(-2048,160,-2048),(4096,180,-4096)):
                    cmd="locate biome "+target if anchor is None else "execute positioned %d %d %d run locate biome "%anchor+target
                    found=conn.cmd(cmd)
                    coords=re.search(r"\[\s*(-?\d+)\s*,\s*(?:~|-?\d+)\s*,\s*(-?\d+)\s*\]",found)
                    if coords is None:
                        print("Not found",target,anchor,found,flush=True)
                        continue
                    x,z=(int(v) for v in coords.groups())
                    cx,cz=x//16,z//16
                    if (cx,cz) in seen: continue
                    seen.add((cx,cz))
                    positions=[(a,b) for a in range(cx-2,cx+3) for b in range(cz-2,cz+3)]
                    force=f"{(cx-2)*16} {(cz-2)*16} {(cx+2)*16+15} {(cz+2)*16+15}"
                    print("Generating",target,x,z,conn.cmd("forceload add "+force),flush=True)
                    deadline=time.monotonic()+150
                    full,biomes,counts,addon_columns,grass_columns,distribution,surface=0,set(),Counter(),0,0,{},Counter()
                    while time.monotonic()<deadline:
                        time.sleep(5)
                        conn.cmd("save-all flush")
                        full,biomes,counts,addon_columns,grass_columns,distribution,surface=scan(region,positions)
                        if full==len(positions): break
                    conn.cmd("forceload remove "+force)
                    require(full==len(positions),"Full chunk generation timed out "+target)
                    surface_count=surface.get(target,0)
                    print("Surface columns",target,surface_count,flush=True)
                    if surface_count<32: continue
                    matching=sum(counts.get(n,0) for n in expected)
                    forage_count=sum(counts.get(n,0) for n in forage.get(biome,set()))
                    coverage=addon_columns/max(1,full*256)
                    grass_ok=biome not in {"plains","prairie"} or (grass_columns>addon_columns and grass_columns>0)
                    mosaic_ok=biome!="plains" or (matching>=70 and distribution.get("meadow_clusters_ge4",0)>=2 and distribution.get("grass_only_gap_tiles_8x8",0)>=4)
                    trees=TREE_EXPECTED.get(biome,set())
                    logs=sum(counts.get(n+"_log",0) for n in trees)
                    leaves=sum(counts.get(n+"_leaves",0) for n in trees)
                    trees_ok=not trees or (logs>3 and leaves>5)
                    passed=coverage<=.18 and grass_ok and mosaic_ok and target in biomes and matching>0 and (biome not in forage or forage_count>0) and trees_ok
                    # Cold habitats must contain actual frosted grass, not just another cold plant.
                    if biome in {"snowy_plains","shrub_tundra"}: passed=passed and counts.get("frosted_grass",0)>0
                    row={"biome":target,"status":"PASS" if passed else "FAIL","x":x,"z":z,"full_chunks":full,
                         "surface_biome_columns":surface_count,"expected_species_blocks":matching,"new_forage_blocks":forage_count,
                         "tree_logs":logs,"tree_leaves":leaves,"addon_column_fraction":coverage,
                         "vanilla_short_grass_columns":grass_columns,**distribution,"all_terrain_blocks":dict(counts)}
                    break
                if row is None:
                    row={"biome":target,"status":"NOT_FOUND_AT_SURFACE","searched_windows":len(seen)}
                report["results"].append(row)
                print(json.dumps(row),flush=True)
            require(not FATAL.search(log.read_text(errors="replace")), "Fatal custom world error")
            passed=[r for r in report["results"] if r["status"]=="PASS"]
            require(len(passed)>=(3 if rc26 else 4),"Too few verified surface biomes")
            require(any(r["biome"].endswith(":shrub_tundra" if rc26 else ":snowy_plains") for r in passed),"Frost grass regression not verified at a real cold surface")
            if rc26:
                require(any(r.get("tree_logs",0)>0 for r in passed),"No ecological trees verified in RC26")
                require(any(r.get("all_terrain_blocks",{}).get("willow_log",0)+r.get("all_terrain_blocks",{}).get("larch_log",0)>0 for r in passed),"No new tree species verified in RC26")
            if native_water:
                require(any(r.get("all_terrain_blocks",{}).get("water_lily",0)>0 for r in report["results"]),"No generated aquatic lily found in sampled natural chunks")
            require(not any(r["status"] == "FAIL" for r in report["results"]), "A located target biome has no expected generated vegetation")
            try:
                conn.cmd("stop")
            except (OSError, EOFError):
                pass
            conn.close()
            conn = None
            proc.wait(timeout=60)
            require(proc.returncode == 0, "Custom server did not stop cleanly")
            report["status"] = "passed"
    finally:
        if conn is not None:
            try: conn.cmd("stop")
            except Exception: pass
            conn.close()
        if proc is not None and proc.poll() is None:
            proc.terminate()
            try: proc.wait(timeout=20)
            except subprocess.TimeoutExpired:
                proc.kill()
                proc.wait()
        outpath.write_text(json.dumps(report, indent=2) + "\n")
        print(json.dumps(report, indent=2), flush=True)

if __name__ == "__main__":
    main()
