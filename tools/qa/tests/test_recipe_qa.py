import json
import tempfile
import unittest
import zipfile
from pathlib import Path
from tools.qa.recipe_qa import discover, generate, normalize, write_candidates
from tools.qa.player_suite import validate

class RecipeQATests(unittest.TestCase):
    def setUp(self):
        self.sticks={"type":"minecraft:crafting_shaped","pattern":[" P "," P "],"key":{"P":{"item":"minecraft:oak_planks"}},"result":{"item":"minecraft:stick","count":4}}
    def test_grid_trim_and_real_test(self):
        row=normalize(self.sticks,"test:sticks")
        self.assertEqual(row["grid"],[["minecraft:oak_planks",None],["minecraft:oak_planks",None]])
        steps=validate(generate(row))
        self.assertTrue(any(s.get("action")=="craftGrid" for s in steps))
        self.assertFalse(any(s.get("command","").startswith("give MineModsQA minecraft:stick") for s in steps))
        self.assertTrue(any(s.get("assertion") and "Input consumed" in s["name"] for s in steps))
    def test_modded_and_tags_are_not_passed(self):
        doc=dict(self.sticks,result={"id":"test:new_item"})
        self.assertEqual(normalize(doc,"test:custom")["status"],"MODDED_CLIENT_REQUIRED")
        doc=dict(self.sticks,key={"P":"#minecraft:planks"})
        self.assertEqual(normalize(doc,"test:tag")["status"],"ADAPTER_REQUIRED")
    def test_final_jar_plural_singular_and_shapeless(self):
        with tempfile.TemporaryDirectory() as d:
            jar=Path(d)/"test.jar"
            with zipfile.ZipFile(jar,"w") as z:
                z.writestr("data/test/recipes/sticks.json",json.dumps(self.sticks))
                z.writestr("data/test/recipe/planks.json",json.dumps({"type":"minecraft:crafting_shapeless","ingredients":["minecraft:oak_log"],"result":{"id":"minecraft:oak_planks","count":4}}))
                z.writestr("data/test/recipe/invalid.json","{")
            r=discover(jar,"1.21.1","fabric")
            self.assertEqual(r["counts"],{"GRID_CANDIDATE":2,"INVALID":1})
            write_candidates(r,Path(d)/"out")
            self.assertEqual(len(list((Path(d)/"out").glob("*.json"))),3)
            self.assertTrue(all(x.get("execution")=="NOT_RUN" for x in r["recipes"] if x["status"]=="GRID_CANDIDATE"))
            self.assertEqual(discover(jar,"26.3","neoforge")["counts"]["BACKEND_UNSUPPORTED"],2)
    def test_components_conditions_custom(self):
        for doc in (dict(self.sticks,result={"id":"minecraft:stick","components":{"minecraft:custom_name":"x"}}),dict(self.sticks,conditions=[]),dict(self.sticks,type="test:special")):
            self.assertEqual(normalize(doc,"test:recipe")["status"],"ADAPTER_REQUIRED")
