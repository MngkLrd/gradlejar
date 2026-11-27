import unittest
try:
    from PIL import Image
except ImportError:
    Image=None

@unittest.skipIf(Image is None,"Pillow installed by visual workflow")
class ReviewQueueTests(unittest.TestCase):
    def test_deduplicate_identical_review_assets(self):
        from tools.qa.visual_lint import review_queue
        row={"sha256":"abc","kind":"item","file":"a.png","warnings":["review noise"],"metrics":{},"preview":None,"vanilla_context":None}
        result=review_queue({"textures":[row,dict(row,file="b.png"),dict(row,file="c.png",warnings=[])]})
        self.assertEqual(len(result["tasks"]),1)
        self.assertEqual(result["tasks"][0]["files"],["a.png","b.png"])
        self.assertEqual(result["tasks"][0]["status"],"REVIEW")
