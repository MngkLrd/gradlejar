import tempfile
import unittest
import zipfile
from pathlib import Path
from unittest.mock import patch
from tools.qa.legacy_package import select,package,TARGETS

class LegacyPackagingTests(unittest.TestCase):
    def cohort(self,slug="keepsigntext"):
        return [{"slug":slug,"version":"1.0.1","loader":l,"minecraft":v,
                 "project_dir":f"projects/{slug}-{l}-{v}",
                 "jar_glob":f"{slug}-{l}-mc{v}-1.0.1.jar"} for l,v in TARGETS]
    def test_unrelated_and_partial_never_selected(self):
        rows=self.cohort()
        with patch("tools.qa.legacy_package.LEGACY",{p["project_dir"] for p in rows}):
            self.assertEqual(select([{"slug":"worldlogic"}],"push"),[])
            self.assertEqual(select(rows[:1],"push"),[])
            self.assertEqual(select(rows,"pull_request"),[])
            self.assertEqual(select(rows,"workflow_dispatch"),[])
            self.assertEqual(select(rows,"push"),[{"slug":"keepsigntext","version":"1.0.1"}])
    def test_no_qa_fallback_or_mixed_revision(self):
        rows=self.cohort()
        with patch("tools.qa.legacy_package.LEGACY",{p["project_dir"] for p in rows}):
            self.assertEqual(select(rows[:-1]+[dict(rows[-1],qa={"mode":"server"})],"push"),[])
            self.assertEqual(select(rows[:-1]+[dict(rows[-1],version="1.0.0")],"push"),[])
            self.assertEqual(select(rows[:-1]+[dict(rows[-1],draft=True)],"push"),[])
            self.assertEqual(select(rows[:-1]+[rows[0]],"push"),[])
        with patch("tools.qa.legacy_package.LEGACY",set()):
            self.assertEqual(select(rows,"push"),[])
    def test_exact_archive_and_class_checks(self):
        with tempfile.TemporaryDirectory() as tmp:
            root=Path(tmp);source=root/"jars";source.mkdir()
            rows=self.cohort("campfirebatch")
            for p in rows:
                with zipfile.ZipFile(source/p["jar_glob"],"w") as z:
                    z.writestr("demo/BatchLoader.class",b"fixture")
            archive=package("campfirebatch","1.0.1",source,root/"out")
            with zipfile.ZipFile(archive) as z:
                self.assertEqual(len(z.namelist()),11)
                self.assertTrue(z.namelist()[0].startswith("Forge/"))
            with zipfile.ZipFile(source/rows[0]["jar_glob"],"w") as z:
                z.writestr("other.class",b"fixture")
            with self.assertRaisesRegex(ValueError,"Missing legacy validation"):
                package("campfirebatch","1.0.1",source,root/"out")
            (source/rows[0]["jar_glob"]).unlink()
            with self.assertRaisesRegex(ValueError,"Missing, duplicate"):
                package("campfirebatch","1.0.1",source,root/"out")
