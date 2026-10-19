"""Check the real compiled JAR and optionally run a project's deeper bytecode validator."""
import hashlib
import json
import os
import subprocess
import sys
import zipfile
from pathlib import Path

REPO = Path(__file__).resolve().parents[1]
MOD_META = {
    "neoforge": "META-INF/neoforge.mods.toml",
    "forge": "META-INF/mods.toml",
    "fabric": "fabric.mod.json",
}


def verify(relative):
    base = (REPO / relative).resolve()
    if not base.is_relative_to(REPO):
        raise ValueError("Project escapes repository")
    profile = json.loads((base / "build-profile.json").read_text("utf-8"))
    folder = base / "build" / "libs"
    candidates = [
        p for p in folder.glob(profile["jar_glob"])
        if p.is_file() and not p.name.endswith(("-sources.jar", "-javadoc.jar", "-dev.jar", "-api.jar"))
    ]
    if len(candidates) != 1:
        raise ValueError(f"Expected exactly one release JAR, found {len(candidates)}: {candidates}")
    jar = candidates[0]
    with zipfile.ZipFile(jar) as z:
        if z.testzip():
            raise ValueError("Corrupt JAR ZIP entry")
        names = set(z.namelist())
        meta = MOD_META[profile["loader"]]
        if meta not in names:
            raise ValueError(f"Missing {meta}")
        if "pack.mcmeta" not in names:
            raise ValueError("Missing pack.mcmeta")
        if not any(n.endswith(".class") for n in names):
            raise ValueError("No compiled Java classes")
        if any(n.startswith(("net/minecraft/", "net/neoforged/", "net/fabricmc/")) for n in names):
            raise ValueError("JAR contains bundled Minecraft/loader classes")
    deeper = base / "tools" / "verify_release.py"
    if deeper.is_file():
        subprocess.run([sys.executable, str(deeper), str(jar)], check=True)
    digest = hashlib.sha256(jar.read_bytes()).hexdigest()
    checksum = jar.with_name(jar.name + ".sha256")
    checksum.write_text(f"{digest}  {jar.name}\n", encoding="utf-8")
    if output := os.getenv("GITHUB_OUTPUT"):
        with open(output, "a", encoding="utf-8") as fh:
            fh.write(f"jar_path={jar}\nchecksum_path={checksum}\n")
    print(f"PASS: {jar.name} ({jar.stat().st_size} bytes)")
    print(f"SHA-256: {digest}")


if __name__ == "__main__":
    try:
        verify(sys.argv[1] if len(sys.argv) > 1 else ".")
    except (ValueError, OSError, zipfile.BadZipFile, subprocess.CalledProcessError, KeyError) as exc:
        sys.exit(f"FAILED JAR verification: {exc}")
