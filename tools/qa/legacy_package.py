"""Select and package historical static-only cohorts without touching their sources."""
import argparse
import os
import zipfile
from collections import defaultdict
from pathlib import Path
from .common import LEGACY, require, SAFE_VERSION

TARGETS=[("forge","1.20.1")]+[(l,v) for l in ("neoforge","fabric")
         for v in ("1.20.1","1.21.1","26.1.2","26.2","26.3")]
FOLDERS={"forge":"Forge","neoforge":"NeoForge","fabric":"Fabric"}
# Historical validators preserved; no new legacy projects are admitted.
RULES={
    "keepsigntext":("KeepSignText",(),()),
    "campfirebatch":("CampfireBatch",(),("/BatchLoader.class",)),
    "naturalbuoyancy":("Natural-Buoyancy",("dev/minemods/naturalbuoyancy/BuoyancyPhysics.class",
                        "dev/minemods/naturalbuoyancy/DensityRules.class"),()),
    "realisticdrops":("Realistic-Drops",("dev/minemods/realisticdrops/DropPhysics.class",),()),
    "fooddigestion":("FoodDigestion",("fooddigestion.mixins.json",
                     "dev/minemods/fooddigestion/mixin/FoodDataMixin.class"),()),
    "fragileglass":("FragileGlass",(),("/GlassRules.class",)),
}

def select(profiles, event):
    if event!="push": return []
    groups=defaultdict(list)
    for p in profiles:
        if p.get("slug") in RULES:
            groups[p["slug"]].append(p)
    result=[]
    for slug,ps in sorted(groups.items()):
        # No partial builds, mixed revisions, duplicates, drafts, or QA-v2 fallback.
        if len(ps)!=len(TARGETS) or {(p.get("loader"),p.get("minecraft")) for p in ps}!=set(TARGETS):
            continue
        if any(p.get("qa") is not None or p.get("draft") or p.get("project_dir") not in LEGACY for p in ps):
            continue
        versions={p.get("version") for p in ps}
        if len(versions)!=1: continue
        version=next(iter(versions))
        if not isinstance(version,str) or not SAFE_VERSION.fullmatch(version): continue
        if any(p.get("jar_glob")!=f'{slug}-{p["loader"]}-mc{p["minecraft"]}-{version}.jar' for p in ps):
            continue
        result.append({"slug":slug,"version":version})
    return result

def package(slug,version,source,out):
    require(slug in RULES and SAFE_VERSION.fullmatch(version),"Unknown legacy cohort")
    label,required,suffixes=RULES[slug]
    expected={f"{slug}-{loader}-mc{mc}-{version}.jar":loader for loader,mc in TARGETS}
    files=list(Path(source).rglob("*.jar"))
    require(len(files)==len(expected) and {p.name for p in files}==set(expected),
            "Missing, duplicate or unexpected legacy JAR")
    by_name={p.name:p for p in files}
    for name in expected:
        with zipfile.ZipFile(by_name[name]) as z:
            require(z.testzip() is None,"Corrupt JAR: "+name)
            entries=set(z.namelist())
            require(all(n in entries for n in required) and all(any(n.endswith(s) for n in entries) for s in suffixes),
                    "Missing legacy validation class/resource: "+name)
    out=Path(out);out.mkdir(parents=True,exist_ok=True)
    name=(label+"-ALL-11-JARs.zip" if slug=="keepsigntext" else f"{label}-{version}-ALL-11-JARs.zip")
    archive=out/name
    with zipfile.ZipFile(archive,"w",zipfile.ZIP_DEFLATED) as z:
        for filename,loader in expected.items():
            dest=filename if slug=="fragileglass" else FOLDERS[loader]+"/"+filename
            if slug=="keepsigntext": dest=label+"/"+dest
            z.write(by_name[filename],dest)
    with zipfile.ZipFile(archive) as z:
        require(z.testzip() is None and len(z.namelist())==11 and all(n.endswith(".jar") for n in z.namelist()),
                "Invalid legacy archive")
    return archive

def main():
    p=argparse.ArgumentParser(description=__doc__)
    p.add_argument("slug");p.add_argument("version");p.add_argument("source");p.add_argument("out")
    a=p.parse_args()
    archive=package(a.slug,a.version,a.source,a.out)
    print("Legacy static-only archive:",archive)
    if output:=os.getenv("GITHUB_OUTPUT"):
        with open(output,"a",encoding="utf-8") as f:
            f.write("archive="+str(archive)+"\nname="+archive.stem+"\n")

if __name__=="__main__": main()
