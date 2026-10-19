"""Select one project for manual runs or all profiles for push runs."""
import json
import os
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]

if __name__ == "__main__":
    requested = (sys.argv[1] if len(sys.argv) > 1 else "").strip()
    if requested:
        projects = [requested]
    else:
        projects = ["."]
        projects.extend(str(p.parent.relative_to(ROOT)) for p in sorted((ROOT / "projects").glob("*/build-profile.json")))
    from resolve_project import read_profile
    for item in projects:
        read_profile(item)
    result = json.dumps(projects, separators=(",", ":"))
    print(f"Projects: {result}", file=sys.stderr)
    with open(os.environ["GITHUB_OUTPUT"], "a", encoding="utf8") as fh:
        fh.write(f"project_dirs={result}\n")
