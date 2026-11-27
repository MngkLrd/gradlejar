import io
import json
import tempfile
import unittest
from pathlib import Path
try:
    from PIL import Image
except ImportError:
    Image=None
if Image is not None:
    from tools.qa.real_art_review import build,evaluate,CRITERIA,write_json
    from tools.qa.vanilla_corpus import git_blob

@unittest.skipIf(Image is None,"Pillow installed by standalone research workflow")
class RealArtReviewTests(unittest.TestCase):
    def png(self,color):
        b=io.BytesIO();Image.new("RGBA",(16,16),color).save(b,format="PNG");return b.getvalue()
    def fixture(self,out):
        data={"old":self.png((20,30,40,255)),"new":self.png((40,60,80,255))}
        manifest={"minecraft":"1.21.1","repository":"fixture","cases":[{"id":"test","family":"soil","intent":"soil",
            "references":["block/dirt"],"candidates":[{"revision":n,"blob_sha":git_blob(v)} for n,v in [("old",data["old"]),("new",data["new"])] ]}]}
        manifest["cases"][0]["candidates"][0]["revision"]="earlier"
        manifest["cases"][0]["candidates"][1]["revision"]="later"
        summary=build(manifest,lambda repo,c:data["old" if c["revision"]=="earlier" else "new"],lambda _:data["old"],out,7)
        self.assertEqual(summary["candidate_images"],2)
        return out/"reviewer/deck.json",out/"private/key.json",out/"reviewer/review-template.json"
    def test_blind_packet_and_no_fabricated_accuracy(self):
        with tempfile.TemporaryDirectory() as t:
            deck,key,response=self.fixture(Path(t)/"packet")
            d=json.loads(deck.read_text());self.assertNotIn("revision",deck.read_text())
            self.assertNotIn("blob_sha",deck.read_text())
            self.assertTrue((deck.parent/d["cases"][0]["sheet"]).is_file())
            row=json.loads(response.read_text());row.update(reviewer="fresh reviewer",review_context="fixture",independence_declared=True)
            row["cases"][0]["preference"]="A"
            for a in row["cases"][0]["images"]:
                a.update(verdict="ACCEPT",reference_sha256=[x["sha256"] for x in d["cases"][0]["references"]],
                         findings={k:"Fixture observation, not real art." for k in CRITERIA})
            write_json(response,row)
            result=evaluate(deck,key,response)
            self.assertEqual(result["owner_labeled_images"],0)
            self.assertEqual(result["unlabeled_images"],2)
            self.assertEqual(result["owner_label_confusion"],{})
            self.assertEqual(sum(result["revision_preferences"].values()),1)
            row["cases"][0]["images"][0]["reference_sha256"]=[]
            write_json(response,row)
            with self.assertRaises(ValueError):evaluate(deck,key,response)
    def test_bad_source_hash_rejected(self):
        with tempfile.TemporaryDirectory() as t:
            m={"minecraft":"1.21.1","repository":"fixture","cases":[{"id":"x","family":"item","intent":"x","references":[],
                "candidates":[{"blob_sha":"0"*40,"revision":"snapshot"}]}]}
            with self.assertRaises(ValueError):
                build(m,lambda *a:self.png((1,2,3,255)),lambda x:b"",Path(t)/"out",1)
