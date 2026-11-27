import json
import unittest
from tools.qa.common import ROOT
from tools.qa.player_suite import validate,validate_args

class BotActionsTests(unittest.TestCase):
    def test_control_scenario(self):
        suite=json.loads((ROOT/"tools/qa/scenarios/bot-control-player.json").read_text())
        self.assertTrue(validate(suite))
    def test_bounds(self):
        for action,args in [("move",{"ms":4001}),("craft",{"item":"oak_planks","count":0}),("place",{"item":"stick","face":6}),("attack",{"entity":""}),("equip",{"item":"stick","destination":"invalid"})]:
            with self.subTest(action=action),self.assertRaises(ValueError):
                validate_args(action,args)
    def test_valid_actions(self):
        for action,args in [("move",{"ms":800,"yaw":180}),("jump",{}),("craft",{"item":"oak_planks","count":1}),("attack",{"entity":"pig"}),("equip",{"item":"diamond_sword"}),("use",{"item":"shield","ms":500})]:
            validate_args(action,args)
