"""Prevent historical packaging jobs from returning to the shared build graph."""
import re
import unittest
from pathlib import Path

class BuildGraphTests(unittest.TestCase):
    def test_generic_package_job_only(self):
        workflow=(Path(__file__).resolve().parents[3]/".github/workflows/build.yml").read_text()
        jobs=re.findall(r"^  (package-[a-z0-9-]+):\s*$",workflow,re.M)
        self.assertEqual(jobs,["package-qa-v2"])
        self.assertIn("python3 -m tools.qa.bundle ",workflow)
        self.assertIn("steps.qa.outputs.eligible == 'true'",workflow)
