"""Apply SHA-256 verified Pocket Realms 0.6.1 architectural patch with original UV PNG."""
from pathlib import Path
import base64,hashlib,io,tarfile,subprocess
root=Path(__file__).resolve().parents[1]/"projects"/"pocketrealms"
encoded="".join((root/f"upgrade-v061-{i:02d}.b64").read_text("ascii") for i in range(4))
raw=base64.b64decode(encoded,validate=True)
checksum=hashlib.sha256(raw).hexdigest()
if checksum!="9e5894802b783e5d37651ce0adc3e7bd118bf0215deba9f52e45a6688776be59":
    raise SystemExit("Pocket Realms 0.6.1 data transmission corruption: "+checksum)
with tarfile.open(fileobj=io.BytesIO(raw),mode="r:xz") as arc:
    patch=None
    for m in arc:
        p=Path(m.name)
        if not m.isfile() or p.is_absolute() or ".." in p.parts:raise SystemExit("Unsafe archive path "+m.name)
        with arc.extractfile(m) as fd:data=fd.read()
        if m.name=="update.patch":patch=data
        else:
            dest=root/p
            dest.parent.mkdir(parents=True,exist_ok=True)
            dest.write_bytes(data)
if patch is None:raise SystemExit("Missing source diff")
# README received additional 0.6.0 notes in the CI base; append new notes separately.
import re
diff_header=b"diff -Nru a/README.md b/README.md"
if patch.count(diff_header)!=1:raise SystemExit("Missing README hunk")
start=patch.index(diff_header)
next_file=patch.find(b"diff -Nru ",start+len(diff_header))
if next_file<0:raise SystemExit("README is not followed by other source hunks")
patch=patch[:start]+patch[next_file:]
with (root/"README.md").open("a",encoding="utf8") as f:
    f.write("\\n## 0.6.1 — Six architectural blocks\\n"
        "Crumbling Stone, Ritual Slab, Bone Wall, Rift Grate, Cracked Void Bricks and Broken Pillar. "
        "Correct shapes, creative items, room integration, loot, 32x32 UV PNG.\\n")
check=subprocess.run(["patch","--dry-run","--fuzz=0","-p1"],cwd=root,input=patch,stdout=subprocess.PIPE,stderr=subprocess.STDOUT)
if check.returncode:raise SystemExit("Source patch does not apply:\n"+check.stdout.decode())
run=subprocess.run(["patch","--fuzz=0","-p1"],cwd=root,input=patch,stdout=subprocess.PIPE,stderr=subprocess.STDOUT)
if run.returncode:raise SystemExit(run.stdout.decode())
print("Pocket Realms 0.6.1 verified: exact original six UV assets, blocks, registration and worldgen.")
