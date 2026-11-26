"""Unit tests for QA tooling itself; fixtures are deliberately synthetic, never release JARs."""
import hashlib
import json
import os
import socket
import struct
import tempfile
import unittest
import zipfile
from pathlib import Path
from unittest.mock import patch
from tools.qa.common import compatibility, qa_config, sha256, REGISTRY
from tools.qa.static import pack_check, validate_jar
from tools.qa.server import Rcon, scenario_steps, play
from tools.qa.bundle import build
from tools.qa.crash import analyze
from tools.qa.dashboard import render
from tools import plan_builds

class StaticTests(unittest.TestCase):
    def test_pack_formats(self):
        mc=REGISTRY["minecraft"]["26.3"]
        with self.assertRaisesRegex(ValueError,"excludes"):
            pack_check({"pack":{"min_format":[97,1],"max_format":[97,1]}},mc,["data/x/tags/block/a.json"])
        pack_check({"pack":{"min_format":[97,1],"max_format":[121,0]}},mc,["data/x/tags/block/a.json","assets/x/a.json"])
    def jar(self,path,bad=None):
        pack={"pack":{"min_format":[97,1],"max_format":[121,0]}}
        meta={"id":"testmod","version":"1.0.0","depends":{"minecraft":"26.3","fabricloader":">=0.19.5"}}
        with zipfile.ZipFile(path,"w") as z:
            z.writestr("pack.mcmeta",json.dumps(pack))
            z.writestr("fabric.mod.json",json.dumps(meta))
            z.writestr("testmod/Fixture.class",b"\xca\xfe\xba\xbe"+struct.pack(">HH",0,70 if bad=="java" else 69))
            z.writestr("data/testmod/tags/"+("blocks" if bad=="layout" else "block")+"/fixture.json", "{" if bad=="json" else '{"values":["minecraft:glass"]}')
    def test_final_jar_static(self):
        p={"slug":"testmod","version":"1.0.0","minecraft":"26.3","loader":"fabric"}
        with tempfile.TemporaryDirectory() as tmp:
            path=Path(tmp)/"testmod-fabric-mc26.3-1.0.0.jar"
            self.jar(path)
            self.assertEqual(validate_jar(path,p)["sha256"],sha256(path))
            for bad in ("java","layout","json"):
                with self.subTest(bad=bad):
                    self.jar(path,bad)
                    with self.assertRaises(ValueError):validate_jar(path,p)
    def test_new_profile_cannot_skip_policy(self):
        with self.assertRaisesRegex(ValueError,"New project"):
            qa_config({"project_dir":"projects/never-enrolled-test-fixture"})
    def test_unsupported_pair(self):
        with self.assertRaises(ValueError):compatibility({"minecraft":"26.3","loader":"quilt"})

class ScenarioTests(unittest.TestCase):
    def test_no_vacuous_pass(self):
        with self.assertRaisesRegex(ValueError,"positive gameplay"):
            scenario_steps({"schema":1,"steps":[{"command":"list","expect":"players"}]},"fabric")
    def test_loader_filter(self):
        s={"schema":1,"steps":[{"command":"list","expect":"players","assertion":True,"loaders":["neoforge"]}]}
        with self.assertRaises(ValueError):scenario_steps(s,"fabric")
        self.assertEqual(len(scenario_steps(s,"neoforge")),1)
    def test_scenario_failure(self):
        class Fake:
            def cmd(self,c):return "Test failed"
        with self.assertRaisesRegex(ValueError,"Scenario assertion failed"):
            play(Fake(),[{"command":"execute if block 1 2 3 minecraft:air","expect":"Test passed","assertion":True}],{"checks":[]})
    def test_rcon_partial_read_and_packet(self):
        a,b=socket.socketpair()
        try:
            r=Rcon.__new__(Rcon);r.sock=a
            payload=struct.pack("<ii",2,0)+b"Test passed\0\0"
            b.sendall(struct.pack("<i",len(payload))+payload)
            self.assertEqual(r.receive(),(2,0,"Test passed"))
        finally:a.close();b.close()

class BundleTests(unittest.TestCase):
    def test_exact_cohort_and_checksum(self):
        with tempfile.TemporaryDirectory() as tmp:
            root=Path(tmp);reports=root/"reports";jars=root/"jars";reports.mkdir();jars.mkdir()
            targets=["forge-mc1.20.1","fabric-mc1.20.1"]
            for i,target in enumerate(targets):
                loader=target.split("-")[0];name=f"testmod-{target}-1.0.0.jar"
                with zipfile.ZipFile(jars/name,"w") as z:z.writestr("fixture","data")
                sha=sha256(jars/name)
                r={"schema":1,"mode":"server","slug":"testmod","version":"1.0.0","target":target,"loader":loader,"minecraft":"1.20.1","release_targets":targets,"run_id":"42","commit":"abc","qa_level":4,"eligible":True,"status":"passed","runtime":"production-jar","jar":name,"sha256":sha,"installed_sha256":sha}
                (reports/f"{i}.json").write_text(json.dumps(r))
            result=build(reports,jars,root/"good","42")
            self.assertEqual(result[0]["status"],"passed")
            with zipfile.ZipFile(root/"good"/result[0]["archive"]) as z:
                self.assertEqual(z.namelist(),[f"testmod-{t}-1.0.0.jar" for t in targets])
            with (jars/"testmod-fabric-mc1.20.1-1.0.0.jar").open("ab") as f:f.write(b"changed")
            self.assertEqual(build(reports,jars,root/"bad","42")[0]["status"],"incomplete")
            (reports/"1.json").unlink()
            self.assertEqual(build(reports,jars,root/"partial","42")[0]["status"],"incomplete")

class OtherTests(unittest.TestCase):
    def test_diagnostics(self):
        self.assertEqual(analyze("Caused by: org.spongepowered.asm.mixin.injection.throwables.InvalidInjectionException: target")[0]["category"],"mixin")
        self.assertEqual(analyze("java.lang.UnsupportedClassVersionError: A")[0]["category"],"java")
    def test_dashboard_escaping(self):
        s=render([{"slug":"<script>alert(1)</script>","project":"projects/x","version":"1","minecraft":"26.3","loader":"fabric","mode":"server","status":"no-recent-evidence","qa_level":None}],[],"owner/repo")
        self.assertIn("&lt;script&gt;",s)
        self.assertNotIn("<script>alert(1)",s)
    def test_push_range(self):
        with patch.object(plan_builds,"event_payload",return_value={"before":"a"*40}),patch.object(plan_builds,"diff",return_value=["projects/a/build.gradle","projects/b/build.gradle"]) as diff,patch.dict(os.environ,{"GITHUB_EVENT_NAME":"push"}):
            self.assertEqual(plan_builds.changed_paths_from_push(),["projects/a/build.gradle","projects/b/build.gradle"])
            diff.assert_called_once_with("a"*40)
    def test_infra_only_does_not_build_fieldkit(self):
        with patch.object(plan_builds,"known_profiles",return_value=[]):
            self.assertEqual(plan_builds.select_projects("",["tools/qa/crash.py"]),[])
    def test_scenario_watch(self):
        with patch.object(plan_builds,"known_profiles",return_value=[("projects/test",{"qa":{"scenario":"tools/qa/scenarios/test.json"}})]):
            self.assertEqual(plan_builds.select_projects("",["tools/qa/scenarios/test.json"]),["projects/test"])

if __name__=="__main__":unittest.main()
