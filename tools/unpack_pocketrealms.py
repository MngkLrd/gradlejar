"""One-time import of Pocket Realms sources into the build job."""
from pathlib import Path
import base64
import hashlib
import io
import lzma
import tarfile

ROOT = Path(__file__).resolve().parents[1]
PROJECT = ROOT / "projects" / "pocketrealms"
ENTRYPOINT = PROJECT / "src/main/java/com/kirill/pocketrealms/PocketRealms.java"
SHA256 = "9c33c5645a4043cebb4a5b0fc95ed03c51419681d4a9e08d51370084cb61e849"

if ENTRYPOINT.exists():
    print("Pocket Realms sources already checked in; skipping bootstrap bundle")
else:
    parts = [PROJECT / f"source-part-{i:02d}.b64" for i in range(5)]
    if not all(p.is_file() for p in parts):
        raise SystemExit("Missing Pocket Realms source part")
    data = base64.b64decode("".join(p.read_text("ascii") for p in parts), validate=True)
    digest = hashlib.sha256(data).hexdigest()
    if digest != SHA256:
        raise SystemExit(f"Pocket Realms source bundle checksum mismatch: {digest}")
    with tarfile.open(fileobj=io.BytesIO(lzma.decompress(data)), mode="r:") as archive:
        for member in archive.getmembers():
            member_path = Path(member.name)
            if member_path.is_absolute() or ".." in member_path.parts or not member.isfile():
                raise SystemExit(f"Unsafe archive member: {member.name}")
            output = PROJECT / member_path
            output.parent.mkdir(parents=True, exist_ok=True)
            if output.is_file():
                continue  # Allow separately committed CI fixes to override the base bundle
            with archive.extractfile(member) as fh:
                output.write_bytes(fh.read())
    print(f"Restored Pocket Realms source project; sha256={digest}")
