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

    def test_review_all_assets_including_clean_and_models(self):
        from tools.qa.visual_lint import art_review_inventory
        clean={"sha256":"abc","kind":"item","file":"a.png","warnings":[]}
        flagged={"sha256":"def","kind":"item","file":"b.png","warnings":["flat"]}
        model={"sha256":"ghi","file":"model.json","warnings":[]}
        result=art_review_inventory({"textures":[clean,dict(clean,file="port.png"),flagged],"models":[model]})
        self.assertEqual(len(result["tasks"]),3)
        self.assertEqual(result["tasks"][0]["priority"],"FLAGGED")
        self.assertTrue(all(t["status"]=="NOT_RUN" for t in result["tasks"]))
        self.assertEqual(next(t for t in result["tasks"] if t["sha256"]=="abc")["files"],["a.png","port.png"])
