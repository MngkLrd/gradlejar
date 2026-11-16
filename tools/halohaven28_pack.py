#!/usr/bin/env python3
"""Pack Halo Haven 2.5 standalone from GitHub Gradle builds, preserving 2.4 registries."""
from pathlib import Path
from zipfile import ZipFile,ZIP_DEFLATED
import re,hashlib,json
core=Path("projects/halohaven/build/libs/halohaven-1.21.1-1.8.0.jar")
addons=list(Path("projects/halohaven-alpha21/build/libs").glob("halohavenalpha2-*.jar"))
assert core.exists() and len(addons)==1
out=Path("dist/HaloHaven-2.8.0-Alpha-NeoForge-1.21.1-Standalone.jar")
out.parent.mkdir(parents=True,exist_ok=True)
entries={}
banned={"META-INF/MANIFEST.MF","META-INF/neoforge.mods.toml","pack.mcmeta"}
def allowed(n):return not(n.endswith("/") or n in banned or n.startswith("META-INF/") and n.upper().endswith((".SF",".RSA",".DSA")))
with ZipFile(core) as z:
    entries.update({n:z.read(n) for n in z.namelist() if allowed(n)})
    mods1=z.read("META-INF/neoforge.mods.toml").decode()
    pack=z.read("pack.mcmeta")
with ZipFile(addons[0]) as z:
    entries.update({n:z.read(n) for n in z.namelist() if allowed(n)})
    mods2=z.read("META-INF/neoforge.mods.toml").decode()
assert 'modId="halohaven"' in mods1
assert 'modId="halohavenalpha2"' in mods2
mods1=re.sub(r'(modId="halohaven"\s*\nversion=")[^"]+(")',r'\g<1>2.8.0-alpha\2',mods1,count=1)
mods2=mods2[mods2.index("[[mods]]"):].replace('version="2.1.0-alpha"','version="2.8.0-alpha"',1)
mods2=mods2.replace("Halo Haven Alpha 2.1 Ecosystem Expansion","Halo Haven 2.8 Regional Kingdoms")
entries["META-INF/neoforge.mods.toml"]=(mods1.rstrip()+"\n\n"+mods2.strip()+"\n").encode()
entries["pack.mcmeta"]=pack
# Gradle source-generator artwork is overridden by stable custom art approved in 2.4.
for ns in ("halohaven","halohavenalpha2"):
    root=Path("projects/halohaven/art23_overlay/assets" if ns=="halohaven" else "projects/halohaven-alpha21/src/main/resources/assets")/ns
    if root.exists():
        for p in root.rglob("*"):
            if p.is_file():
                entries[f"assets/{ns}/{p.relative_to(root).as_posix()}"]=p.read_bytes()
field="data/halohaven/worldgen/noise_settings/heaven_islands.json"
assert field in entries
j=json.loads(entries[field])
for key in ("final_density","initial_density_without_jaggedness"):
    assert j["noise_router"][key]["type"]=="minecraft:min"
assert json.loads(entries["data/halohaven/worldgen/density_function/climate_temperature.json"])["xz_scale"]==0.53
assert json.loads(entries["data/halohaven/worldgen/density_function/climate_vegetation.json"])["xz_scale"]==0.49
assert len([n for n in entries if n.endswith(".nbt") and "/structure/" in n])>=63
assert 'dev/minemods/halohaven/HavenMistFeature.class' in entries
assert 'dev/minemods/halohaven/HavenGroundedTemplateFeature.class' in entries
assert 'data/halohaven/worldgen/configured_feature/sky_clouds.json' in entries
assert 'assets/halohaven/textures/block/sky_mist.png' in entries
assert json.loads(entries['data/halohaven/worldgen/noise_settings/heaven_islands.json'])['noise']['height']==256
assert 'data/halohaven/worldgen/placed_feature/sky_clouds_chalk.json' in entries
assert json.loads(entries['data/halohaven/worldgen/density_function/climate_temperature.json'])['xz_scale']==0.53
assert not any(n.startswith('data/halohavenalpha2/worldgen/structure_set/') for n in entries)
assert len([n for n in entries if n.startswith('data/halohaven/worldgen/placed_feature/')])>=10
assert entries["META-INF/neoforge.mods.toml"].count(b'version="2.8.0-alpha"')==2
with ZipFile(out,"w",ZIP_DEFLATED,compresslevel=8) as z:
    for n in sorted(entries):z.writestr(n,entries[n])
with ZipFile(out) as z:assert z.testzip() is None
sha=hashlib.sha256(out.read_bytes()).hexdigest()
(out.parent/(out.name+".sha256")).write_text(sha+"  "+out.name+"\n")
print("PASS STANDALONE JAR",out,out.stat().st_size,"bytes, SHA256",sha)
