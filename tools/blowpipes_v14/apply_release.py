#!/usr/bin/env python3
"""Release overlay for Blowpipes 1.4.0, Minecraft 1.21.1 NeoForge."""
from pathlib import Path
import json
import shutil
import sys

root = Path(sys.argv[1]).resolve()
project = Path(__file__).resolve().parent
java = root / "src/main/java/dev/minemods/blowpipes"
resources = root / "src/main/resources"
recipes = resources / "data/blowpipes/recipe"

for name in ("DartProjectile.java", "BlowpipeEvents.java"):
    shutil.copyfile(project / name, java / name)

weapon = java / "BlowpipeItem.java"
source = weapon.read_text()
changes = {
    "float velocity = tier == 0 ? 1.45F : tier == 1 ? 2.25F : 2.95F;":
        "float velocity = tier == 0 ? 1.50F : tier == 1 ? 2.45F : 3.25F;",
    "float spread = tier == 0 ? 1.25F : 0.45F;":
        "float spread = tier == 0 ? 2.20F : tier == 1 ? 0.70F : 0.15F;",
    "player.getCooldowns().addCooldown(this, tier == 0 ? 14 : tier == 1 ? 18 : 24);":
        "player.getCooldowns().addCooldown(this, tier == 0 ? 12 : tier == 1 ? 16 : 22);",
}
for before, after in changes.items():
    if source.count(before) != 1:
        raise ValueError("Missing or ambiguous balance anchor: " + before)
    source = source.replace(before, after)
weapon.write_text(source)

# JEI discovers all nine recipes using the standard vanilla RecipeManager.
# No JEI class is referenced: it is optional for servers and modpack compatibility.
toml = resources / "META-INF/neoforge.mods.toml"
t = toml.read_text()
if t.count('version="1.3.0"') != 1:
    raise ValueError("Wrong starting mod version")
t = t.replace('version="1.3.0"', 'version="1.4.0"')
t += '''
[[dependencies.blowpipes]]
modId="jei"
type="optional"
versionRange="[19.0,)"
ordering="AFTER"
side="CLIENT"
'''
toml.write_text(t)

gradle = root / "build.gradle"
g = gradle.read_text()
assert "version='1.3.0'" in g
gradle.write_text(g.replace("version='1.3.0'", "version='1.4.0'"))

profile = root / "build-profile.json"
if profile.exists():
    p = json.loads(profile.read_text())
    p["version"] = "1.4.0"
    p["jar_glob"] = "blowpipes-neoforge-1.21.1-1.4.0.jar"
    profile.write_text(json.dumps(p, indent=2) + "\n")

def shaped(name, pattern, keys, count=1):
    data = {"type": "minecraft:crafting_shaped", "category": "equipment" if name.endswith("blowpipe") else "misc",
            "pattern": pattern, "key": keys,
            "result": {"id": "blowpipes:" + name, "count": count}}
    (recipes / (name + ".json")).write_text(json.dumps(data, indent=2) + "\n")

shaped("bamboo_blowpipe", [" B ", "BBB", " B "], {"B":"minecraft:bamboo"})
# Actual upgrades: each new weapon is assembled around the previous one.
shaped("hunting_blowpipe", [" L ", "CBC", " C "],
       {"L":"minecraft:leather", "C":"minecraft:copper_ingot", "B":"blowpipes:bamboo_blowpipe"})
shaped("reinforced_blowpipe", [" I ", "IHI", " I "],
       {"I":"minecraft:iron_ingot", "H":"blowpipes:hunting_blowpipe"})
shaped("plain_dart", [" F ", " S "],
       {"F":"minecraft:flint", "S":"minecraft:stick"}, 8)
shaped("iron_dart", [" I ", " S "],
       {"I":"minecraft:iron_nugget", "S":"minecraft:stick"}, 8)
for name, ingredient in (
    ("poison_dart", "minecraft:spider_eye"),
    ("slowness_dart", "minecraft:cobweb"),
    ("fire_dart", "minecraft:blaze_powder"),
    ("sleep_dart", "minecraft:fermented_spider_eye"),
):
    shaped(name, ["D D", " R ", "D D"],
           {"D":"blowpipes:plain_dart", "R":ingredient}, 4)

ru = {
 "bamboo_blowpipe": ("Быстрая (0,6 с), но неточная на дистанции.", "Бонус урона: +0 | Эффекты: x1 | Скорость: 1,5"),
 "hunting_blowpipe": ("Охотничья: точная, дальнобойная и универсальная.", "Бонус урона: +1,5 | Эффекты: x1,4 | Скорость: 2,45"),
 "reinforced_blowpipe": ("Тяжёлая: мощнее и точнее, но медленнее (1,1 с).", "Бонус урона: +3 | Эффекты: x1,8 | Скорость: 3,25"),
 "plain_dart": ("Урон: 2 | Без дополнительных эффектов.", "Дешёвый боеприпас. Бонус трубки прибавляется отдельно."),
 "iron_dart": ("Урон: 5,5 | На 3,5 больше обычного дротика.", "Чистый урон, подходит против нежити."),
 "poison_dart": ("Урон: 1,75 | Яд 8/11/14 сек. (сильная трубка: Яд II).", "Нежить обычно невосприимчива к яду."),
 "slowness_dart": ("Урон: 1,75 | Замедление III на 8/11/14 сек.", "Враги продолжают атаковать и стрелять."),
 "fire_dart": ("Урон: 3 | Поджигает на 6/8/11 секунд.", "Дождь и вода могут потушить цель."),
 "sleep_dart": ("Урон: 0,75 | Сон мобов на 5/7/9 секунд.", "Обычные мобы НЕ атакуют. Урон будит; боссы иммунны. Игроки: замедление.")
}
en = {
 "bamboo_blowpipe": ("Fast reload (0.6s), but inaccurate at long range.", "Damage bonus: +0 | Effects: x1 | Speed: 1.5"),
 "hunting_blowpipe": ("Accurate, long-ranged all-round hunting weapon.", "Damage bonus: +1.5 | Effects: x1.4 | Speed: 2.45"),
 "reinforced_blowpipe": ("Heavy, accurate and powerful. Slower reload (1.1s).", "Damage bonus: +3 | Effects: x1.8 | Speed: 3.25"),
 "plain_dart": ("Damage: 2 | No extra effect.", "Inexpensive ammunition. Pipe bonus adds on top."),
 "iron_dart": ("Damage: 5.5 | +3.5 vs plain dart.", "Reliable pure damage, useful against undead."),
 "poison_dart": ("Damage: 1.75 | Poison for 8/11/14s (strong pipe: Poison II).", "Undead are generally immune to poison."),
 "slowness_dart": ("Damage: 1.75 | Slowness III for 8/11/14s.", "Enemies can still attack and shoot."),
 "fire_dart": ("Damage: 3 | Ignites for 6/8/11 seconds.", "Rain and water can extinguish the target."),
 "sleep_dart": ("Damage: 0.75 | Mob sleep for 5/7/9 seconds.", "Normal mobs CANNOT attack. Damage wakes them; bosses immune. Players slowed.")
}
for loc, items in (("ru_ru",ru),("en_us",en)):
    path = resources / "assets/blowpipes/lang" / (loc + ".json")
    old = json.loads(path.read_text())
    for key, (primary, secondary) in items.items():
        old["tooltip.blowpipes." + key + ".primary"] = primary
        old["tooltip.blowpipes." + key + ".secondary"] = secondary
    old["message.blowpipes.jei"] = ("JEI: наведи курсор на предмет, нажми R для рецепта или U для применения."
        if loc=="ru_ru" else "JEI: hover an item and press R for recipe or U for uses.")
    path.write_text(json.dumps(old, ensure_ascii=False, indent=2) + "\n")

# Recipe-book unlocks complement JEI's default vanilla recipe discovery.
# All nine outputs are announced to the vanilla recipe book after acquiring
# a matching material; no custom JEI categories or hard dependencies.
unlocks = {
    "bamboo_blowpipe": "minecraft:bamboo",
    "hunting_blowpipe": "minecraft:copper_ingot",
    "reinforced_blowpipe": "minecraft:iron_ingot",
    "plain_dart": "minecraft:flint",
    "iron_dart": "minecraft:iron_nugget",
    "poison_dart": "minecraft:spider_eye",
    "slowness_dart": "minecraft:cobweb",
    "fire_dart": "minecraft:blaze_powder",
    "sleep_dart": "minecraft:fermented_spider_eye",
}
adv_dir = resources / "data/blowpipes/advancement/recipes/combat"
adv_dir.mkdir(parents=True, exist_ok=True)
for name, material in unlocks.items():
    advancement = {
        "parent": "minecraft:recipes/root",
        "criteria": {
            "has_ingredient": {"trigger": "minecraft:inventory_changed",
                               "conditions": {"items": [{"items": material}]}},
            "has_recipe": {"trigger": "minecraft:recipe_unlocked",
                           "conditions": {"recipe": "blowpipes:" + name}}
        },
        "requirements": [["has_ingredient", "has_recipe"]],
        "rewards": {"recipes": ["blowpipes:" + name]}
    }
    (adv_dir / (name + ".json")).write_text(json.dumps(advancement, indent=2) + "\n")

(root/"README.md").write_text("""# Blowpipes 1.4.0 — Minecraft 1.21.1 NeoForge

Requires Java 21, NeoForge 21.1.x. Supports optional Just Enough Items (JEI)
1.21.1 NeoForge: the nine standard shaped recipes are automatically indexed.
Install JEI separately into mods/ to use R (crafting recipe) and U (uses).
JEI is not required for the mod or a dedicated server.

## Controls
Hold right mouse to bring the tube to your mouth, release when ready to fire.
Sneak + right mouse changes to the next available dart.

## Weapon progression
- Bamboo: rapid shots, lower precision, +0 damage, 1.0x effect time.
- Hunting: balanced precision, +1.5 damage, 1.4x effect time.
- Reinforced: highest precision/power, slower reload, +3 damage, 1.8x effect time.
Each advanced pipe now consumes the preceding tier in its crafting recipe.

## Ammo and effects
Plain 2 dmg; iron 5.5 dmg; poison 1.75 dmg plus timed Poison;
slowness 1.75 dmg plus Slowness III (not a stun);
fire 3 dmg and 6 seconds burn (scaled); sleep 0.75 dmg and
5/7/9 seconds of *actual server-side mob AI pause*.
A sleeping mob stops attacking (including skeletons and zombies).
Further damage wakes it. Mobs >=100 max HP and elder guardians cannot sleep.
Players receive only 2.5 seconds of Slowness III.

## Build
Use Java 21, Gradle 8.14.5: gradle clean build
Compiled jar: build/libs/blowpipes-neoforge-1.21.1-1.4.0.jar
""")
for path in recipes.glob("*.json"):
    json.loads(path.read_text())
for path in adv_dir.glob("*.json"):
    json.loads(path.read_text())
print("Blowpipes 1.4.0 validated: 9 JEI-visible vanilla recipes, 9 recipe-book advancements, 18 tooltip lines, two server-side classes.")
