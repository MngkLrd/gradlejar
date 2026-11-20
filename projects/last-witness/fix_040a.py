from pathlib import Path
p=Path(__file__).resolve().parent/"src/main/java/dev/minemods/lastwitness/guard/GuardEntity.java"
s=p.read_text(encoding="utf8")
for typ in ("Player","Monster"):
    old=f"new NearestAttackableTargetGoal<>(this, {typ}.class, 8, true,"
    new=f"new NearestAttackableTargetGoal<>(this, {typ}.class, 8, true, false,"
    if old not in s: raise RuntimeError("Missing guard target goal: "+typ)
    s=s.replace(old,new)
for name in ("addAdditionalSaveData","readAdditionalSaveData"):
    old=f"protected void {name}(CompoundTag tag)"
    new=f"public void {name}(CompoundTag tag)"
    if old not in s: raise RuntimeError("Missing guard NBT method: "+name)
    s=s.replace(old,new)
p.write_text(s,encoding="utf8")
