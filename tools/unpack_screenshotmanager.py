"""Extract the versioned Screenshot Manager source snapshot into the build workspace.

This keeps the exact user-provided Java project in the CI build, without fake
Minecraft API stubs or rewritten bytecode.
"""
import base64
import io
import lzma
from pathlib import Path
import tarfile

ROOT = Path(__file__).resolve().parents[1]
PAYLOAD = ROOT / "tools/payloads/screenshotmanager-0.2.0.tar.xz.b64"
DESTINATION = ROOT / "projects/screenshotmanager"

def main():
    archive_bytes = lzma.decompress(base64.b64decode(PAYLOAD.read_text(encoding="ascii").strip(), validate=True))
    DESTINATION.mkdir(parents=True, exist_ok=True)
    with tarfile.open(fileobj=io.BytesIO(archive_bytes), mode="r:") as tar:
        for member in tar:
            if not member.isfile():
                continue
            out = (DESTINATION / member.name).resolve()
            if not out.is_relative_to(DESTINATION.resolve()):
                raise ValueError("Invalid payload path: " + member.name)
            out.parent.mkdir(parents=True, exist_ok=True)
            source = tar.extractfile(member)
            if source is None:
                raise ValueError("Missing archived contents for " + member.name)
            with source, out.open("wb") as target:
                target.write(source.read())
    print("Restored Screenshot Manager Java/resources/Gradle source files.")

if __name__ == "__main__":
    main()
