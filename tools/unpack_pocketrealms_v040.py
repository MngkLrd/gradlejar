"""Apply the verified Pocket Realms 0.4.0 gameplay patch after legacy overlays."""
from pathlib import Path
import base64,hashlib,io,lzma,tarfile
root=Path(__file__).resolve().parents[1] / "projects"/"pocketrealms"
parts=[root/f"upgrade-v040-{n:02d}.b64" for n in range(6)]
raw=base64.b64decode("".join(p.read_text("ascii") for p in parts),validate=True)
checksum=hashlib.sha256(raw).hexdigest()
expected="115b01ec59147cbd4cf42c06696ab9a912646a0235c97705e51a506d63dbd4cd"
if checksum!=expected:
    raise SystemExit("Pocket Realms source transfer is corrupt: "+checksum)
with tarfile.open(fileobj=io.BytesIO(lzma.decompress(raw)),mode="r:") as archive:
    for member in archive.getmembers():
        name=Path(member.name)
        if not member.isfile() or name.is_absolute() or ".." in name.parts:
            raise SystemExit(f"Unsafe source member: {name}")
        path=root/name
        path.parent.mkdir(parents=True,exist_ok=True)
        with archive.extractfile(member) as source:
            path.write_bytes(source.read())
print("Pocket Realms 0.4.0 staged; checksum",checksum)
