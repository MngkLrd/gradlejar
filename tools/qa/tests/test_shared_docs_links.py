"""Check internal inline links in the shared MineMods reference and research documents."""
import re
import subprocess
import unittest
from pathlib import Path, PurePosixPath
from urllib.parse import unquote, urlsplit
import posixpath

ROOT = Path(__file__).resolve().parents[3]
DOCS = (
    "AGENTS.md", "README.md", "docs/QA-V2.md", "docs/QA-ASSIGNMENT-TEMPLATE.md",
    "docs/BOT-ACTIONS.md", "docs/RECIPE-QA.md", "docs/VISUAL-QA.md",
    "docs/VANILLA-ART-ATLAS.md", "docs/VANILLA-RESEARCH.md", "docs/VANILLA-REAL-ART-REVIEW.md",
    "docs/MOD-NAMING.md", "docs/MOD-ADAPTATION.md", "docs/ITEM-ARMOR-ART.md",
    "docs/qa/QA-V2.md",
    "docs/qa/QA-ASSIGNMENT-TEMPLATE.md",
    "docs/qa/BOT-ACTIONS.md",
    "docs/qa/RECIPE-QA.md",
    "docs/art/VISUAL-QA.md",
    "docs/art/ITEM-ARMOR-ART.md",
    "docs/art/VANILLA-ART-ATLAS.md",
    "docs/research/VANILLA-RESEARCH.md",
    "docs/research/VANILLA-REAL-ART-REVIEW.md",
    "docs/development/MOD-NAMING.md",
    "docs/development/MOD-ADAPTATION.md",
    "docs/development/ECOSYSTEM-INTEGRATION.md",
    "docs/README.md",
    "docs/art/BLOCK-ART.md",
    "docs/art/STRUCTURE-QA.md",
    "docs/art/UI-ART.md",
    "docs/art/MOB-ART.md",
)
INLINE = re.compile(r"!?\[[^\]\n]*\]\((?:<([^>\n]+)>|([^\s)\n]+))(?:[ \t]+[\"'][^)\n]*[\"'])?\)")
HEADING = re.compile(r"^ {0,3}#{1,6}\s+(.+?)\s*#*\s*$")
FENCE = re.compile(r"^ {0,3}(\x60{3,}|~{3,})")

def visible_lines(content):
    fence = None
    for number, line in enumerate(content.splitlines(), 1):
        m = FENCE.match(line)
        if m:
            marker = m.group(1)
            if fence is None:
                fence = marker
            elif marker[0] == fence[0] and len(marker) >= len(fence):
                fence = None
            continue
        if fence is None:
            yield number, line

def anchors(content):
    seen = set()
    for _, line in visible_lines(content):
        m = HEADING.match(line)
        if not m:
            continue
        title = m.group(1).lower()
        base = re.sub(r"[^\w\s-]", "", title).replace(" ", "-")
        candidate = base
        n = 1
        while candidate in seen:
            candidate = f"{base}-{n}"
            n += 1
        seen.add(candidate)
    return seen

def inspect(root, tracked, docs=DOCS):
    errors = []
    for doc in docs:
        if doc not in tracked or not (root/doc).is_file():
            errors.append(f"{doc}: required reference missing")
            continue
        for line_num, line in visible_lines((root/doc).read_text(encoding="utf-8")):
            # Examples in inline code are not live Markdown links.
            line = re.sub(r"\x60+[^\x60]*\x60+", "", line)
            for match in INLINE.finditer(line):
                link = match.group(1) or match.group(2)
                u = urlsplit(link)
                if u.scheme or u.netloc:
                    continue
                path = unquote(u.path)
                fragment = unquote(u.fragment)
                if path.startswith("/"):
                    errors.append(f"{doc}:{line_num}: use a repo-relative or absolute HTTPS link: {link}")
                    continue
                target = posixpath.normpath(posixpath.join(posixpath.dirname(doc), path)) if path else doc
                if target not in tracked or not (root/target).is_file():
                    errors.append(f"{doc}:{line_num}: missing tracked file: {target}")
                    continue
                if fragment and PurePosixPath(target).suffix.lower() == ".md":
                    if fragment not in anchors((root/target).read_text(encoding="utf-8")):
                        errors.append(f"{doc}:{line_num}: missing heading: {target}#{fragment}")
    return errors

class SharedDocsLinks(unittest.TestCase):
    def test_shared_reference_links(self):
        tracked = set(subprocess.check_output(["git","ls-files","-z"],cwd=ROOT,text=True).split("\0"))
        self.assertEqual(inspect(ROOT,tracked), [])

    def test_canonical_topic_docs_and_legacy_pointers(self):
        for old,new in [["docs/QA-V2.md","docs/qa/QA-V2.md"],["docs/QA-ASSIGNMENT-TEMPLATE.md","docs/qa/QA-ASSIGNMENT-TEMPLATE.md"],["docs/BOT-ACTIONS.md","docs/qa/BOT-ACTIONS.md"],["docs/RECIPE-QA.md","docs/qa/RECIPE-QA.md"],["docs/VISUAL-QA.md","docs/art/VISUAL-QA.md"],["docs/ITEM-ARMOR-ART.md","docs/art/ITEM-ARMOR-ART.md"],["docs/VANILLA-ART-ATLAS.md","docs/art/VANILLA-ART-ATLAS.md"],["docs/VANILLA-RESEARCH.md","docs/research/VANILLA-RESEARCH.md"],["docs/VANILLA-REAL-ART-REVIEW.md","docs/research/VANILLA-REAL-ART-REVIEW.md"],["docs/MOD-NAMING.md","docs/development/MOD-NAMING.md"],["docs/MOD-ADAPTATION.md","docs/development/MOD-ADAPTATION.md"]]:
            self.assertIn(old,DOCS)
            self.assertIn(new,DOCS)
            text=(ROOT/old).read_text(encoding="utf-8")
            self.assertIn("Canonical content moved",text)
            self.assertIn(new,text)
            self.assertGreater((ROOT/new).stat().st_size,0)
        self.assertIn("docs/README.md",DOCS)

    def test_missing_file_wrong_case_and_heading(self):
        import tempfile
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp)
            (root/"AGENTS.md").write_text(
                "[ok](VISUAL.md#visual-qa)\n[case](visual.md)\n"
                "[missing](fashions.md)\n[heading](VISUAL.md#wrong)\n"
                "\x60\x60\x60md\n[example](not-real.md)\n\x60\x60\x60\n"
                "\x60[example](not-real.md)\x60\n", encoding="utf-8")
            (root/"VISUAL.md").write_text("# Visual QA\n",encoding="utf-8")
            errors = inspect(root,{"AGENTS.md","VISUAL.md"},("AGENTS.md",))
            self.assertEqual(len(errors),3)
            self.assertTrue(any("fashions.md" in e for e in errors))
            self.assertTrue(any("visual.md" in e for e in errors))
            self.assertTrue(any("#wrong" in e for e in errors))

if __name__ == "__main__":
    unittest.main()
