import json
import tempfile
import unittest
import zipfile
from pathlib import Path
from tools.qa.content_discovery import reconcile, digest

class ContentDiscoveryTests(unittest.TestCase):
    def setUp(self):
        self.tmp=tempfile.TemporaryDirectory()
        self.addCleanup(self.tmp.cleanup)
        self.jar=Path(self.tmp.name)/"test.jar"
        with zipfile.ZipFile(self.jar,"w") as z:
            for n in ("assets/demo/blockstates/box.json","assets/demo/models/item/box.json",
                      "assets/demo/items/box.json","data/demo/loot_tables/blocks/box.json",
                      "data/demo/recipe/box.json","data/minecraft/recipes/override.json"):
                z.writestr(n,"{}")
            z.writestr("assets/demo/lang/en_us.json",json.dumps({
                "entity.demo.frog":"Frog","effect.demo.sleep":"Sleep",
                "block.demo.box":"Box","item.demo.box":"Box","menu.demo.foo":"Foo"}))
            z.writestr("assets/demo/lang/ru_ru.json","invalid")
    def rows(self,qa=None):
        report=reconcile(self.jar,"demo",qa)
        return report,{(r["kind"],r["id"]):r for r in report["content"]}
    def test_inventory_deduplicates_but_distinguishes_block_item(self):
        report,rows=self.rows()
        self.assertEqual(len(rows),6)
        self.assertEqual(len(rows["block","demo:box"]["sources"]),3)
        self.assertEqual(len(rows["item","demo:box"]["sources"]),3)
        self.assertEqual(rows["recipe","minecraft:override"]["namespace_scope"],"other_namespace")
        self.assertEqual(report["live_registry"],"NOT_RUN")
        self.assertEqual(len(report["warnings"]),1)
    def test_exact_subject_and_kind_only(self):
        qa={"sha256":digest(self.jar),"coverage":{"entries":[
            {"id":"a","kind":"item","subject":"demo:box","status":"PASS"},
            {"id":"b","kind":"block","subject":"Box is usable","status":"PASS"},
            {"id":"c","kind":"recipe","subject":"demo:box","status":"FAIL"},
            {"id":"d","kind":"effect","subject":"demo:sleep","status":"NOT_RUN"}]}}
        _,rows=self.rows(qa)
        self.assertEqual(rows["item","demo:box"]["status"],"DECLARED_PASS")
        self.assertEqual(rows["block","demo:box"]["status"],"NO_EXACT_DECLARATION")
        self.assertEqual(rows["recipe","demo:box"]["status"],"DECLARED_FAIL")
        self.assertEqual(rows["effect","demo:sleep"]["status"],"DECLARED_NOT_RUN")
    def test_missing_evidence_and_wrong_jar(self):
        report,rows=self.rows()
        self.assertEqual(report["evidence"],"NOT_AVAILABLE")
        self.assertTrue(all(r["status"]=="EVIDENCE_UNAVAILABLE" for r in rows.values()))
        with self.assertRaisesRegex(ValueError,"checksum"):
            self.rows({"sha256":"0"*64})
    def test_fail_beats_pass(self):
        qa={"sha256":digest(self.jar),"coverage":{"entries":[
            {"id":s,"kind":"item","subject":"demo:box","status":status}
            for s,status in [("a","PASS"),("b","FAIL")]]}}
        self.assertEqual(self.rows(qa)[1]["item","demo:box"]["status"],"DECLARED_FAIL")
