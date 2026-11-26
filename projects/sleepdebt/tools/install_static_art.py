"""Unpack reviewed PNG bytes to a generated resource directory; no drawing or recoloring."""
import base64, json, struct, sys, zlib
from pathlib import Path
import shutil
root=Path(sys.argv[1])/"assets"/"sleepdebt"/"textures"
if root.exists(): shutil.rmtree(root)
manifest=json.loads(Path(__file__).with_name("static_art_315.json").read_text())
assert len(manifest)==57, len(manifest)
for name, encoded in manifest.items():
    assert name.split("/")[0] in {"item","block","gui","mob_effect"} and ".." not in name
    raw=base64.b64decode(encoded,validate=True)
    assert raw[:8]==b"\x89PNG\r\n\x1a\n", name
    w,h=struct.unpack(">II",raw[16:24])
    pos=8
    data=bytearray()
    while pos<len(raw):
        length=struct.unpack(">I",raw[pos:pos+4])[0]
        kind=raw[pos+4:pos+8]
        payload=raw[pos+8:pos+8+length]
        assert zlib.crc32(kind+payload)&0xffffffff==struct.unpack(">I",raw[pos+8+length:pos+12+length])[0], name
        if kind==b"IDAT": data.extend(payload)
        pos+=length+12
    pixels=zlib.decompress(data)
    assert len(pixels)==h*(w*4+1),name
    path=root/name
    path.parent.mkdir(parents=True,exist_ok=True)
    path.write_bytes(raw)
print("Installed 57 static PNG textures with verified CRC and DEFLATE")
