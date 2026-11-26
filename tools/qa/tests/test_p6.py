import unittest
from unittest.mock import patch
from tools import plan_builds
from tools.qa.catalog import debt,migration_queue
from tools.qa.common import shared_paths,project_tree,profile
from tools.qa.migrate import proposal

class P6Tests(unittest.TestCase):
    def test_shared_change_selects_consumers(self):
        profiles=[("projects/a",{"qa":{},"shared_sources":["shared/a/common"]}),("projects/b",{"qa":{},"shared_sources":["shared/b/common"]})]
        with patch.object(plan_builds,"known_profiles",return_value=profiles):
            self.assertEqual(plan_builds.select_projects("",["shared/a/common/src/main/java/A.java"]),["projects/a"])
    def test_core_change_selects_enrolled_only(self):
        with patch.object(plan_builds,"known_profiles",return_value=[("projects/a",{"qa":{}}),("projects/b",{})]):
            self.assertEqual(plan_builds.select_projects("",["tools/qa/server.py"]),["projects/a"])
    def test_pilot_fingerprint(self):
        p=profile("projects/fragileglass-fabric-26.3")
        self.assertEqual(len(shared_paths(p)),2)
        self.assertEqual(len(project_tree(p["project_dir"])),64)
    def test_unknown_is_not_confirmed_failure(self):
        rows=[{"project":"projects/a","slug":"a","minecraft":"26.3","loader":"fabric","mode":"legacy","status":"no-recent-evidence"}]
        d=debt(rows,[])
        self.assertNotIn("latest-check-failed",d[0]["reasons"])
        self.assertEqual(migration_queue(rows,d)[0]["status"],"migration-planned-not-approved")
    def test_migration_proposal(self):
        p=proposal("projects/naturalbuoyancy-fabric-26.3","server","tools/qa/scenarios/naturalbuoyancy-standalone.json",["fabric-mc26.3","neoforge-mc26.3"])
        self.assertEqual(p["status"],"proposal-not-approved")
