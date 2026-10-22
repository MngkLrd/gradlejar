"""Apply exact Pocket Realms 0.5.1 binary/audio patch in the CI workspace."""
from pathlib import Path
import base64
import hashlib
import io
import lzma
import tarfile

root = Path(__file__).resolve().parents[1] / "projects" / "pocketrealms"
encoded = (root / "upgrade-v051.b64").read_text(encoding="ascii")
packed = base64.b64decode(encoded, validate=True)
digest = hashlib.sha256(packed).hexdigest()
EXPECTED = "b442d1ee9fff5087e5ffc90019dd219c8adf985b73f65b7e16a84c458ecfff54"
if digest != EXPECTED:
    raise SystemExit(f"Pocket Realms 0.5.1 bundle checksum mismatch: {digest}")

with tarfile.open(fileobj=io.BytesIO(lzma.decompress(packed)), mode="r:") as archive:
    for member in archive.getmembers():
        name = Path(member.name)
        if name.is_absolute() or ".." in name.parts or not member.isfile():
            raise SystemExit(f"Unsafe source archive member: {member.name}")
        target = root / name
        target.parent.mkdir(parents=True, exist_ok=True)
        with archive.extractfile(member) as src:
            target.write_bytes(src.read())

print("Pocket Realms 0.5.1 Ogg Vorbis sounds and Java patch restored and checksum verified")
