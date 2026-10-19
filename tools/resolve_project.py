"""Resolve and validate a per-mod build profile for GitHub Actions."""
import json
import re
import sys
from pathlib import Path

REPO = Path(__file__).resolve().parents[1]
NAME = re.compile(r"^[a-z][a-z0-9_-]{0,60}$")
VERSION = re.compile(r"^[0-9A-Za-z][0-9A-Za-z._-]{0,63}$")
GRADLE = re.compile(r"^[0-9]+\.[0-9]+(?:\.[0-9]+)?$")
MINECRAFT = re.compile(r"^1\.[0-9]+(?:\.[0-9]+)?$")


def read_profile(relative):
    if relative != "." and not re.fullmatch(r"projects/[a-z][a-z0-9_-]{0,60}", relative):
        raise ValueError("Project must be '.' or 'projects/<slug>'")
    base = (REPO / relative).resolve()
    if not base.is_relative_to(REPO):
        raise ValueError("Project path escapes repository")
    if not (base / "settings.gradle").is_file() and not (base / "settings.gradle.kts").is_file():
        raise ValueError("Missing settings.gradle(.kts)")
    if not (base / "build.gradle").is_file() and not (base / "build.gradle.kts").is_file():
        raise ValueError("Missing build.gradle(.kts)")
    profile_file = base / "build-profile.json"
    if not profile_file.is_file():
        raise ValueError(f"Missing {relative}/build-profile.json")
    p = json.loads(profile_file.read_text("utf-8"))
    if not isinstance(p, dict):
        raise ValueError("build-profile.json must be an object")
    for key in ["slug", "version", "minecraft", "loader", "java", "gradle", "jar_glob"]:
        if not isinstance(p.get(key), str):
            raise ValueError(f"Expected string property: {key}")
    if not NAME.fullmatch(p["slug"]):
        raise ValueError("Invalid slug")
    if not VERSION.fullmatch(p["version"]):
        raise ValueError("Invalid version")
    if not MINECRAFT.fullmatch(p["minecraft"]):
        raise ValueError("Invalid Minecraft version")
    if p["loader"] not in ("neoforge", "forge", "fabric"):
        raise ValueError("Loader must be neoforge, forge, or fabric")
    if p["java"] not in ("17", "21", "25"):
        raise ValueError("Java must be 17, 21, or 25")
    if not GRADLE.fullmatch(p["gradle"]):
        raise ValueError("Invalid Gradle version")
    glob = p["jar_glob"]
    if not glob or len(glob) > 100 or "/" in glob or "\\" in glob or ".." in glob or "[" in glob or "]" in glob:
        raise ValueError("Unsafe jar_glob; only file names/wildcards within build/libs are allowed")
    if not glob.endswith(".jar"):
        raise ValueError("jar_glob must end with .jar")
    p["project_dir"] = relative
    return p


def main():
    project = sys.argv[1] if len(sys.argv) > 1 else "."
    p = read_profile(project)
    fields = ("slug", "version", "minecraft", "loader", "java", "gradle", "jar_glob", "project_dir")
    for k in fields:
        print(f"{k}={p[k]}")
    print(f"Validated profile: {p['slug']} / Minecraft {p['minecraft']} / {p['loader']}", file=sys.stderr)


if __name__ == "__main__":
    try:
        main()
    except (ValueError, OSError, json.JSONDecodeError) as exc:
        sys.exit(f"Invalid project profile: {exc}")
