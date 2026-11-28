import io
import json
import unittest
import zipfile
try:
    from PIL import Image
except ImportError:
    Image=None
if Image is not None:
    from tools.qa.vanilla_extended import animation_plan, analyze_animation, extended_inventory, transition
    from tools.qa.vanilla_corpus import inventory

@unittest.skipIf(Image is None,"Pillow installed by standalone research workflow")
class VanillaExtendedTests(unittest.TestCase):
    def fixture(self):
        im=Image.new("RGBA",(2,6))
        for y in range(2): 
            for x in range(2):im.putpixel((x,y),(120,80,40,255))
        for y in range(2,4):
            for x in range(2):im.putpixel((x,y),(90,60,30,255))
        return im
    def test_animation_sequence_repeat_duration_and_loop(self):
        r=analyze_animation(self.fixture(),{"frametime":2,"frames":[0,{"index":1,"time":5},0,2],"interpolate":True})
        self.assertEqual((r["available_frames"],r["used_frames"],r["cycle_ticks"]),(3,3,11))
        self.assertEqual(r["sequence_length"],4)
        self.assertTrue(r["interpolate"])
        self.assertTrue(any(x["from"]==2 and x["to"]==0 and x["mask_changed_fraction"]==1 for x in r["transitions"]))
    def test_invalid_and_unsupported_layout(self):
        im=self.fixture()
        for meta in ({"frames":[3]},{"frametime":0},{"width":3,"height":2},{"frames":[True]},{"interpolate":1}):
            with self.assertRaises(ValueError):animation_plan(im,meta)
        with self.assertRaises(NotImplementedError):animation_plan(im,{"width":2})
        self.assertEqual(animation_plan(im,{"width":1,"height":2})[2],6)
    def test_hidden_rgb_is_not_motion(self):
        a=Image.new("RGBA",(2,2),(255,0,0,0));b=Image.new("RGBA",(2,2),(0,255,0,0))
        self.assertEqual(transition(a,b)["changed_fraction"],0)
    def test_deferred_metadata_gui_and_large_not_sprite_aggregate(self):
        buf=io.BytesIO()
        with zipfile.ZipFile(buf,"w") as z:
            for path,im,meta in [
                ("block/fire.png",self.fixture(),{"animation":{}}),
                ("gui/sprites/panel.png",self.fixture(),{"gui":{"scaling":{"type":"nine_slice","border":1,"width":2,"height":6}}}),
                ("entity/large.png",Image.new("RGBA",(256,128)),None),
                ("block/plain.png",Image.new("RGBA",(16,16)),None)]:
                p="assets/minecraft/textures/"+path
                b=io.BytesIO();im.save(b,format="PNG");z.writestr(p,b.getvalue())
                if meta is not None:z.writestr(p+".mcmeta",json.dumps(meta))
        with zipfile.ZipFile(buf) as z:r=extended_inventory(z,inventory(z))
        self.assertEqual(r["deferred_count"],3)
        self.assertEqual(r["statuses"],{"ANIMATION_FRAMES_MEASURED":1,"LARGE_IMAGE_STRUCTURE_MEASURED":1,"NON_ANIMATION_METADATA_INSPECTED":1})
        self.assertEqual(r["animation_used_frames"],3)
        self.assertTrue(all(x["visual_review"]=="NOT_RUN" for x in r["assets"]))
        self.assertEqual(r["role_coverage"]["gui_sprite"],{"METADATA_REVIEW_REQUIRED":1})
