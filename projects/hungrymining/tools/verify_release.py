"""Verify the Hungry Mining release contains the actual gameplay logic."""
from pathlib import Path
from sys import argv
from zipfile import ZipFile

jar = Path(argv[1])
required = {
    "dev/hungrymining/HungryMining.class",
    "dev/hungrymining/gameplay/HungerMiningSpeed.class",
    "META-INF/neoforge.mods.toml",
    "pack.mcmeta",
}

with ZipFile(jar) as archive:
    names = set(archive.namelist())
    missing = required - names
    if missing:
        raise SystemExit(f"Hungry Mining JAR missing: {sorted(missing)}")

print(f"Hungry Mining classes verified in {jar.name}")
