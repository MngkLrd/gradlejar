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
import json,re
patch=lzma.decompress(raw)
# SaveData fields and translations diverged in the 0.5.2 build hotfixes.
# Apply those three files via guarded targeted edits rather than stale diff context.
pieces=re.split(r"(?=^--- a/)",patch.decode("utf8"),flags=re.MULTILINE)
skip=("world/PocketRunEntry.java","/lang/en_us.json","/lang/ru_ru.json")
filtered="".join(piece for piece in pieces if not any(needle in piece.split("\\n",1)[0] for needle in skip))
patch=filtered.encode("utf8")
runfile=root/"src/main/java/com/kirill/pocketrealms/world/PocketRunEntry.java"
body=runfile.read_text(encoding="utf8")
for old,new in [
 ("    public long nextShift;","    public long nextShift;\\n    public long collapseStartAt;"),
 ('        tag.putLong("NextShift", nextShift);','        tag.putLong("NextShift", nextShift);\\n        tag.putLong("CollapseStartAt", collapseStartAt);'),
 ('        entry.nextShift = tag.getLong("NextShift");','        entry.nextShift = tag.getLong("NextShift");\\n        entry.collapseStartAt = tag.getLong("CollapseStartAt");'),
]:
    if body.count(old)!=1:raise SystemExit("PocketRunEntry patch position changed: "+old)
    body=body.replace(old,new,1)
runfile.write_text(body,encoding="utf8")
for locale,warning in [("en_us","The stones begin to tremble."),("ru_ru","Камни начинают дрожать.")]:
    lang=root/f"src/main/resources/assets/pocketrealms/lang/{locale}.json"
    data=json.loads(lang.read_text(encoding="utf8"))
    data["message.pocketrealms.collapse_warning"]=warning
    lang.write_text(json.dumps(data,indent=2,ensure_ascii=False)+"\\n",encoding="utf8")
check=subprocess.run(["patch","--dry-run","--fuzz=0","-p1"],cwd=root,input=patch,capture_output=True)
if check.returncode:raise SystemExit("0.5.3 patch conflicts with restored sources:\n"+check.stdout.decode()+check.stderr.decode())
apply=subprocess.run(["patch","--fuzz=0","-p1"],cwd=root,input=patch,capture_output=True)
if apply.returncode:raise SystemExit("0.5.3 patch failed: "+apply.stdout.decode()+apply.stderr.decode())
print("Applied and verified Pocket Realms 0.5.3 source patch, sha256="+sha)
