import unittest
from tools.qa.control import batch_select,release_candidates
from tools.qa.player_suite import validate

class P7Tests(unittest.TestCase):
    def test_complete_batch(self):
        p={"slug":"alpha","version":"1.0.0","minecraft":"1.21.1","loader":"fabric","qa":{"release_targets":["fabric-mc1.21.1","neoforge-mc1.21.1"]}}
        q=dict(p,loader="neoforge")
        self.assertEqual(batch_select([("a",p),("b",q)],["alpha"]),["a","b"])
        with self.assertRaisesRegex(ValueError,"Incomplete"):batch_select([("a",p)],["alpha"])
        with self.assertRaisesRegex(ValueError,"budget"):batch_select([("a",p),("b",q)],["alpha"],limit=1)
    def test_discovery_no_snapshots(self):
        rows=release_candidates({"versions":[{"id":"snapshot","type":"snapshot"},{"id":"1.21.2","type":"release"}]}, {})
        self.assertEqual([r["id"] for r in rows],["1.21.2"])
    def test_player_requires_real_action_and_assertion(self):
        with self.assertRaises(ValueError):validate({"schema":1,"steps":[{"command":"list","expect":"players","assertion":True,"name":"test"}]})
        steps=validate({"schema":1,"steps":[{"action":"startDig","args":{}},{"command":"execute if block 1 2 3 minecraft:air","expect":"Test passed","assertion":True,"name":"glass broken"}]})
        self.assertEqual(len(steps),2)
