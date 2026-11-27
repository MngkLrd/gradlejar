import random
import unittest
from collections import Counter
try:
    from PIL import Image
except ImportError:
    Image=None
if Image is not None:
    from tools.qa.vanilla_benchmark import mutate, visible_hist, pixels, score

@unittest.skipIf(Image is None,"Pillow installed by standalone research workflow")
class VanillaBenchmarkTests(unittest.TestCase):
    def fixture(self):
        im=Image.new("RGBA",(4,4));im.putdata([(90,80,70,255)]*5+[(20,10,5,255)]*5+[(160,150,140,255)]*3+[(0,0,0,0)]*3)
        return im
    def test_shuffle_preserves_histogram_and_mask(self):
        im=self.fixture();out=mutate(im,"shuffle_visible",random.Random(206))
        self.assertIsNotNone(out)
        self.assertEqual(visible_hist(im),visible_hist(out))
        self.assertEqual([p[3] for p in pixels(im)],[p[3] for p in pixels(out)])
        self.assertNotEqual(pixels(im),pixels(out))
    def test_alpha_shuffle_and_flatten(self):
        im=self.fixture();out=mutate(im,"shuffle_alpha",random.Random(206))
        self.assertEqual(Counter(pixels(im)),Counter(pixels(out)))
        self.assertLess(len(visible_hist(mutate(im,"flatten_ramp",random.Random(206)))),len(visible_hist(im)))
    def test_unsure_is_not_pass_and_missing_is_error(self):
        key=[{"id":"a","family":"bark","source_side":"A"},{"id":"b","family":"bark","source_side":"B"},{"id":"c","family":"ore","source_side":"A"}]
        r=score(key,{"a":"A","b":"A","c":"UNSURE"})
        self.assertEqual(r["total"],{"total":3,"correct":1,"wrong":1,"unsure":1})
        self.assertAlmostEqual(r["source_choice_accuracy"],1/3)
        with self.assertRaises(ValueError):score(key,{"a":"A"})
