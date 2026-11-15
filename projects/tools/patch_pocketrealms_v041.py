"""Pocket Realms 0.4.1: buff corrupted hostiles and rebalance dungeon lighting.

Runs AFTER all older Pocket Realms source overlays on CI.
"""
from pathlib import Path

root = Path(__file__).resolve().parents[1] / "projects/pocketrealms"
src = root / "src/main/java/com/kirill/pocketrealms"

def patch(rel, replacements):
    target = src / rel
    body = target.read_text(encoding="utf-8")
    for old, new in replacements:
        if body.count(old) != 1:
            raise SystemExit(f"{rel}: expected one occurrence for {old!r}, got {body.count(old)}")
        body = body.replace(old, new, 1)
    target.write_text(body, encoding="utf-8")

patch("registry/ModEntityAttributes.java", [
    ("Attributes.MAX_HEALTH, 22.0D", "Attributes.MAX_HEALTH, 34.0D"),
    ("Attributes.MOVEMENT_SPEED, 0.245D", "Attributes.MOVEMENT_SPEED, 0.30D"),
    ("Attributes.ATTACK_DAMAGE, 3.0D", "Attributes.ATTACK_DAMAGE, 6.0D"),
    ("Attributes.MAX_HEALTH, 18.0D", "Attributes.MAX_HEALTH, 28.0D"),
    ("Attributes.MOVEMENT_SPEED, 0.225D", "Attributes.MOVEMENT_SPEED, 0.285D"),
])
patch("entity/CorruptedSkeleton.java", [
    ("import net.minecraft.world.DifficultyInstance;",
     "import net.minecraft.core.registries.Registries;\nimport net.minecraft.world.item.enchantment.Enchantments;\nimport net.minecraft.world.DifficultyInstance;"),
    ("""        if (!getMainHandItem().is(Items.BOW)) {
            setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.BOW));
        }""",
     """        ItemStack bow = new ItemStack(Items.BOW);
        // Power II makes corrupted archers substantially more dangerous than vanilla skeletons.
        level.getLevel().registryAccess().registryOrThrow(Registries.ENCHANTMENT)
                .getHolder(Enchantments.POWER).ifPresent(enchantment -> bow.enchant(enchantment, 2));
        setItemSlot(EquipmentSlot.MAINHAND, bow);"""),
    ("protected int getAttackInterval() { return 32; }",
     "protected int getAttackInterval() { return 26; }"),
])
patch("world/PocketGenerator.java", [
    ("every seven steps", "every ten steps"),
    ("++step % 7 == 0", "++step % 10 == 0"),
    ("random.nextFloat() < 0.17F", "random.nextFloat() < 0.08F"),
    ("random.nextFloat() < 0.11F", "random.nextFloat() < 0.05F"),
])

props = root / "gradle.properties"
s = props.read_text(encoding="utf8")
if s.count("mod_version=0.4.0") != 1:
    raise SystemExit("Expected mod_version=0.4.0")
props.write_text(s.replace("mod_version=0.4.0", "mod_version=0.4.1"), encoding="utf8")

readme = root / "README.md"
readme.write_text(readme.read_text(encoding="utf8") + """
## Pocket Realms 0.4.1: combat and lighting rebalance

- Corrupted Zombie: 34 HP, speed 0.30, base attack damage 6.
- Corrupted Skeleton: 28 HP, speed 0.285, Power II bow, 26-tick attack interval.
- Ambient lantern chance: 17% -> 8%; soul torch chance: 11% -> 5%.
- Guaranteed route marker lamps every 10 cells, not 7.
- Exit chamber remains illuminated.
- No changes to portal mechanics, spawner limits, loot, run cleanup or collapse timer.
""", encoding="utf8")
print("Pocket Realms 0.4.1 rebalance applied.")
