"""Static test for release packaging. Executed before Gradle for new mod projects."""
import json
import sys
from pathlib import Path
p = Path(sys.argv[1])
d = json.loads((p / "build-profile.json").read_text())
assert d["slug"] == "keepsigntext"
assert (p / "src/main/java/com/kirill/keepsigntext/KeepSignText.java").is_file()
assert (p / "src/main/resources/pack.mcmeta").is_file()
assert (p / "src/main/resources" / ("fabric.mod.json" if d["loader"] == "fabric" else "META-INF/mods.toml" if d["minecraft"] == "1.20.1" else "META-INF/neoforge.mods.toml")).is_file()
print("Keep Sign Text structure validated:", d["minecraft"], d["loader"])
