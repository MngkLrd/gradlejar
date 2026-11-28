import io
import unittest
import zipfile
from tools.qa.actions_dependency import extract_jar, digest

def zipped(name,data):
    b=io.BytesIO()
    with zipfile.ZipFile(b,"w") as z:z.writestr(name,data)
    return b.getvalue()

class TestPinnedActionsDependency(unittest.TestCase):
    def test_exact_jar(self):
        jar=zipped("META-INF/MANIFEST.MF",b"Manifest-Version: 1.0")
        dep={"name":"core.jar","sha256":digest(jar)}
        self.assertEqual(extract_jar(zipped("core.jar",jar),dep),jar)
        with self.assertRaises(ValueError):extract_jar(zipped("other.jar",jar),dep)
        with self.assertRaises(ValueError):extract_jar(zipped("core.jar",b"changed"),dep)
    def test_duplicate_rejected(self):
        jar=zipped("a",b"b")
        b=io.BytesIO()
        with zipfile.ZipFile(b,"w") as z:
            z.writestr("a/core.jar",jar);z.writestr("b/core.jar",jar)
        with self.assertRaises(ValueError):extract_jar(b.getvalue(),{"name":"core.jar","sha256":digest(jar)})
