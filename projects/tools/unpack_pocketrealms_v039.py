"""Restore verified 0.3.9 Pocket Realms overlay on top of the original mod sources."""
from pathlib import Path
import base64, hashlib, io, lzma, tarfile
root=Path(__file__).resolve().parents[1] / "projects" / "pocketrealms"
parts=[root / f"upgrade-v039-{n:02d}.b64" for n in range(8)]
encoded="".join(path.read_text("ascii") for path in parts)
data=base64.b64decode(encoded,validate=True)
actual=hashlib.sha256(data).hexdigest()
expected="8a33cedc35f4f10409f19869576c16da4a7eaa2e4263d2a245919b20d19c02c3"
if actual != expected:
    raise SystemExit(f"Corrupted Pocket Realms source transfer: {actual}")
with tarfile.open(fileobj=io.BytesIO(lzma.decompress(data)),mode="r:") as t:
    for member in t.getmembers():
        path=Path(member.name)
        if not member.isfile() or path.is_absolute() or ".." in path.parts:
            raise SystemExit(f"Invalid archive entry {member.name}")
        target=root/path
        target.parent.mkdir(parents=True,exist_ok=True)
        with t.extractfile(member) as source:
            target.write_bytes(source.read())
print("Pocket Realms 0.3.9 overlay verified and restored")
