"""Fix spawner and hanging lamp vertical positions after 0.6.0 roof height increase."""
from pathlib import Path
root=Path(__file__).resolve().parents[1]/"projects/pocketrealms/src/main/java/com/kirill/pocketrealms/world"
gen=root/"PocketGenerator.java"
src=gen.read_text("utf8")
for old,new in [
("gridToWorld(centerX, centerZ, gx, gz).above(3)","gridToWorld(centerX, centerZ, gx, gz).above(HEIGHT-1)"),
("pos.above(3)","pos.above(HEIGHT-1)"),
("exitCell.offset(1, 3, 0)","exitCell.offset(1, HEIGHT-1, 0)"),
("exitCell.offset(dx, 3, -1)","exitCell.offset(dx, HEIGHT-1, -1)")
]:
    if old not in src:raise SystemExit("Expected pre-fix roof placement: "+old)
    src=src.replace(old,new)
gen.write_text(src,encoding="utf8")
spawner=root/"CorruptionSpawner.java"
s=spawner.read_text("utf8")
if s.count("nest.getY()-3")!=1:raise SystemExit("Ceiling nest source changed")
spawner.write_text(s.replace("nest.getY()-3","nest.getY()-4"),encoding="utf8")
print("0.6.0 roof-height dependent light and mob positions validated.")
