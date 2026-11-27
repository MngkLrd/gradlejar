import io
import json
import tempfile
import unittest
from pathlib import Path
try:
    from PIL import Image
except ImportError:
    Image=None

@unittest.skipIf(Image is None,"Pillow installed by dedicated visual QA workflow")
class VisualLintTests(unittest.TestCase):
    def png(self, im):
        b=io.BytesIO();im.save(b,format="PNG");return b.getvalue()

    def test_clusters_and_transparency(self):
        from tools.qa.visual_lint import stats
        im=Image.new("RGBA",(3,2),(0,0,0,0))
        for p in [(0,0),(1,0),(2,1)]:im.putpixel(p,(255,0,0,255))
        m=stats(im)
        self.assertEqual(m["visible_pixels"],3)
        self.assertEqual(m["palette_colors"],1)
        self.assertEqual(m["clusters_4_connected"],2)
        self.assertEqual(m["largest_cluster_area"],2)

    def test_checkerboard_and_tiling(self):
        from tools.qa.visual_lint import inspect
        im=Image.new("RGBA",(16,16))
        for y in range(16):
            for x in range(16):im.putpixel((x,y),(255,255,255,255) if (x+y)%2 else (0,0,0,255))
        name="assets/test/textures/block/test.png"
        with tempfile.TemporaryDirectory() as d:
            root=Path(d)
            r=inspect([name],{name:self.png(im)}.get,root)
            self.assertEqual(r["status"],"review")
            self.assertEqual(r["textures"][0]["metrics"]["checkerboard_quad_fraction"],1)
            with Image.open(root/"previews/001/tiling-3x3.png") as wall:self.assertEqual(wall.size,(48,48))

    def test_broken_png_fails_and_identical_ports_deduplicate(self):
        from tools.qa.visual_lint import inspect
        a="a/assets/test/textures/item/x.png";b="b/assets/test/textures/item/x.png"
        bad="assets/test/textures/item/bad.png"
        im=self.png(Image.new("RGBA",(16,16),(40,60,80,255)))
        with tempfile.TemporaryDirectory() as d:
            r=inspect([a,b,bad],{a:im,b:im,bad:b"broken"}.get,Path(d))
            self.assertEqual(r["counts"]["errors"],1)
            self.assertEqual(r["counts"]["unique_textures"],1)
            self.assertEqual(r["counts"]["textures"],2)

    def test_uv_stretch_advisory_invalid_uv_error(self):
        from tools.qa.visual_lint import model_metrics
        name="assets/test/models/block/x.json"
        model={"textures":{"a":"test:block/x"},"elements":[{"from":[0,0,0],"to":[16,16,16],"faces":{"north":{"texture":"#a","uv":[0,0,8,16]}}}]}
        tex=self.png(Image.new("RGBA",(16,16)))
        read={"assets/test/textures/block/x.png":tex}.get
        r=model_metrics(json.dumps(model).encode(),name,read)
        self.assertEqual(r["estimated_stretched_faces"],1)
        self.assertEqual(r["errors"],[])
        model["elements"][0]["faces"]["north"]["uv"]=[0,1]
        self.assertTrue(model_metrics(json.dumps(model).encode(),name,read)["errors"])

    def test_reference_ranges_only_matching_size(self):
        from tools.qa.visual_lint import stats,reference_ranges
        m=stats(Image.new("RGBA",(16,16),(10,20,30,255)))
        ref={"samples":[{"kind":"item","metrics":m}]}
        self.assertEqual(reference_ranges(ref,"item",(16,16))["ranges"]["palette_colors"],[1,1])
        self.assertIsNone(reference_ranges(ref,"item",(32,32)))
