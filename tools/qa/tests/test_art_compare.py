import io
import tempfile
import unittest
from pathlib import Path
try:
    from PIL import Image
except ImportError:
    Image=None

@unittest.skipIf(Image is None,"Pillow installed by visual QA workflow")
class ArtCompareTests(unittest.TestCase):
    def png(self, im):
        stream=io.BytesIO()
        im.save(stream,format="PNG")
        return stream.getvalue()

    def test_components_and_transparent_pixels(self):
        from tools.qa.art_compare import palette
        im=Image.new("RGBA",(4,3),(0,0,0,0))
        for x,y in [(0,0),(1,0),(3,2)]: im.putpixel((x,y),(120,80,40,255))
        im.putpixel((2,1),(200,140,80,255))
        p=palette(im)
        self.assertEqual(p[0]["pixels"],3)
        self.assertEqual(p[0]["components"],2)
        self.assertEqual(p[0]["largest_component"],2)
        self.assertEqual(p[0]["visible_fraction"],.75)

    def test_comparison_has_no_automatic_pass(self):
        from tools.qa.art_compare import make_comparison
        c=self.png(Image.new("RGBA",(16,16),(180,80,30,255)))
        ref=self.png(Image.new("RGBA",(16,16),(200,120,80,255)))
        with tempfile.TemporaryDirectory() as t:
            out=Path(t)
            r=make_comparison("flask.png",c,[("item/example",ref)],"1.21.1","fixture",out)
            self.assertEqual(r["aesthetic_review"],"NOT_RUN")
            self.assertEqual(r["client_rendering"],"NOT_RUN")
            self.assertNotEqual(r["assets"][0]["sha256"],r["assets"][1]["sha256"])
            self.assertEqual(r["scale"],8)
            with Image.open(out/"comparison.png") as im:
                self.assertGreater(im.width,256)

    def test_reference_path_safety(self):
        from tools.qa.art_compare import reference_path
        self.assertEqual(reference_path("item/copper_ingot"),"assets/minecraft/textures/item/copper_ingot.png")
        for value in ("item/../secret","item//a","https://example.com/a","gui/x"):
            with self.assertRaises(ValueError): reference_path(value)
