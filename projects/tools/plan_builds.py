"""Choose changed mod projects on push or one explicitly requested project.

An unrelated project with a malformed build profile must not block a mod's build.
The GitHub push event payload enumerates changed paths without requiring git history.
"""
import json
import os
import subprocess
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]


def changed_paths_from_push():
    event_path = os.environ.get("GITHUB_EVENT_PATH", "")
    if os.environ.get("GITHUB_EVENT_NAME") not in ("push", "pull_request"):
        return None
    # Use the checked-out commit's real diff, not the GitHub event's
    # sometimes missing 'commits[].modified' lists. Plan job fetches depth=2.
    diff = subprocess.run(
        ["git", "diff", "--name-only", "HEAD^", "HEAD"],
        cwd=ROOT, capture_output=True, text=True, check=False
    )
    if diff.returncode == 0 and diff.stdout.strip():
        return sorted(set(diff.stdout.splitlines()))
    if not event_path:
        return None
    try:
        event = json.loads(Path(event_path).read_text(encoding="utf8"))
    except (OSError, ValueError):
        return None
    if "commits" not in event:
        return None
    changed = set()
    for commit in event.get("commits", []):
        for category in ("added", "modified", "removed"):
            changed.update(commit.get(category, []))
    if not changed:
        for category in ("added", "modified", "removed"):
            changed.update((event.get("head_commit") or {}).get(category, []))
    return sorted(changed) if changed else None


def select_projects(requested):
    if requested:
        return [requested]
    changed = changed_paths_from_push()
    if changed is None:
        # Fallback for older/manual runs where the push payload is unavailable.
        # Keep other project profiles independent: a broken mod should not block all builds.
        return ["."]
    projects = set()
    for path in changed:
        parts = Path(path).parts
        if len(parts) >= 3 and parts[0] == "projects":
            if parts[1] and (ROOT / "projects" / parts[1] / "build-profile.json").is_file():
                projects.add("projects/" + parts[1])
        elif path in ("build.gradle", "settings.gradle", "gradle.properties", "build-profile.json") or path.startswith("src/"):
            projects.add(".")
    return sorted(projects) if projects else ["."]


if __name__ == "__main__":
    requested = (sys.argv[1] if len(sys.argv) > 1 else "").strip()
    project_dirs = select_projects(requested)
    from resolve_project import read_profile
    for project in project_dirs:
        read_profile(project)
    result = json.dumps(project_dirs, separators=(",", ":"))
    print(f"Projects: {result}", file=sys.stderr)
    with open(os.environ["GITHUB_OUTPUT"], "a", encoding="utf8") as fh:
        fh.write(f"project_dirs={result}\n")
