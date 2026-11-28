import tempfile
import unittest
from pathlib import Path
from tools.qa.naming import public_filename, delivery_copy, sha256

class NamingTests(unittest.TestCase):
    def identity(self,**kw):
        return dict({"version":"1.2.3","minecraft":"1.21.1","loader":"neoforge","series":"worldlogic","addon":"trauma"},**kw)
    def test_examples(self):
        self.assertEqual(public_filename(**self.identity()),"worldlogic_trauma_1.2.3_1.21.1_neoforge.jar")
        self.assertEqual(public_filename(**self.identity(addon="core")),"worldlogic_core_1.2.3_1.21.1_neoforge.jar")
        self.assertEqual(public_filename(mod="fragileglass",version="0.2.1",minecraft="26.3",loader="fabric"),"fragileglass_0.2.1_26.3_fabric.jar")
    def test_reject_rc_and_ambiguous_names(self):
        for v in ("1.0.0-rc33","1.2","1.2.3.4","01.2.3","1.0.0.2.3.1","1.2.3-dev"):
            with self.subTest(v=v), self.assertRaises(ValueError):
                public_filename(**self.identity(version=v))
        for kw in ({"addon":"../core"},{"addon":"Core"},{"addon":None},{"mod":"other"},{"loader":"forge-1"},{"minecraft":"1.21.1.0"}):
            with self.subTest(kw=kw), self.assertRaises(ValueError):
                public_filename(**self.identity(**kw))
    def test_copy_same_bytes_and_conflict_refused(self):
        with tempfile.TemporaryDirectory() as t:
            root=Path(t);source=root/"verified.jar";source.write_bytes(b"fixture bytes")
            h=sha256(source)
            target=delivery_copy(source,root/"out",h,**self.identity())
            self.assertEqual(target.read_bytes(),source.read_bytes())
            self.assertEqual(delivery_copy(source,root/"out",h,**self.identity()),target)
            target.write_bytes(b"conflict")
            with self.assertRaises(ValueError):
                delivery_copy(source,root/"out",h,**self.identity())
            self.assertEqual(target.read_bytes(),b"conflict")
            with self.assertRaises(ValueError):
                delivery_copy(source,root/"other","0"*64,**self.identity())
            self.assertFalse((root/"other").exists())
