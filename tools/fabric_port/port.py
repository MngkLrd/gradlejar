#!/usr/bin/env python3
"""Port the verified NeoForge 1.4.0 source archive into two real Fabric projects.
The released source ZIP must be unzipped at --source first. All content/assets
originate from that release; Fabric-only code lives beside this script.
"""
import argparse, json, shutil
from pathlib import Path

here=Path(__file__).resolve().parent
parser=argparse.ArgumentParser()
parser.add_argument("--source",type=Path,required=True)
parser.add_argument("--output",type=Path,default=Path.cwd())
args=parser.parse_args()
source=args.source.resolve()
if not (source/"src/main/java/dev/minemods/blowpipes/BlowpipeItem.java").exists():
    raise RuntimeError("Missing verified 1.4.0 source root: "+str(source))

items=["bamboo_blowpipe","hunting_blowpipe","reinforced_blowpipe",
       "plain_dart","iron_dart","poison_dart","slowness_dart","fire_dart","sleep_dart"]

for mc in ("1.20.1","1.21.1"):
    old=mc=="1.20.1"
    java=17 if old else 21
    folder="blowpipes-fabric-"+mc
    dest=(args.output/folder).resolve()
    java_path=dest/"src/main/java/dev/minemods/blowpipes"
    common_source=source/"src/main/java/dev/minemods/blowpipes"
    resources=dest/"src/main/resources"
    java_path.mkdir(parents=True,exist_ok=True)
    shutil.copytree(source/"src/main/resources",resources,dirs_exist_ok=True)
    # NeoForge bootstrap/extension metadata cannot be present in Fabric jar.
    if (resources/"META-INF").exists():
        shutil.rmtree(resources/"META-INF")
    # Do not include NeoForge-exclusive Java classes.
    for name in ("BlowpipeItem","DartProjectile","DartModel","DartRenderer"):
        s=(common_source/(name+".java")).read_text(encoding="utf-8")
        if old:
            s=s.replace("ResourceLocation.fromNamespaceAndPath(", "new ResourceLocation(")
            s=s.replace("target.igniteForSeconds(", "target.setSecondsOnFire(")
            s=s.replace("SoundEvents.BREEZE_SHOOT", "SoundEvents.FIRE_EXTINGUISH")
            s=s.replace(
                "public int getUseDuration(ItemStack stack, LivingEntity user)",
                "public int getUseDuration(ItemStack stack)")
        (java_path/(name+".java")).write_text(s,encoding="utf-8")
    for name in ("Blowpipes","BlowpipeEvents","SleepState","BlowpipesClient"):
        s=(here/(name+".java")).read_text(encoding="utf-8")
        if name=="Blowpipes":
            resolution = "new ResourceLocation(ID, name)" if old else \
                "ResourceLocation.fromNamespaceAndPath(ID, name)"
            s=s.replace("/*VERSION_LOCATION*/",resolution)
        (java_path/(name+".java")).write_text(s,encoding="utf-8")
    if not old:
        client_file=java_path/"BlowpipesClient.java"
        client_source=client_file.read_text(encoding="utf-8")
        assert "(stack, context, lines) ->" in client_source
        client_file.write_text(client_source.replace("(stack, context, lines) ->", "(stack, context, tooltipType, lines) ->"),encoding="utf-8")
    curves_path=java_path/"client/BlowpipeAnimationCurves.java"
    curves_path.parent.mkdir(parents=True,exist_ok=True)
    shutil.copyfile(common_source/"client/BlowpipeAnimationCurves.java",curves_path)
    for name,package in (("MobSleepMixin","mixin"),("FirstPersonMixin","mixin/client"),
                         ("ThirdPersonMixin","mixin/client")):
        target=java_path/package/(name+".java")
        target.parent.mkdir(parents=True,exist_ok=True)
        shutil.copyfile(here/(name+".java"),target)

    for model in items:
        path=resources/"assets/blowpipes/models/item"/(model+".json")
        assert path.is_file(),path
        json.loads(path.read_text())
    for dart in items[3:]:
        assert (resources/"assets/blowpipes/textures/entity/dart"/(dart+".png")).exists()

    if old:
        # 1.20 uses plural data directories, old item predicates and old
        # result/key shapes. 1.21 uses singular data directories and IDs.
        namespace=resources/"data/blowpipes"
        old_recipes=namespace/"recipe"
        new_recipes=namespace/"recipes"
        new_recipes.mkdir(parents=True,exist_ok=True)
        for item in items:
            d=json.loads((old_recipes/(item+".json")).read_text())
            d["key"]={k:({"item":v} if isinstance(v,str) else v)
                      for k,v in d["key"].items()}
            if "id" in d["result"]:
                d["result"]["item"]=d["result"].pop("id")
            (new_recipes/(item+".json")).write_text(json.dumps(d,indent=2)+"\n")
        shutil.rmtree(old_recipes)
        from_adv=namespace/"advancement"
        to_adv=namespace/"advancements"
        if from_adv.exists():
            for p in from_adv.rglob("*.json"):
                ad=json.loads(p.read_text())
                for criterion in ad["criteria"].values():
                    cond=criterion.get("conditions",{})
                    for predicate in cond.get("items",[]):
                        listed=predicate.get("items")
                        if isinstance(listed,str):predicate["items"]=[listed]
                out=to_adv/p.relative_to(from_adv)
                out.parent.mkdir(parents=True,exist_ok=True)
                out.write_text(json.dumps(ad,indent=2)+"\n")
            shutil.rmtree(from_adv)
    else:
        assert (resources/"data/blowpipes/recipe").is_dir()

    (resources/"pack.mcmeta").write_text(
        json.dumps({"pack":{"pack_format":15 if old else 34,
                            "description":"Blowpipes vanilla-scale 3D assets for Fabric "+mc}},indent=2)+"\n")
    (resources/"fabric.mod.json").write_text(json.dumps({
        "schemaVersion":1,
        "id":"blowpipes","version":"1.4.0",
        "name":"Blowpipes",
        "description":"Three blowpipes, six effect darts, custom aiming and real mob sleep.",
        "environment":"*",
        "license":"MIT",
        "entrypoints":{
            "main":["dev.minemods.blowpipes.Blowpipes"],
            "client":["dev.minemods.blowpipes.BlowpipesClient"]},
        "mixins":["blowpipes.mixins.json"],
        "depends":{"fabricloader":">=0.16.0",
                   "minecraft":"~"+mc,
                   "java":">="+str(java),
                   "fabric-api":"*"},
        "suggests":{"emi":"*","jei":"*"}
    },indent=2)+"\n")
    (resources/"blowpipes.mixins.json").write_text(json.dumps({
        "required":True,"minVersion":"0.8",
        "package":"dev.minemods.blowpipes.mixin",
        "compatibilityLevel":"JAVA_"+str(java),
        "mixins":["MobSleepMixin"],
        "client":["client.FirstPersonMixin","client.ThirdPersonMixin"],
        "injectors":{"defaultRequire":1}
    },indent=2)+"\n")

    (dest/"settings.gradle").write_text(
        "pluginManagement { repositories { maven { url='https://maven.fabricmc.net/' }; gradlePluginPortal(); mavenCentral() } }\n"
        "rootProject.name='blowpipes-fabric-"+mc+"'\n")
    (dest/"gradle.properties").write_text("org.gradle.jvmargs=-Xmx3G\norg.gradle.daemon=false\n")
    (dest/"build.gradle").write_text(
        "plugins { id 'fabric-loom' version '1.11.8'; id 'maven-publish' }\n"
        "group='dev.minemods'\nversion='1.4.0'\n"
        "base.archivesName='blowpipes-fabric-"+mc+"'\n"
        "java.toolchain.languageVersion=JavaLanguageVersion.of("+str(java)+")\n"
        "dependencies {\n"
        "    minecraft 'com.mojang:minecraft:"+mc+"'\n"
        "    mappings loom.officialMojangMappings()\n"
        "    modImplementation 'net.fabricmc:fabric-loader:0.17.3'\n"
        "    modImplementation 'net.fabricmc.fabric-api:fabric-api:"+
             ("0.92.2+1.20.1" if old else "0.116.17+1.21.1")+"'\n"
        "}\n"
        "tasks.withType(JavaCompile).configureEach {\n"
        "    options.encoding='UTF-8'\n"
        "    options.release="+str(java)+"\n"
        "}\n"
        "")
    (dest/"README.md").write_text(
        "# Blowpipes 1.4.0, Fabric "+mc+"\n\n"
        "Java "+str(java)+" for Minecraft; Fabric Loader 0.16+ and Fabric API required.\n"
        "Install JEI or EMI separately for R/U recipe lookup; no hard dependency.\n\n"
        "Hold RMB to bring the pipe to the mouth; release after half a second.\n"
        "Shift+RMB cycles between available darts in the inventory.\n\n"
        "Sleep dart temporarily disables ordinary mob AI (including skeleton attacks).\n"
        "Damage wakes the mob, expiring sleep restores its previous AI state.\n"
        "Effect duration is 5/7/9 seconds depending on the blowpipe.\n\n"
        "Other ammo: flint, iron, poison, slowness, fire, sleep.\n"
        "Upgrades: bamboo -> hunting -> reinforced, using prior tiers in recipes.\n\n"
        "Build: Gradle 8.14.5, run \`gradle clean build\`.\n"
        "JAR: build/libs/blowpipes-fabric-"+mc+"-1.4.0.jar.\n"
        "Requires manual visual and combat testing in game.\n"
    )
    (dest/"build-profile.json").write_text(json.dumps({
        "mod_id":"blowpipes","version":"1.4.0","minecraft":mc,
        "loader":"fabric","java":java,"gradle":"8.14.5"
    },indent=2)+"\n")
    # Hard-fail on missing localizations, recipes or rendering resources.
    lang=json.loads((resources/"assets/blowpipes/lang/en_us.json").read_text())
    for item in items:
        assert "tooltip.blowpipes."+item+".primary" in lang
        assert "tooltip.blowpipes."+item+".secondary" in lang
        assert (resources/"data/blowpipes"/("recipes" if old else "recipe")/
                (item+".json")).is_file(), item
    print("PORT READY:",dest,"Java",java,"9 recipes, 9 tooltips, 3D assets, sleep mixin")
