"""NeoForge 1.21.1 fixes for Pocket Realms 0.5.0 new mobs."""
from pathlib import Path
root=Path(__file__).resolve().parents[1]/"projects/pocketrealms/src/main/java/com/kirill/pocketrealms"

def replace(rel, pairs):
    file=root/rel
    src=file.read_text(encoding="utf8")
    for original, fixed in pairs:
        if src.count(original)!=1:
            raise SystemExit(f"{rel}: unexpected source for {original}")
        src=src.replace(original,fixed,1)
    file.write_text(src,encoding="utf8")

replace("entity/WallGrasper.java",[
("@Override protected void addAdditionalSaveData","@Override public void addAdditionalSaveData"),
("@Override protected void readAdditionalSaveData","@Override public void readAdditionalSaveData")])
replace("entity/VoidPursuer.java",[
("import net.minecraft.world.entity.monster.Monster;","import net.minecraft.world.entity.monster.Zombie;"),
("extends Monster {","extends Zombie {"),
("EntityType<? extends Monster> type","EntityType<? extends Zombie> type"),
("p -> !p.isCreative() && !p.isSpectator()",
 "p -> p instanceof Player target && !target.isCreative() && !target.isSpectator()")])
replace("entity/VoidPursuer.java", [("return Monster.createMonsterAttributes()", "return Zombie.createAttributes()")])
print("NeoForge 1.21.1 compatibility changes applied to hand mob and pursuer.")
