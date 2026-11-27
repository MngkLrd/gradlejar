"""Changed tracked texture review lock. Validates evidence, not artistic truth."""
import argparse
import hashlib
import json
import os
import re
import subprocess
from pathlib import Path, PurePosixPath

ROOT=Path(__file__).resolve().parents[2]
# Existing textures at adoption are not retroactively enrolled. Always compare
# against this fixed tree, including manual rebuilds and later code-only pushes.
BASELINE="cb29b55f45f87782094b77e6fd0670d7fc601ddc"
TEXTURE=re.compile(r"(?:^|/)assets/[a-z0-9_.-]+/textures/.+\.png$")
SHA=re.compile(r"[a-f0-9]{64}")
REF=re.compile(r"minecraft:[a-z0-9_./-]+")
VERSION=re.compile(r"[a-zA-Z0-9_.+-]+")
COMMON=["silhouette","cluster_structure","micro_contrast","light_and_volume","material_continuity","edge_variation","variant_identity","vanilla_comparison"]
PROFILES={"bark":["grain_direction","furrows_and_ridges"],"log_end":["growth_rings","ring_microstructure","bark_boundary"],"conifer":["needle_direction","structural_transparency"],"broadleaf":["leaf_groups","canopy_depth","structural_transparency"],"soil":["aggregate_structure","tile_rhythm"],"ore":["host_rock","mineral_inclusion","inclusion_light"],"clothing":["folds_and_seams","material_specific_shading"],"sapling":["branch_hierarchy","species_silhouette","foliage_attachment"],"vessel":["construction","surface_turn","material_specific_shading"],"other":["functional_material_cues"]}

def git(root,*args):
    return subprocess.check_output(["git",*args],cwd=root)

def changed(root,baseline=BASELINE):
    try:
        git(root,"cat-file","-e",baseline+"^{commit}")
    except subprocess.CalledProcessError:
        subprocess.run(["git","fetch","--no-tags","--depth=1","origin",baseline],cwd=root,check=True)
    raw=git(root,"diff","--name-only","--no-renames","--diff-filter=AM","-z",baseline,"HEAD")
    return sorted(p for p in raw.decode("utf-8").split("\0") if TEXTURE.search(p))

def observation(value):
    # Prevent blank/placeholder attestations; this cannot establish that an agent saw pixels.
    return isinstance(value,str) and len(value.strip())>=16 and len(set(value.lower().split()))>=3

def validate(record,digest):
    if not isinstance(record,dict):return ["Missing review record"]
    errors=[]
    if record.get("sha256")!=digest:errors.append("Review SHA-256 does not match actual PNG")
    if record.get("verdict")!="PASS":errors.append("Aesthetic verdict must be PASS after real review")
    profile=record.get("material_profile")
    if not isinstance(profile,str) or profile not in PROFILES:
        errors.append("Unknown/missing material_profile");profile="other"
    refs=record.get("references")
    if not isinstance(refs,list) or not refs:
        errors.append("Missing actual vanilla reference IDs, versions and hashes")
    else:
        for ref in refs:
            if not isinstance(ref,dict) or not all(isinstance(ref.get(k),str) and pat.fullmatch(ref[k]) for k,pat in (("id",REF),("minecraft",VERSION),("sha256",SHA))):
                errors.append("Invalid reference ID/version/SHA-256")
            elif any(x in ("",".","..") for x in ref["id"].split(":",1)[1].split("/")):
                errors.append("Invalid reference path")
    findings=record.get("findings")
    if not isinstance(findings,dict):
        findings={}
    for criterion in COMMON+PROFILES[profile]:
        if not observation(findings.get(criterion)):
            errors.append("Missing specific visual observation: "+criterion)
    if not observation(record.get("comparison")):
        errors.append("Missing concrete comparison of candidate against actual reference")
    return errors

def belongs(path,project,shared):
    if any(path.startswith(root.rstrip("/")+"/") for root in shared):return True
    if project==".":return path.startswith("src/")
    return path.startswith(project.rstrip("/")+"/")

def project_errors(root,project,paths):
    profile=json.loads((root/project/"build-profile.json").read_text(encoding="utf-8"))
    relevant=[p for p in paths if belongs(p,project,profile.get("shared_sources",[]))]
    if not relevant:return []
    manifest=root/project/"art-review.json"
    try:
        doc=json.loads(manifest.read_text(encoding="utf-8"))
        if doc.get("schema")!=1 or not isinstance(doc.get("assets"),dict):raise ValueError("Invalid manifest schema")
        rows=doc["assets"]
    except (OSError,ValueError,AttributeError) as e:
        return [f"{project}/art-review.json: {e}; {len(relevant)} changed PNG(s) require review"]
    errors=[]
    for path in relevant:
        actual=root/path
        if not actual.is_file():
            errors.append(path+": changed texture unavailable");continue
        digest=hashlib.sha256(actual.read_bytes()).hexdigest()
        errors.extend(path+": "+e for e in validate(rows.get(path),digest))
    return errors

def main():
    parser=argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--projects",required=True,help="JSON selected project directories")
    args=parser.parse_args()
    projects=json.loads(args.projects)
    if not isinstance(projects,list) or any(not isinstance(p,str) or (p!="." and not re.fullmatch(r"projects/[A-Za-z0-9_.-]+",p)) for p in projects):
        raise ValueError("Invalid project selection")
    paths=changed(ROOT) if projects else []
    blocked=[]
    for project in projects:
        errors=project_errors(ROOT,project,paths)
        if errors:
            blocked.append(project)
            print(f"ART REVIEW LOCK: {project} BLOCKED ({len(errors)} issues)")
            for error in errors:print("  "+error)
        else:print(f"ART REVIEW LOCK: {project} evidence check clear (not automated artistic approval)")
    with open(os.environ["GITHUB_OUTPUT"],"a",encoding="utf-8") as f:
        f.write("blocked="+json.dumps(blocked,separators=(",",":"))+"\n")

if __name__=="__main__":main()
