"""Static verification for a ModDevGradle-built FieldKit release JAR."""
import json
import struct
import sys
import zipfile
from pathlib import Path


def method_refs(data):
    assert data[:4] == b"\xca\xfe\xba\xbe", "Not a Java class"
    entries = [None]
    count = struct.unpack_from(">H", data, 8)[0]
    pos = 10
    while len(entries) < count:
        tag = data[pos]
        pos += 1
        if tag == 1:
            n = struct.unpack_from(">H", data, pos)[0]
            pos += 2
            entries.append((tag, data[pos:pos + n].decode("utf-8")))
            pos += n
        elif tag in (3, 4):
            entries.append((tag, None))
            pos += 4
        elif tag in (5, 6):
            entries.extend(((tag, None), None))
            pos += 8
        elif tag in (7, 8, 16, 19, 20):
            entries.append((tag, struct.unpack_from(">H", data, pos)[0]))
            pos += 2
        elif tag in (9, 10, 11, 12, 17, 18):
            entries.append((tag, *struct.unpack_from(">HH", data, pos)))
            pos += 4
        elif tag == 15:
            entries.append((tag, *struct.unpack_from(">BH", data, pos)))
            pos += 3
        else:
            raise AssertionError(f"Invalid constant pool tag {tag}")

    for rec in entries:
        if rec is not None and rec[0] in (10, 11):
            owner = entries[entries[rec[1]][1]][1]
            nt = entries[rec[2]]
            yield rec[0], owner, entries[nt[1]][1], entries[nt[2]][1]


def verify(path):
    with zipfile.ZipFile(path) as z:
        assert z.testzip() is None, "Corrupted JAR"
        names = set(z.namelist())
        assert "META-INF/neoforge.mods.toml" in names, "NeoForge metadata missing"
        assert "pack.mcmeta" in names, "pack.mcmeta missing"
        assert "dev/fieldkit/client/HudOverlay.class" in names, "HUD class missing"
        assert not any(n.startswith(("net/minecraft/", "net/neoforged/")) for n in names), "Bundled fake MC classes"
        metadata = z.read("META-INF/neoforge.mods.toml").decode()
        assert 'modId="fieldkit"' in metadata and 'version="1.2.2"' in metadata, "Wrong mod metadata"
        assert json.loads(z.read("pack.mcmeta"))["pack"]["pack_format"] == 34
        found_translatable = False
        for name in names:
            if name.startswith("assets/fieldkit/lang/") and name.endswith(".json"):
                json.loads(z.read(name))
            if not name.endswith(".class"):
                continue
            for tag, owner, method, desc in method_refs(z.read(name)):
                if owner == "net/minecraft/network/chat/Component" and method in ("translatable", "literal"):
                    assert tag == 11, f"{name}: Component.{method} not InterfaceMethodref"
                    found_translatable |= method == "translatable"
                assert not (method.startswith("m_") and method.endswith("_")), f"{name}: unexpected manual SRG mapping"
        assert found_translatable, "No translatable Component call was found"
    print(f"PASS: {path}")


if __name__ == "__main__":
    if len(sys.argv) != 2:
        sys.exit("Usage: verify_release.py JAR")
    try:
        verify(Path(sys.argv[1]))
    except (AssertionError, OSError, ValueError, zipfile.BadZipFile) as exc:
        sys.exit(f"FAIL: {exc}")
