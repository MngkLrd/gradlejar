import unittest
from tools.qa.runtime_registry import CONTROLS, PROBE, validate, compare

class RuntimeRegistryTests(unittest.TestCase):
    def fixture(self):
        return {"schema":1,"registries":{k:[v] for k,v in CONTROLS.items()}}
    def test_controls(self):
        data=self.fixture()
        validate(data)
        with self.assertRaisesRegex(ValueError,"Code-only"):
            validate(data,fixture=True)
        data["registries"]["item"].append(PROBE+":code_only")
        validate(data,fixture=True)
        with self.assertRaisesRegex(ValueError,"Code-only"):
            validate(data)
    def test_bad_evidence(self):
        for change in ("missing","duplicate","negative","empty"):
            data=self.fixture()
            if change=="missing": del data["registries"]["recipe"]
            elif change=="duplicate": data["registries"]["item"].append("minecraft:stone")
            elif change=="negative": data["registries"]["item"].append(PROBE+":nonexistent")
            else: data["registries"]["item"]=[]
            with self.assertRaises(ValueError): validate(data)
    def test_runtime_only_is_not_resource_hint(self):
        data=self.fixture()["registries"]
        data["item"] += ["demo:only","demo:both"]
        result=compare({"content":[{"kind":"item","id":"demo:both"},{"kind":"block","id":"demo:hint"}]},data,"demo")
        self.assertEqual(result["runtime_only"],[{"kind":"item","id":"demo:only"}])
        self.assertEqual(result["resource_hint_not_active"],[{"kind":"block","id":"demo:hint"}])
        self.assertEqual(len(result["runtime_confirmed"]),2)
