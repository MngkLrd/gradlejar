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
