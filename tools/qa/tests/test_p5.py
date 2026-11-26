import hashlib
import io
import json
import tempfile
import unittest
import zipfile
from pathlib import Path
from tools.qa.scaffold import generate
from tools.qa.common import qa_config
from tools.qa.scenario_gen import generate as gen_scenario
from tools.qa.compare_perf import differences
from tools.qa.release_prep import prepare
from tools.qa.port_plan import plan,duplicates

class P5Tests(unittest.TestCase):
    def test_all_draft_profiles(self):
        with tempfile.TemporaryDirectory() as tmp:
            out=Path(tmp)/"drafts"
            rows=generate({"id":"testmod","version":"0.1.0","name":"Test","description":"Draft","mode":"server"},out)
            self.assertEqual(len(rows),11)
            self.assertEqual(len({r["target"] for r in rows}),11)
            for row in rows:
                p=out/row["directory"]
                self.assertTrue((p/"build.gradle").is_file())
                self.assertTrue((p/"src/main/resources/pack.mcmeta").is_file())
                with self.assertRaisesRegex(ValueError,"Draft"):
                    qa_config(row["profile"])
            self.assertEqual(json.loads((out/"scenario-draft.json").read_text())["steps"],[])
            with self.assertRaises(ValueError):generate({"id":"testmod","version":"0.1.0","name":"Test","description":"Draft"},out)
    def test_scenario_no_empty_assertion(self):
        with self.assertRaisesRegex(ValueError,"No gameplay"):
            gen_scenario({"name":"test","operations":[{"type":"wait","seconds":1}]})
        s=gen_scenario({"name":"test","operations":[{"type":"assert_block","pos":[1,2,3],"block":"minecraft:air","name":"Break"}]})
        self.assertEqual(s["steps"][0]["command"],"execute if block 1 2 3 minecraft:air")
    def test_delta_missing_values(self):
        a={p:{"effective_tps":20,"mspt":None,"rss_mib_process_tree":500} for p in ("idle","loaded")}
        b={p:{"effective_tps":19,"mspt":2,"rss_mib_process_tree":510} for p in ("idle","loaded")}
        self.assertEqual(differences(a,b)["idle"],{"effective_tps":-1,"mspt":None,"rss_mib_process_tree":10})
    def test_port_plan_and_duplicate_report(self):
        p=plan("projects/fragileglass-fabric-1.21.1",["all"])
        self.assertEqual(len(p["targets"]),11)
        self.assertEqual(p["status"],"plan-only")
        r=duplicates(["projects/fragileglass-fabric-26.2","projects/fragileglass-fabric-26.3"])
        self.assertGreater(r["duplicated_bytes"],0)
    def test_release_hash_and_no_drafts(self):
        with tempfile.TemporaryDirectory() as tmp:
            root=Path(tmp);reports=root/"reports";reports.mkdir()
            n="testmod-fabric-mc26.3-1.0.0.jar";buf=io.BytesIO()
            with zipfile.ZipFile(buf,"w") as z:z.writestr("fixture","data")
            b=buf.getvalue();h=hashlib.sha256(b).hexdigest()
            archive=root/"bundle.zip"
            with zipfile.ZipFile(archive,"w") as z:z.writestr(n,b)
            r={"slug":"testmod","version":"1.0.0","jar":n,"eligible":True,"qa_level":4,"status":"passed","runtime":"production-jar","sha256":h,"installed_sha256":h,"target":"fabric-mc26.3","minecraft":"26.3","loader":"fabric","release_targets":["fabric-mc26.3"],"commit":"abc","run_id":"42"}
            file=reports/"report.json";file.write_text(json.dumps(r))
            s={"slug":"testmod","version":"1.0.0","summary":"Test","changes":["Actual change"],"targets":["fabric-mc26.3"]}
            self.assertEqual(len(prepare(s,archive,reports,root/"good")),1)
            r["installed_sha256"]="0"*64;file.write_text(json.dumps(r))
            with self.assertRaisesRegex(ValueError,"runtime hash"):prepare(s,archive,reports,root/"bad")
            r["eligible"]=False;file.write_text(json.dumps(r))
            with self.assertRaisesRegex(ValueError,"Unapproved"):prepare(s,archive,reports,root/"draft")
