#!/usr/bin/env python3
"""Check WallSlam release JARs for the damage-type and loader metadata.

This catches packaging and format regressions in CI, not runtime mod conflicts.
"""
import json
import sys
import zipfile
from pathlib import Path


def require(value, message):
    if not value:
        raise SystemExit("WallSlam JAR verification failed: " + message)


root = Path(sys.argv[1])
profile = json.loads((root / "build-profile.json").read_text(encoding="utf-8"))
mc = profile["minecraft"]
jar_path = root / "build" / "libs" / profile["jar_glob"]
require(jar_path.is_file(), f"Missing JAR: {jar_path}")

with zipfile.ZipFile(jar_path) as jar:
    def load_json(path):
        require(path in jar.namelist(), f"Missing packaged resource: {path}")
        return json.loads(jar.read(path))

    damage = load_json("data/wallslam/damage_type/collision.json")
    require(damage.get("message_id") == "wallslam_collision", "Incorrect collision message ID")
    require(damage.get("scaling") == "never", "Incorrect collision scaling")
    require(isinstance(damage.get("exhaustion"), (float, int)), "Missing collision exhaustion")

    tag = load_json("data/minecraft/tags/damage_type/bypasses_cooldown.json")
    require("wallslam:collision" in tag.get("values", []), "Collision damage missing cooldown bypass tag")
    pack = load_json("pack.mcmeta")["pack"]

    if mc == "1.20.1":
        require(pack.get("pack_format") == 15, "Incorrect Forge 1.20.1 pack format")
        require("META-INF/mods.toml" in jar.namelist(), "Forge mods.toml missing")
        toml = jar.read("META-INF/mods.toml").decode("utf-8")
        require('versionRange="[47.1.0,)"' in toml, "Forge must not require the latest loader")
    elif mc == "1.21.1":
        require(pack.get("pack_format") == 34, "Incorrect 1.21.1 resource pack format")
        require(pack.get("supported_formats") == [34, 48], "1.21.1 data pack format 48 not supported")
    elif mc == "26.3":
        require(pack.get("min_format") == [97, 1], "Incorrect 26.3 resource pack minimum")
        require(pack.get("max_format") >= 121, "26.3 data pack format 121 not supported")
    else:
        raise SystemExit("Unexpected Minecraft version " + mc)

    classes = [n for n in jar.namelist() if n.endswith("/mechanics/ImpactTracker.class")]
    require(len(classes) == 1, "Missing ImpactTracker.class")
    bytecode = jar.read(classes[0])
    require(b"Custom wallslam:collision damage type not found" in bytecode,
            "Missing defensive damage-type lookup fallback")

print(f"WallSlam JAR OK: {jar_path} | loader={profile['loader']} | minecraft={mc}")
