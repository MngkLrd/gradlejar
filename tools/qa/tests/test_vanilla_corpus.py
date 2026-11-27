import io
import unittest
import zipfile
from PIL import Image
from tools.qa.vanilla_corpus import extra_metrics, inventory, summarize, git_blob

class VanillaCorpusTests(unittest.TestCase):
    def png(self,colors):
        im=Image.new("RGBA",(2,2));im.putdata(colors)
        b=io.BytesIO();im.save(b,format="PNG");return b.getvalue()
    def test_exact_layout_is_color_invariant_not_alpha_invariant(self):
        a=Image.new("RGBA",(2,2));a.putdata([(1,2,3,255),(1,2,3,255),(4,5,6,255),(0,0,0,0)])
        b=Image.new("RGBA",(2,2));b.putdata([(9,9,9,255),(9,9,9,255),(8,8,8,255),(255,255,255,0)])
        self.assertEqual(extra_metrics(a)["layout_sha256"],extra_metrics(b)["layout_sha256"])
        b.putpixel((1,1),(8,8,8,255))
        self.assertNotEqual(extra_metrics(a)["layout_sha256"],extra_metrics(b)["layout_sha256"])
    def test_all_pngs_inventoried_even_metadata(self):
        data=self.png([(10,10,10,255)]*4)
        buf=io.BytesIO()
        with zipfile.ZipFile(buf,"w") as z:
            for name in ("block/oak_log","block/spruce_log","block/water","gui/x"):
                z.writestr("assets/minecraft/textures/"+name+".png",data)
            z.writestr("assets/minecraft/textures/block/water.png.mcmeta","{}")
            z.writestr("assets/other/textures/x.png",data)
        with zipfile.ZipFile(buf) as z:rows=inventory(z)
        s=summarize(rows)
        self.assertEqual(len(rows),4)
        self.assertEqual(s["statuses"],{"MEASURED":3,"METADATA_REVIEW_REQUIRED":1})
        self.assertEqual(len(s["shared_color_layouts"][0]),3)
        self.assertTrue(all(r["visual_review"]=="NOT_RUN" for r in rows))
        self.assertTrue(all(r["git_blob_sha1"]==git_blob(data) for r in rows))
