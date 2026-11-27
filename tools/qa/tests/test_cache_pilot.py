import json
import tempfile
import unittest
from pathlib import Path
from unittest.mock import patch
from tools.qa import cache_pilot

class CachePilotTests(unittest.TestCase):
    def test_exact_environment_identity(self):
        with tempfile.TemporaryDirectory() as t:
            p=Path(t)
            (p/"build-profile.json").write_text(json.dumps(cache_pilot.BASELINE))
            (p/"gradle.properties").write_text("neo_version=21.1.262\n")
            (p/"build.gradle").write_text("plugins { id 'net.neoforged.moddev' version '2.0.148' }\nneoForge { version=project.neo_version }\n")
            cache_pilot.check(p)
            (p/"gradle.properties").write_text("neo_version=21.1.80\n")
            with self.assertRaisesRegex(ValueError,"properties changed"): cache_pilot.check(p)
            (p/"gradle.properties").write_text("neo_version=21.1.262\n")
            (p/"build-profile.json").write_text(json.dumps(dict(cache_pilot.BASELINE,gradle="8.14.3")))
            with self.assertRaisesRegex(ValueError,"gradle"): cache_pilot.check(p)
    def test_measurement(self):
        with tempfile.TemporaryDirectory() as t, patch.object(cache_pilot,"OUT",Path(t)):
            for name,v in [("compare-start",1),("compare-build-start",11),("compare-build-end",26)]:
                (Path(t)/(name+".json")).write_text(json.dumps(v))
            cache_pilot.report("scoped")
            r=json.loads((Path(t)/"scoped-report.json").read_text())
            self.assertEqual(r["setup_seconds"],10)
            self.assertEqual(r["through_verification_seconds"],25)
            self.assertEqual(r["compile_and_verify_seconds"],15)
