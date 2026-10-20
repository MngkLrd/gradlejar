"""Validate Anvilborn-specific invariants in the production release JAR."""
import json
import sys
from zipfile import ZipFile

jar = sys.argv[1]
with ZipFile(jar) as z:
    names = set(z.namelist())
    required = {
        "dev/anvilborn/Anvilborn.class",
        "dev/anvilborn/forge/ForgeRules.class",
        "dev/anvilborn/forge/ForgeMenu.class",
        "dev/anvilborn/forge/Aspects.class",
        "dev/anvilborn/client/ForgeScreen.class",
        "dev/anvilborn/client/ForgeStrikeButton.class",
        "assets/anvilborn/textures/gui/forge_screen.png",
        "assets/anvilborn/textures/gui/forge_widgets.png",
        "assets/anvilborn/lang/en_us.json",
        "assets/anvilborn/lang/ru_ru.json",
    }
    missing = required - names
    if missing:
        raise SystemExit(f"Missing Anvilborn class/resource: {sorted(missing)}")
    for lang in ("en_us", "ru_ru"):
        data = json.loads(z.read(f"assets/anvilborn/lang/{lang}.json"))
        aspect_keys = [k for k in data if k.startswith("aspect.anvilborn.")]
        if len(aspect_keys) != 50:
            raise SystemExit(f"{lang}: expected 50 named aspect entries; got {len(aspect_keys)}")
    for asset, dimensions in (
        ("forge_screen.png", (256, 256)),
        ("forge_widgets.png", (256, 128)),
    ):
        data = z.read(f"assets/anvilborn/textures/gui/{asset}")
        if data[:8] != b"\x89PNG\r\n\x1a\n":
            raise SystemExit(f"Invalid PNG signature: {asset}")
        import struct
        actual = struct.unpack(">II", data[16:24])
        if actual != dimensions:
            raise SystemExit(f"Wrong {asset} dimensions: {actual} instead of {dimensions}")
print("PASS: Anvilborn classes, textures and 50 localized aspects present")
