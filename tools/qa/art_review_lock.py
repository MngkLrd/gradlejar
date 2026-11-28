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
# R11 item/armor-specific obligations only apply to new/changed artwork since adoption.
ITEM_BASELINE="327a26138826b6142651918f684ee454eadea222"
# R12 mob evidence is scoped to new/changed artwork, not historical migration.
MOB_BASELINE="73464b88ad7af3089a30fd21244f4688d5c2346e"
ATLAS_FILE=ROOT/"tools/qa/research/vanilla-art-rules.json"
ATLAS_FINDINGS=("applied_pattern","reference_limit","native_view","context_view")
CONTEXT_TYPES={"tile_3x3","icon_family","model_uv","placed_cutout","gui_state","animation_frames","other"}
TEXTURE=re.compile(r"(?:^|/)assets/[a-z0-9_.-]+/textures/.+\.png$")
SHA=re.compile(r"[a-f0-9]{64}")
REF=re.compile(r"minecraft:[a-z0-9_./-]+")
VERSION=re.compile(r"[a-zA-Z0-9_.+-]+")
COMMON=["silhouette","cluster_structure","micro_contrast","light_and_volume","material_continuity","edge_variation","variant_identity","vanilla_comparison"]
PROFILES={"bark":["grain_direction","furrows_and_ridges"],"log_end":["growth_rings","ring_microstructure","bark_boundary"],"conifer":["needle_direction","structural_transparency"],"broadleaf":["leaf_groups","canopy_depth","structural_transparency"],"soil":["aggregate_structure","tile_rhythm"],"ore":["host_rock","mineral_inclusion","inclusion_light"],"clothing":["folds_and_seams","material_specific_shading"],"sapling":["branch_hierarchy","species_silhouette","foliage_attachment"],"vessel":["construction","surface_turn","material_specific_shading"],"other":["functional_material_cues"]}

PROFILES.update({
    "tool":["working_head","grip_and_joint","held_orientation"],
    "stateful_item":["state_identity","anchor_continuity","state_transition"],
    "functional_item":["functional_parts","use_context"],
    "consumable":["edible_or_liquid_material","container_or_food_shape"],
    "armor_icon":["slot_silhouette","construction_and_material","equipped_identity"],
    "armor_icon_overlay":["base_alignment","composite_readability","tint_role"],
    "armor_uv":["uv_face_layout","slot_coverage","seam_and_layer_alignment","equipped_identity"],
    "mob_body":["species_anatomy","uv_face_layout","pivot_and_motion","behavior_readability"],
    "mob_layer":["base_alignment","layer_render_role","state_visibility","behavior_readability"],
})
ARMOR_SLOTS={"helmet","chestplate","leggings","boots"}
ARMOR_ICON=re.compile(r"(?:^|/)textures/item/(?:[^/]+/)*[^/]+_(helmet|chestplate|leggings|boots)(_overlay)?\.png$")
ARMOR_UV=re.compile(r"(?:^|/)textures/models/armor/(?:[^/]+/)*[^/]+_layer_([12])(?:_overlay)?\.png$")
TOOL_ICON=re.compile(r"(?:^|/)textures/item/(?:[^/]+/)*[^/]+_(?:axe|pickaxe|sword|hoe|shovel)\.png$")

def conventional_profile(path):
    if ARMOR_UV.search(path):return "armor_uv"
    match=ARMOR_ICON.search(path)
    if match:return "armor_icon_overlay" if match.group(2) else "armor_icon"
    if TOOL_ICON.search(path):return "tool"
    return None

def equipment_errors(root,project,shared,path,record,catalog):
    if not isinstance(record,dict):return []
    expected=conventional_profile(path)
    profile=record.get("material_profile")
    # Path rules are conservative hints for conventional filenames, not Java type discovery.
    # An explicitly explained non-equipment asset may use the correct semantic profile.
    if expected and profile!=expected:
        if not observation(record.get("non_equipment_reason")):
            return ["Conventional item/armor filename requires "+expected+" profile or actual non-equipment explanation"]
    if profile not in ("armor_icon","armor_icon_overlay","armor_uv"):return []
    e=record.get("equipment")
    if not isinstance(e,dict):return ["Missing equipment review with exact counterpart binding"]
    errors=[]
    slots=e.get("slots")
    if not isinstance(slots,list) or not slots or any(not isinstance(s,str) or s not in ARMOR_SLOTS for s in slots) or len(set(slots))!=len(slots):
        return ["Invalid equipment slot list"]
    match=ARMOR_ICON.search(path)
    if match and slots!=[match.group(1)]:errors.append("Armor icon slot disagrees with conventional filename")
    if profile in ("armor_icon","armor_icon_overlay") and len(slots)!=1:
        errors.append("Armor icon must identify one slot")
    match=ARMOR_UV.search(path)
    if match:
        allowed={"leggings"} if match.group(1)=="2" else ARMOR_SLOTS-{"leggings"}
        if not set(slots)<=allowed:errors.append("Armor UV slot disagrees with inner/outer layer")
    if not observation(e.get("binding")):errors.append("Describe actual item model/material/renderer binding")
    status=e.get("client_status")
    if status not in ("PASS","NOT_RUN","UNSUPPORTED"):
        errors.append("Declare separate equipped-client status; rejected/failed visual review cannot be accepted")
    if not observation(e.get("client_evidence")):errors.append("Record actual client evidence or explicit NOT_RUN/UNSUPPORTED reason")
    links=e.get("counterparts")
    if not isinstance(links,list) or not links:
        return errors+["Missing icon/worn-layer counterpart"]
    covered=set()
    for link in links:
        if not isinstance(link,dict):
            errors.append("Invalid equipment counterpart");continue
        slot=link.get("slot");role=link.get("role");digest=link.get("sha256")
        expected_role="worn_uv" if profile=="armor_icon" else "icon"
        if slot not in slots or role!=expected_role or not isinstance(digest,str) or not SHA.fullmatch(digest):
            errors.append("Invalid equipment counterpart slot/role/hash");continue
        rel=link.get("path")
        ref=link.get("vanilla_reference")
        if (rel is None)==(ref is None):
            errors.append("Counterpart requires one project PNG path or catalog vanilla reference");continue
        if rel is not None:
            if not isinstance(rel,str) or not TEXTURE.search(rel) or any(x in ("",".","..") for x in rel.split("/")) or not belongs(rel,project,shared) or rel==path:
                errors.append("Invalid equipment counterpart path");continue
            f=(root/rel).resolve()
            if not f.is_relative_to(root.resolve()) or not f.is_file() or hashlib.sha256(f.read_bytes()).hexdigest()!=digest:
                errors.append("Missing or stale equipment counterpart PNG: "+rel);continue
            uv_match=ARMOR_UV.search(rel);icon_match=ARMOR_ICON.search(rel)
            if role=="worn_uv" and icon_match:
                errors.append("Worn counterpart cannot be a conventional inventory icon");continue
            if role=="icon" and uv_match:
                errors.append("Icon counterpart cannot be a conventional worn UV");continue
            if role=="worn_uv" and uv_match and ((uv_match.group(1)=="2")!=(slot=="leggings")):
                errors.append("Counterpart must use correct inner/outer layer");continue
            if role=="icon" and icon_match and (icon_match.group(1)!=slot or icon_match.group(2)):
                errors.append("Counterpart must be base icon for matching slot");continue
        else:
            if not isinstance(ref,str) or not isinstance(catalog,dict) or catalog["references"].get(ref)!=digest:
                errors.append("Unverified vanilla counterpart");continue
            if role=="worn_uv" and (not ref.startswith("models/armor/") or ("_layer_2" in ref)!=(slot=="leggings")):
                errors.append("Vanilla worn counterpart has wrong layer");continue
            if role=="icon" and (not ref.startswith("item/") or not ref.endswith("_"+slot)):
                errors.append("Vanilla icon counterpart has wrong slot");continue
            if not observation(link.get("reuse_reason")):
                errors.append("Explain intentional vanilla counterpart reuse");continue
        covered.add(slot)
    if covered!=set(slots):errors.append("Missing valid counterpart for every declared equipment slot")
    return errors

MOB_PROFILES={"mob_body","mob_layer"}
MOB_BEHAVIORS={"peaceful","neutral","hostile"}
ENTITY_PNG=re.compile(r"(?:^|/)textures/entity/.+\.png$")
ENTITY_ID=re.compile(r"[a-z0-9_.-]+:[a-z0-9_./-]+")

def mob_errors(root,project,shared,path,record,catalog):
    if not isinstance(record,dict):return []
    profile=record.get("material_profile")
    if profile not in MOB_PROFILES:
        if ENTITY_PNG.search(path) and not observation(record.get("non_mob_reason")):
            return ["Entity texture requires mob_body/mob_layer or concrete non-mob render-role explanation"]
        return []
    e=record.get("mob")
    if not isinstance(e,dict):return ["Missing mob review evidence"]
    errors=[]
    eid=e.get("entity_id")
    if not isinstance(eid,str) or not ENTITY_ID.fullmatch(eid) or any(p in ("",".","..") for p in eid.split(":",1)[1].split("/")):
        errors.append("Invalid mob registry ID")
    if not isinstance(e.get("behavior"),str) or e["behavior"] not in MOB_BEHAVIORS:errors.append("Declare peaceful, neutral or hostile behavior toward player")
    for key in ("behavior_conditions","model_binding","motion_review","client_evidence"):
        if not observation(e.get(key)):errors.append("Missing concrete mob observation: "+key)
    if e.get("client_status") not in ("PASS","NOT_RUN","UNSUPPORTED"):
        errors.append("Declare separate actual client status for mob")
    role=e.get("role")
    if role not in (("base","variant") if profile=="mob_body" else ("overlay",)):
        errors.append("Mob role/profile mismatch")
    if role in ("variant","overlay"):
        link=e.get("base_texture")
        if not isinstance(link,dict):
            return errors+["Mob variant/overlay requires actual base texture binding"]
        digest=link.get("sha256");rel=link.get("path");ref=link.get("vanilla_reference")
        if not isinstance(digest,str) or not SHA.fullmatch(digest) or (rel is None)==(ref is None):
            return errors+["Invalid mob base texture locator/hash"]
        if rel is not None:
            if not isinstance(rel,str) or not TEXTURE.search(rel) or any(x in ("",".","..") for x in rel.split("/")) or not belongs(rel,project,shared) or rel==path:
                return errors+["Invalid mob base texture path"]
            f=(root/rel).resolve()
            if not f.is_relative_to(root.resolve()) or not f.is_file() or hashlib.sha256(f.read_bytes()).hexdigest()!=digest:
                errors.append("Missing or stale mob base texture: "+rel)
        elif not isinstance(ref,str) or not ref.startswith("entity/") or not isinstance(catalog,dict) or catalog["references"].get(ref)!=digest or not observation(link.get("reuse_reason")):
            errors.append("Unverified or unexplained vanilla mob base texture")
    return errors

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
        if profile not in ("other","tool","stateful_item","functional_item","consumable","armor_icon","armor_icon_overlay","armor_uv","mob_body","mob_layer"):
            errors.append("Custom atlas rule is reserved for genuinely uncovered other or specialized item/armor/mob profiles")
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
    elif profile in ("tool","stateful_item","functional_item","consumable","armor_icon","armor_icon_overlay") and context!="icon_family":
        errors.append("Item sprite requires native icon-family review context")
    elif profile=="armor_uv" and context!="model_uv":
        errors.append("Worn armor requires actual model/UV preview review context")
    elif profile in MOB_PROFILES and context!="model_uv":
        errors.append("Mob art requires actual model/UV preview review context")
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

def project_errors(root,project,paths,atlas_paths=(),catalog=None,item_paths=(),mob_paths=()):
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
        if path in item_paths and profile.get("minecraft")=="1.21.1":
            errors.extend(path+": "+e for e in equipment_errors(root,project,profile.get("shared_sources",[]),path,rows.get(path),catalog))
        if path in mob_paths:
            errors.extend(path+": "+e for e in mob_errors(root,project,profile.get("shared_sources",[]),path,rows.get(path),catalog))
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
    item_paths=set(changed(ROOT,ITEM_BASELINE)) if projects else set()
    mob_paths=set(changed(ROOT,MOB_BASELINE)) if projects else set()
    paths=sorted(set(changed(ROOT))|atlas_paths|item_paths|mob_paths) if projects else []
    catalog=load_atlas() if atlas_paths or item_paths or mob_paths else None
    blocked=[]
    for project in projects:
        errors=project_errors(ROOT,project,paths,atlas_paths,catalog,item_paths,mob_paths)
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
