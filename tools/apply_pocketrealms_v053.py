"""Apply the 0.5.3 Living Nightmare source diff after previous verified overlays."""
from pathlib import Path
import base64,hashlib,lzma,subprocess
repo=Path(__file__).resolve().parents[1]
root=repo/"projects"/"pocketrealms"
parts=[root/f"upgrade-v053-{i:02d}.b64" for i in range(3)]
raw=base64.b64decode("".join(p.read_text("ascii") for p in parts),validate=True)
sha=hashlib.sha256(raw).hexdigest()
if sha!="611324218e76f521b99ae807ee1b19a66bbf46ab647d29edb3e0f538b1de9ee6":
    raise SystemExit("Patch transport corrupt: "+sha)
patch=lzma.decompress(raw)
check=subprocess.run(["patch","--dry-run","--fuzz=0","-p1"],cwd=root,input=patch,capture_output=True)
if check.returncode:raise SystemExit("0.5.3 patch conflicts with restored sources:\n"+check.stdout.decode()+check.stderr.decode())
apply=subprocess.run(["patch","--fuzz=0","-p1"],cwd=root,input=patch,capture_output=True)
if apply.returncode:raise SystemExit("0.5.3 patch failed: "+apply.stdout.decode()+apply.stderr.decode())
print("Applied and verified Pocket Realms 0.5.3 source patch, sha256="+sha)
