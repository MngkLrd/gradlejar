import unittest
from unittest.mock import patch
from tools.plan_builds import select_projects

class SmartBuildTests(unittest.TestCase):
    def test_explicit_changed_range(self):
        with patch("tools.plan_builds.diff",return_value=["docs/QA-V2.md"]) as diff:
            self.assertEqual(select_projects("@changed:"+"a"*40),[])
            diff.assert_called_once_with("a"*40)
    def test_invalid_ref_rejected(self):
        with self.assertRaisesRegex(ValueError,"exact base"):
            select_projects("@changed:main")
    def test_changed_one_project(self):
        path="projects/fragileglass-fabric-1.21.1"
        with patch("tools.plan_builds.diff",return_value=[path+"/src/main/java/example.java"]):
            self.assertEqual(select_projects("@changed:"+"b"*40),[path])
