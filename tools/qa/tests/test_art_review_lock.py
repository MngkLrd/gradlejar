import hashlib
import json
import tempfile
import unittest
from pathlib import Path
from tools.qa.art_review_lock import COMMON,PROFILES,validate,project_errors

class ArtReviewLockTests(unittest.TestCase):
    def row(self,digest):
        return {"sha256":digest,"verdict":"PASS","material_profile":"log_end",
                "references":[{"id":"minecraft:block/spruce_log_top","minecraft":"1.21.1","sha256":"a"*64}],
                "comparison":"Outer bark has local variation unlike flat border",
                "findings":{k:f"Fixture observation for distinct criterion {k}" for k in COMMON+PROFILES["log_end"]}}
    def test_hash_and_material_criteria_are_mandatory(self):
        r=self.row("b"*64)
        self.assertEqual(validate(r,"b"*64),[])
        self.assertTrue(validate(r,"c"*64))
        del r["findings"]["ring_microstructure"]
        self.assertTrue(any("ring_microstructure" in x for x in validate(r,"b"*64)))
        r["verdict"]="NOT_RUN"
        self.assertTrue(any("verdict" in x for x in validate(r,"b"*64)))
    def test_references_and_nonempty_observations(self):
        r=self.row("b"*64);r["references"]=[]
        r["findings"]["micro_contrast"]="PASS"
        errors=validate(r,"b"*64)
        self.assertTrue(any("reference" in x for x in errors))
        self.assertTrue(any("micro_contrast" in x for x in errors))
    def test_only_changed_project_assets_and_exact_bytes(self):
        with tempfile.TemporaryDirectory() as t:
            root=Path(t);project="projects/demo";p=root/project;p.mkdir(parents=True)
            (p/"build-profile.json").write_text("{}")
            path=project+"/src/main/resources/assets/demo/textures/block/test.png"
            other="projects/other/src/main/resources/assets/other/textures/block/test.png"
            self.assertEqual(project_errors(root,project,[other]),[])
            f=root/path;f.parent.mkdir(parents=True);f.write_bytes(b"fixture")
            self.assertTrue(project_errors(root,project,[path,other]))
            (p/"art-review.json").write_text(json.dumps({"schema":1,"assets":{path:self.row(hashlib.sha256(b"fixture").hexdigest())}}))
            self.assertEqual(project_errors(root,project,[path,other]),[])
            f.write_bytes(b"changed")
            self.assertTrue(project_errors(root,project,[path]))

    def test_repeated_finding_is_not_review_evidence(self):
        r=self.row("b"*64)
        r["findings"]["silhouette"]=r["findings"]["cluster_structure"]
        self.assertTrue(any("Copy-pasted" in x for x in validate(r,"b"*64)))

    def test_fixed_baseline_detects_changes_after_code_only_commit(self):
        import subprocess
        from tools.qa.art_review_lock import changed
        with tempfile.TemporaryDirectory() as t:
            root=Path(t)
            def git(*args):
                return subprocess.check_output(["git",*args],cwd=root).decode().strip()
            git("init","-q")
            git("config","user.email","fixture@example.invalid")
            git("config","user.name","Fixture")
            p=root/"src/main/resources/assets/demo/textures/block/stone.png"
            p.parent.mkdir(parents=True)
            p.write_bytes(b"old")
            git("add",".");git("commit","-qm","baseline")
            baseline=git("rev-parse","HEAD")
            self.assertEqual(changed(root,baseline),[])
            p.write_bytes(b"new")
            git("add",".");git("commit","-qm","texture")
            (root/"code.java").write_text("code")
            git("add",".");git("commit","-qm","code only")
            self.assertEqual(changed(root,baseline),[p.relative_to(root).as_posix()])
            p.write_bytes(b"old")
            git("add",".");git("commit","-qm","revert texture")
            self.assertEqual(changed(root,baseline),[])

    def atlas_row(self):
        from tools.qa.art_review_lock import load_atlas
        c=load_atlas()
        row=self.row("b"*64)
        row["references"][0]["sha256"]=c["references"]["block/spruce_log_top"]
        row["atlas"]={"revision":c["revision"],"rule":"growth_rings","reference":"minecraft:block/spruce_log_top",
            "reference_minecraft":"1.21.1","applied_pattern":"Break ring edges with secondary warm shade.",
            "reference_limit":"Ring shape depends on heartwood and bark.",
            "native_view":"Outer border is distinct at native size.",
            "context_view":"Repeat three by three; no unplanned seams.",
            "context_type":"tile_3x3"}
        return row,c

    def test_atlas_required_and_hash_verified(self):
        row,c=self.atlas_row()
        self.assertEqual(validate(row,"b"*64,True,c),[])
        row["references"][0]["sha256"]="f"*64
        self.assertTrue(any("Official atlas SHA-256" in e for e in validate(row,"b"*64,True,c)))
        del row["atlas"]
        self.assertTrue(any("Missing mandatory atlas" in e for e in validate(row,"b"*64,True,c)))
        self.assertEqual(validate(row,"b"*64),[])  # original evidence contract retained for unchanged old assets

    def test_wrong_rule_custom_and_context_rejected(self):
        row,c=self.atlas_row()
        row["atlas"]["rule"]="needles"
        self.assertTrue(any("profile" in e for e in validate(row,"b"*64,True,c)))
        row["atlas"]["rule"]="custom"
        self.assertTrue(any("reserved" in e for e in validate(row,"b"*64,True,c)))
        row["atlas"]["rule"]="growth_rings";row["atlas"]["context_type"]="icon_family"
        self.assertTrue(any("3x3" in e for e in validate(row,"b"*64,True,c)))

    def test_duplicate_atlas_observations_and_unbound_reference(self):
        row,c=self.atlas_row()
        row["atlas"]["context_view"]=row["atlas"]["native_view"]
        row["atlas"]["reference"]="minecraft:block/oak_log_top"
        errs=validate(row,"b"*64,True,c)
        self.assertTrue(any("Duplicate atlas" in e for e in errs))
        self.assertTrue(any("matching actual reference" in e for e in errs))

    def test_atlas_scope_is_per_asset_not_global_migration(self):
        with tempfile.TemporaryDirectory() as t:
            root=Path(t);project="projects/demo";p=root/project;p.mkdir(parents=True)
            (p/"build-profile.json").write_text("{}")
            path=project+"/src/main/resources/assets/demo/textures/block/test.png"
            f=root/path;f.parent.mkdir(parents=True);f.write_bytes(b"fixture")
            row=self.row(hashlib.sha256(b"fixture").hexdigest())
            (p/"art-review.json").write_text(json.dumps({"schema":1,"assets":{path:row}}))
            self.assertEqual(project_errors(root,project,[path]),[])
            self.assertTrue(any("Missing mandatory atlas" in e for e in project_errors(root,project,[path],{path})))

    def test_catalog_integrity_and_custom_uncovered_profile(self):
        from tools.qa.art_review_lock import load_atlas
        c=load_atlas()
        self.assertEqual(len(c["references"]),100)
        self.assertEqual(len(c["benchmark_r9_reference_ids"]),57)
        self.assertTrue(set(c["benchmark_r9_reference_ids"]).issubset(c["references"]))
        all_refs=set()
        for rule in c["rules"].values():
            self.assertTrue(set(rule["profiles"]).issubset(PROFILES))
            self.assertTrue(set(rule["reference_ids"]).issubset(c["references"]))
            all_refs.update(rule["reference_ids"])
        self.assertEqual(all_refs,set(c["references"]))
        row=self.row("b"*64);row["material_profile"]="other"
        row["findings"]={k:f"Fixture observation for distinct criterion {k}" for k in COMMON+PROFILES["other"]}
        row["references"]=[{"id":"minecraft:gui/sprites/test","minecraft":"1.21.1","sha256":"a"*64}]
        row["atlas"]={"revision":"r9","rule":"custom","reference":"minecraft:gui/sprites/test","reference_minecraft":"1.21.1",
                      "applied_pattern":"Purposeful panel border and icon inset.",
                      "reference_limit":"Not a tile or material texture.",
                      "native_view":"Icon readable at original display size.",
                      "context_view":"Panel needs inspection with adjacent slots.",
                      "context_type":"gui_state","custom_reason":"Uncovered interface type needs GUI comparator.",
                      "reference_source":"Official client exact resource path and digest."}
        self.assertEqual(validate(row,"b"*64,True,c),[])

    def test_local_preflight_exit_and_ci_output_contract(self):
        from unittest.mock import patch
        from tools.qa import art_review_lock as lock
        from contextlib import redirect_stdout
        import io
        import os
        with tempfile.TemporaryDirectory() as t, redirect_stdout(io.StringIO()):
            out=Path(t)/"github-output"
            args=["--projects",'["projects/demo"]']
            with patch.object(lock,"changed",return_value=["projects/demo/src/main/resources/assets/demo/textures/block/test.png"]), patch.object(lock,"load_atlas",return_value={}), patch.object(lock,"project_errors",return_value=["Missing atlas evidence"]) as check, patch.dict(os.environ,{"GITHUB_OUTPUT":str(out)}):
                self.assertEqual(lock.main(args+["--check"]),1)
                self.assertFalse(out.exists())
                self.assertEqual(lock.main(args),0)
                self.assertEqual(out.read_text(),'blocked=["projects/demo"]\n')
                self.assertTrue(check.call_args.args[3])
                check.return_value=[]
                self.assertEqual(lock.main(args+["--check"]),0)

    def test_equipment_path_profile_and_counterpart(self):
        from tools.qa.art_review_lock import equipment_errors, conventional_profile, load_atlas
        c=load_atlas()
        self.assertEqual(conventional_profile("assets/demo/textures/item/copper_axe.png"),"tool")
        self.assertEqual(conventional_profile("assets/demo/textures/item/copper_chestplate_overlay.png"),"armor_icon_overlay")
        with tempfile.TemporaryDirectory() as t:
            root=Path(t);project="projects/demo"
            prefix=project+"/src/main/resources/assets/demo/textures/"
            path=prefix+"item/copper_chestplate.png"
            uv=prefix+"models/armor/copper_layer_1.png"
            f=root/uv;f.parent.mkdir(parents=True);f.write_bytes(b"uv")
            row={"material_profile":"armor_icon","equipment":{"slots":["chestplate"],
                "binding":"Material resolves outer copper layer.",
                "client_status":"NOT_RUN","client_evidence":"Graphical client is not available.",
                "counterparts":[{"slot":"chestplate","role":"worn_uv","path":uv,"sha256":hashlib.sha256(b"uv").hexdigest()}]}}
            self.assertEqual(equipment_errors(root,project,[],path,row,c),[])
            f.write_bytes(b"changed")
            self.assertTrue(any("stale" in e for e in equipment_errors(root,project,[],path,row,c)))
            row["material_profile"]="other"
            self.assertTrue(any("requires armor_icon" in e for e in equipment_errors(root,project,[],path,row,c)))

    def test_armor_layers_and_context(self):
        from tools.qa.art_review_lock import equipment_errors, load_atlas
        c=load_atlas()
        path="projects/demo/src/main/resources/assets/demo/textures/models/armor/copper_layer_2.png"
        row={"material_profile":"armor_uv","equipment":{"slots":["chestplate"],
             "binding":"Actual inner layer resource binding.",
             "client_status":"NOT_RUN","client_evidence":"No graphical client in this environment.",
             "counterparts":[{"slot":"chestplate","role":"icon","vanilla_reference":"item/iron_chestplate",
             "sha256":c["references"]["item/iron_chestplate"],"reuse_reason":"Explicitly reuse existing vanilla inventory model."}]}}
        self.assertTrue(any("inner/outer" in e for e in equipment_errors(Path("."),"projects/demo",[],path,row,c)))
        row,c=self.atlas_row()
        row["material_profile"]="armor_uv"
        row["findings"]={k:f"Fixture observation for distinct criterion {k}" for k in COMMON+PROFILES["armor_uv"]}
        ref="models/armor/iron_layer_1"
        row["references"]=[{"id":"minecraft:"+ref,"minecraft":"1.21.1","sha256":c["references"][ref]}]
        row["atlas"].update(rule="armor_metal_uv",reference="minecraft:"+ref,context_type="icon_family")
        self.assertTrue(any("model/UV" in e for e in validate(row,"b"*64,True,c)))
        row["atlas"]["context_type"]="model_uv"
        self.assertEqual(validate(row,"b"*64,True,c),[])
