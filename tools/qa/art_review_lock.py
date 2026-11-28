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
# Existing original-lock obligations remain. Require R9 evidence for PNGs new/changed since this tree.
ATLAS_BASELINE="8e7d5b44a17360965ec0965874268d4112129d2d"
ATLAS_FILE=ROOT/"tools/qa/research/vanilla-art-rules.json"
ATLAS_FINDINGS=("applied_pattern","reference_limit","native_view","context_view")
CONTEXT_TYPES={"tile_3x3","icon_family","model_uv","placed_cutout","gui_state","animation_frames","other"}
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

def load_atlas():
    return json.loads(ATLAS_FILE.read_text(encoding="utf-8"))

def atlas_errors(record,profile,catalog):
    if not isinstance(catalog,dict):raise ValueError("Missing canonical atlas catalog")
    evidence=record.get("atlas")
    if not isinstance(evidence,dict):return ["Missing mandatory atlas review evidence"]
    errors=[]
    if evidence.get("revision")!=catalog["revision"]:
        errors.append("Atlas revision mismatch; use current catalog")
    rule_id=evidence.get("rule")
    rule=catalog["rules"].get(rule_id) if isinstance(rule_id,str) else None
    ref_id=evidence.get("reference")
    ref_version=evidence.get("reference_minecraft")
    refs=record.get("references")
    refs=refs if isinstance(refs,list) else []
    selected=[x for x in refs if isinstance(x,dict) and x.get("id")==ref_id and x.get("minecraft")==ref_version]
    if len(selected)!=1:errors.append("Atlas comparator must identify one matching actual reference ID/version")
    n=ref_id.removeprefix("minecraft:") if isinstance(ref_id,str) else ""
    known=catalog["references"]
    for ref in refs:
        if not isinstance(ref,dict):continue
        rid=ref.get("id")
        if not isinstance(rid,str):continue
        path=rid.removeprefix("minecraft:")
        if ref.get("minecraft")==catalog["minecraft"] and path in known and ref.get("sha256")!=known[path]:
            errors.append("Official atlas SHA-256 mismatch: "+rid)
    if rule_id=="custom":
        if profile!="other":
            errors.append("Custom atlas rule is reserved for genuinely uncovered other profiles")
        if any(profile in q["profiles"] and n in q["reference_ids"] for q in catalog["rules"].values()):
            errors.append("Known applicable comparator requires an atlas rule, not custom")
        if not observation(evidence.get("custom_reason")):
            errors.append("Explain why no existing atlas rule applies")
        if not observation(evidence.get("reference_source")):
            errors.append("Identify actual custom comparator source and verification method")
    elif rule is None:
        errors.append("Unknown or missing atlas rule")
    else:
        if profile not in rule["profiles"]:errors.append("Atlas rule does not match material profile")
        if n not in rule["reference_ids"] or ref_version!=catalog["minecraft"]:
            errors.append("Atlas rule requires an inspected comparator from its official catalog; add target-version reference separately if needed")
    for field in ATLAS_FINDINGS:
        if not observation(evidence.get(field)):errors.append("Missing concrete atlas observation: "+field)
    values=[evidence[k].strip().casefold() for k in ATLAS_FINDINGS if observation(evidence.get(k))]
    if len(set(values))!=len(values):errors.append("Duplicate atlas observations")
    context=evidence.get("context_type")
    if not isinstance(context,str) or context not in CONTEXT_TYPES:errors.append("Missing/invalid atlas context_type")
    elif profile in ("soil","ore","bark","log_end") and context!="tile_3x3":
        errors.append("Ground/ore/log material requires actual 3x3 tiled preview observation")
    return errors

def validate(record,digest,atlas_required=False,catalog=None):
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
    required=COMMON+PROFILES[profile]
    supplied=[findings[k].strip().casefold() for k in required if observation(findings.get(k))]
    if len(set(supplied))!=len(supplied):
        errors.append("Copy-pasted criterion observations; describe each distinct visual property")
    if not observation(record.get("comparison")):
        errors.append("Missing concrete comparison of candidate against actual reference")
    if atlas_required:errors.extend(atlas_errors(record,profile,catalog if catalog is not None else load_atlas()))
    return errors

def belongs(path,project,shared):
    if any(path.startswith(root.rstrip("/")+"/") for root in shared):return True
    if project==".":return path.startswith("src/")
    return path.startswith(project.rstrip("/")+"/")

def project_errors(root,project,paths,atlas_paths=(),catalog=None):
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
        errors.extend(path+": "+e for e in validate(rows.get(path),digest,path in atlas_paths,catalog))
    return errors

def main(argv=None):
    parser=argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--projects",required=True,help="JSON selected project directories")
    parser.add_argument("--check",action="store_true",help="Local committed-source preflight: exit 1 if any project is blocked; no GITHUB_OUTPUT required")
    args=parser.parse_args(argv)
    projects=json.loads(args.projects)
    if not isinstance(projects,list) or any(not isinstance(p,str) or (p!="." and not re.fullmatch(r"projects/[A-Za-z0-9_.-]+",p)) for p in projects):
        raise ValueError("Invalid project selection")
    atlas_paths=set(changed(ROOT,ATLAS_BASELINE)) if projects else set()
    paths=sorted(set(changed(ROOT))|atlas_paths) if projects else []
    catalog=load_atlas() if atlas_paths else None
    blocked=[]
    for project in projects:
        errors=project_errors(ROOT,project,paths,atlas_paths,catalog)
        if errors:
            blocked.append(project)
            print(f"ART REVIEW LOCK: {project} BLOCKED ({len(errors)} issues)")
            for error in errors:print("  "+error)
        else:print(f"ART REVIEW LOCK: {project} evidence check clear (not automated artistic approval)")
    if args.check:return 1 if blocked else 0
    with open(os.environ["GITHUB_OUTPUT"],"a",encoding="utf-8") as f:
        f.write("blocked="+json.dumps(blocked,separators=(",",":"))+"\n")
    return 0

if __name__=="__main__":raise SystemExit(main())
