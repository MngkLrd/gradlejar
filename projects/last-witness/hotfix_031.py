from pathlib import Path

root=Path(__file__).resolve().parent
p=root/"tools/fetch_fixed_skins.py"
s=p.read_text(encoding="utf8")
before="url = texture_meta['textures']['SKIN']['url']"
after="url = texture_meta['textures']['SKIN']['url'].replace('http://textures.minecraft.net/texture/', 'https://textures.minecraft.net/texture/', 1)"
if before not in s: raise RuntimeError("Skin importer hotfix location not found")
s=s.replace(before,after)
s=s.replace("    for name in PLAINS_NAMES:\n        try:", "    for name in PLAINS_NAMES:\n        time.sleep(1.2)\n        try:")
p.write_text(s,encoding="utf8")
for q in (root/"src/main/resources/META-INF/neoforge.mods.toml",root/"README.md"):
    q.write_text(q.read_text(encoding="utf8").replace("0.3.0","0.3.1"),encoding="utf8")
