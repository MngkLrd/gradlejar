import unittest
from tools.qa.coverage import validate_plan, evaluate, prepare
from unittest.mock import patch
from tools.qa.common import profile, qa_config

class CoverageTests(unittest.TestCase):
    def setUp(self):
        self.target="fabric-mc1.21.1"
        self.steps=[{"name":"glass", "command":"execute if block 1 2 3 minecraft:air","expect":"Test passed","assertion":True}]
        self.rows=[{"id":"glass","kind":"mechanic","subject":"Break glass","checks":[{"backend":"server","name":"glass"}]}]
    def plan(self,rows=None,steps=None):
        return validate_plan(self.rows if rows is None else rows,self.target,self.steps if steps is None else steps,[],{self.target})
    def test_missing_and_duplicate_assertions_block(self):
        for steps in ([], self.steps*2, [dict(self.steps[0],assertion=False)]):
            with self.assertRaisesRegex(ValueError,"Missing or ambiguous"):
                self.plan(steps=steps)
    def test_unexecuted_or_failed_cannot_pass(self):
        plan=self.plan()
        self.assertEqual(evaluate(plan,{"checks":[]})["counts"]["NOT_RUN"],1)
        self.assertEqual(evaluate(plan,{"checks":[{"name":"glass","passed":True,"assertion":False}]})["status"],"incomplete")
        self.assertEqual(evaluate(plan,{"checks":[{"name":"glass","passed":False,"assertion":True}]})["counts"]["FAIL"],1)
        self.assertEqual(evaluate(plan,{"checks":[{"name":"glass","passed":True,"assertion":True}]})["status"],"declared-pass")
    def test_target_exceptions(self):
        row=dict(self.rows[0],targets=["forge-mc1.20.1"])
        with self.assertRaisesRegex(ValueError,"Invalid coverage targets"):
            self.plan(rows=[row])
        with self.assertRaisesRegex(ValueError,"No coverage entries"):
            validate_plan([row],self.target,self.steps,[],{self.target,"forge-mc1.20.1"})
    def test_player_evidence_not_server(self):
        row=dict(self.rows[0],checks=[{"backend":"player","name":"glass"}])
        plan=validate_plan([row],self.target,self.steps,self.steps,{self.target})
        report={"checks":[{"name":"glass","passed":True,"assertion":True}]}
        self.assertEqual(evaluate(plan,report)["status"],"incomplete")
        report["virtual_player"]={"checks":report["checks"]}
        self.assertEqual(evaluate(plan,report)["status"],"declared-pass")
    def test_real_pilot_references(self):
        for path in ("projects/fragileglass-fabric-1.21.1","projects/fragileglass-neoforge-26.3","projects/naturalbuoyancy-fabric-26.3"):
            p=profile(path)
            self.assertTrue(prepare(p,qa_config(p)))
    def test_new_profile_requires_coverage(self):
        p=profile("projects/naturalbuoyancy-fabric-26.3")
        q=qa_config(p)
        with patch("tools.qa.coverage.json.loads",return_value={"schema":1,"steps":[]}),patch("tools.qa.coverage.unchanged_baseline",return_value=False):
            with self.assertRaisesRegex(ValueError,"requires machine-readable"):
                prepare(p,q)
