import random
import unittest
from collections import Counter
try:
    from PIL import Image
except ImportError:
    Image=None
if Image is not None:
    from tools.qa.vanilla_benchmark import mutate, visible_hist, pixels, score, baseline_results

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

    def test_baselines_count_ties_missing_wrong_and_modes(self):
        cases=[
            {"mode":"flat","source_metrics":{"palette_colors":6},"perturbed_metrics":{"palette_colors":3}},
            {"mode":"shuffle","source_metrics":{"palette_colors":6},"perturbed_metrics":{"palette_colors":6}},
            {"mode":"other","source_metrics":{"palette_colors":4},"perturbed_metrics":{"palette_colors":5}},
            {"mode":"other","source_metrics":{},"perturbed_metrics":{"palette_colors":3}},
        ]
        rows=baseline_results(cases)
        self.assertEqual(rows["more_palette_colors"]["total"],{"correct":1,"wrong":1,"unsure":2,"total":4})
        self.assertEqual(rows["more_palette_colors"]["coverage"],.5)
        self.assertEqual(rows["more_palette_colors"]["source_choice_accuracy"],.25)
        self.assertEqual(rows["more_palette_colors"]["by_mode"]["shuffle"]["unsure"],1)
        self.assertEqual(rows["larger_component"]["total"]["unsure"],4)

    def test_expanded_pool_and_local_defect_controls(self):
        from tools.qa.vanilla_benchmark import EXPANDED, EXPANDED_METHODS
        sources=[n for refs in EXPANDED.values() for n in refs]
        self.assertEqual(len(EXPANDED),12)
        self.assertEqual(len(set(sources)),60)
        im=Image.new("RGBA",(8,8))
        im.putdata([(20+(i%5)*30,40,60,255) for i in range(64)])
        out=mutate(im,"local_flatten",random.Random(209))
        self.assertIsNotNone(out)
        self.assertEqual([p[3] for p in pixels(im)],[p[3] for p in pixels(out)])
        self.assertLessEqual(sum(a!=b for a,b in zip(pixels(im),pixels(out))),16)
        out=mutate(im,"repeat_quadrant",random.Random(209))
        self.assertIsNotNone(out)
        self.assertEqual(out.crop((0,0,4,4)).tobytes(),out.crop((4,4,8,8)).tobytes())
        self.assertIn("local_flatten",EXPANDED_METHODS)
        im.putpixel((0,0),(0,0,0,0))
        self.assertIsNone(mutate(im,"repeat_quadrant",random.Random(209)))
