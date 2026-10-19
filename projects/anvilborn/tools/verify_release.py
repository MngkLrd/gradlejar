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
print("PASS: Anvilborn classes, GUI, and all 50 localized aspects present")
