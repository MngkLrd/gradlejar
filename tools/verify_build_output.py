"""Check the real compiled JAR and optionally run a project's deeper bytecode validator."""
import hashlib
import json
import tomllib
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
        meta = ("META-INF/mods.toml" if profile["loader"] == "neoforge"
                and profile["minecraft"] == "1.20.1"
                else MOD_META[profile["loader"]])
        if meta not in names:
            raise ValueError(f"Missing {meta}")
        if profile["loader"] in ("neoforge", "forge"):
            manifest = tomllib.loads(z.read(meta).decode("utf-8-sig"))
            if manifest.get("license") is None:
                raise ValueError(f"{meta}: missing license")
            mods = manifest.get("mods")
            if not isinstance(mods, list) or len(mods) != 1:
                raise ValueError(f"{meta}: expected exactly one [[mods]]")
            info = mods[0]
            if info.get("modId") != profile["slug"]:
                raise ValueError(f"{meta}: wrong modId {info.get('modId')!r}")
            if info.get("version") != profile["version"]:
                raise ValueError(f"{meta}: wrong version {info.get('version')!r}")
            mc = profile["minecraft"]
            # On NeoForge/Forge up to 1.21.4 the two root keys are mandatory.
            # NeoForge 1.21.5+ defaults to javafml and its supported loader range.
            legacy_loader_schema = (
                mc == "1.20.1"
                or (mc.startswith("1.") and
                    tuple(int(v) for v in mc.split(".")[1:]) < (21, 5))
            )
            if legacy_loader_schema:
                if manifest.get("modLoader") != "javafml":
                    raise ValueError(f"{meta}: Missing ModLoader: modLoader='javafml' is required")
                version_range = manifest.get("loaderVersion")
                if not isinstance(version_range, str) or not version_range.strip():
                    raise ValueError(f"{meta}: Missing loaderVersion")
            deps = manifest.get("dependencies", {})
            required = ("forge" if mc == "1.20.1" else "neoforge")
            if not isinstance(deps, dict) or info["modId"] not in deps:
                raise ValueError(f"{meta}: missing mod dependencies")
            dep_ids = {d.get("modId") for d in deps[info["modId"]]}
            if not {required, "minecraft"}.issubset(dep_ids):
                raise ValueError(f"{meta}: expected dependencies on {required} and minecraft")
            # Optional compatibility contract for profiles that declare a loader floor.
            # This prevents accidentally shipping a JAR pinned to the latest build.
            minimum = profile.get("minimum_loader_version")
            compiled = profile.get("compile_loader_version")
            if minimum:
                loader_deps = [d for d in deps[info["modId"]] if d.get("modId") == required]
                if len(loader_deps) != 1:
                    raise ValueError(f"{meta}: expected one {required} dependency")
                declared = loader_deps[0].get("versionRange")
                if declared != f"[{minimum},)":
                    raise ValueError(
                        f"{meta}: loader min {declared!r} must be "
                        f"[{minimum},) according to build-profile.json"
                    )
                if compiled:
                    def numeric_version(version):
                        parts = version.split(".")
                        if not all(p.isdigit() for p in parts):
                            raise ValueError(f"Non-numeric loader version {version!r}")
                        return tuple(map(int, parts))
                    if numeric_version(compiled) < numeric_version(minimum):
                        raise ValueError(
                            f"Compile loader {compiled} must not be older than minimum {minimum}"
                        )
                    props = (base / "gradle.properties").read_text("utf-8").splitlines()
                    key = "forge_version" if required == "forge" else "neo_version"
                    vals = [line.split("=", 1)[1].strip() for line in props
                            if line.strip().startswith(key + "=")]
                    if vals != [compiled]:
                        raise ValueError(
                            f"Gradle {key}={vals!r} does not match "
                            f"build-profile.json compile_loader_version={compiled}"
                        )
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
