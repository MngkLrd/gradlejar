#!/usr/bin/env python3
"""Produce FOUR real 26.x loaders with version-specific recipes, models, metadata.
Source: Blowpipes 1.4.0 release; all nine vanilla scale textures are retained.
No 1.21.x advancement schema is carried across 26.2 / 26.3.
"""
import json,shutil,sys
from pathlib import Path
src=Path(sys.argv[1]).resolve()
output=Path(sys.argv[2]).resolve()
here=Path(__file__).resolve().parent/'modern'
assert (src/'src/main/resources/assets/blowpipes/lang/en_us.json').is_file()
assets=src/'src/main/resources/assets/blowpipes'
recipes=src/'src/main/resources/data/blowpipes/recipe'
all_names=['bamboo_blowpipe','hunting_blowpipe','reinforced_blowpipe',
           'plain_dart','iron_dart','poison_dart','slowness_dart','fire_dart','sleep_dart']
dart_names=all_names[3:]
for name in all_names:assert (recipes/(name+'.json')).is_file(),name
def flight_model(name):
    # Native pixel units, no paper-thin quads or billboard sprites.
    faces={}
    def cuboid(lo,hi):
        f={}
        for face in ('north','south','east','west','up','down'):
            if face in ('north','south'):uv=[0,0,max(2,hi[0]-lo[0]),max(2,hi[1]-lo[1])]
            elif face in ('east','west'):uv=[0,0,max(2,hi[2]-lo[2]),max(2,hi[1]-lo[1])]
            else:uv=[0,0,max(2,hi[0]-lo[0]),max(2,hi[2]-lo[2])]
            f[face]={'uv':uv,'texture':'#surface'}
        return {'from':lo,'to':hi,'faces':f}
    return {'ambientocclusion':False,'textures':{'surface':'blowpipes:item/'+name,
                'particle':'blowpipes:item/'+name},
            'elements':[
                cuboid([7,7,2],[9,9,14]),
                cuboid([6,6,13],[10,10,15]),
                cuboid([4,7,0],[12,9,3]),
                cuboid([7,4,0],[9,12,3])],
            'display':{
                'gui':{'rotation':[0,140,-45],'scale':[.7,.7,.7]},
                'ground':{'scale':[.5,.5,.5]},
                'firstperson_righthand':{'rotation':[0,100,45],'scale':[.65,.65,.65]},
                'thirdperson_righthand':{'rotation':[0,100,45],'scale':[.65,.65,.65]}
            }}
for mc in ['26.2','26.3']:
    for loader in ['neoforge','fabric']:
        dir=output/('blowpipes-'+loader+'-'+mc)
        j=dir/'src/main/java/dev/minemods/blowpipes'
        r=dir/'src/main/resources'
        j.mkdir(parents=True,exist_ok=True)
        dest_assets=r/'assets/blowpipes'
        shutil.copytree(assets,dest_assets,dirs_exist_ok=True)
        # This folder, introduced after 1.21.2, tells the game which item model
        # to load; copying only models/item/ would create missing textures in 26.x.
        items_json=dest_assets/'items'
        items_json.mkdir(parents=True,exist_ok=True)
        for name in all_names+['flight_'+n for n in dart_names]:
            if name.startswith('flight_'):
                path=dest_assets/'models/item'/(name+'.json')
                path.write_text(json.dumps(flight_model(name[7:]),indent=2)+'\n')
            itemdef={'model':{'type':'minecraft:model','model':'blowpipes:item/'+name}}
            (items_json/(name+'.json')).write_text(json.dumps(itemdef,indent=2)+'\n')
        # Copy the validated new shaped recipe format ONLY. Do not copy 1.21.1
        # recipe-book achievements: the 26.3 recipe_unlocked trigger changed.
        output_rec=r/'data/blowpipes/recipe'
        output_rec.mkdir(parents=True,exist_ok=True)
        for name in all_names:
            obj=json.loads((recipes/(name+'.json')).read_text())
            assert obj['type']=='minecraft:crafting_shaped',name
            assert isinstance(obj['result']['id'],str) and obj['result']['id']=='blowpipes:'+name
            assert obj['result']['count']>=1
            assert all(isinstance(v,str) for v in obj['key'].values()),name
            assert len({len(x) for x in obj['pattern']})==1,name
            (output_rec/(name+'.json')).write_text(json.dumps(obj,indent=2)+'\n')
        for lang in ('en_us','ru_ru'):
            path=dest_assets/'lang'/(lang+'.json')
            data=json.loads(path.read_text())
            data['itemGroup.blowpipes.main']='Духовые трубки' if lang=='ru_ru' else 'Blowpipes'
            path.write_text(json.dumps(data,ensure_ascii=False,indent=2)+'\n')
        pack={'pack':{'description':'Blowpipes '+loader+' '+mc,
               'min_format':[88,0] if mc=='26.2' else [97,1],
               'max_format':[88,0] if mc=='26.2' else 97}}
        (r/'pack.mcmeta').write_text(json.dumps(pack,indent=2)+'\n')
        for name in ('BlowpipeItem','BlowpipeInfoItem','DartProjectile','SleepSystem'):
            shutil.copyfile(here/(name+'.java'),j/(name+'.java'))
        if loader=='neoforge':
            shutil.copyfile(here/'BlowpipesNeo.java',j/'Blowpipes.java')
            shutil.copyfile(here/'BlowpipesNeoClient.java',j/'BlowpipesClient.java')
            version='26.2.0.93' if mc=='26.2' else '26.3.0.95'
            (dir/'settings.gradle').write_text(
                "pluginManagement { repositories { maven { url = 'https://maven.neoforged.net/releases' }; gradlePluginPortal(); mavenCentral() } }\n"
                "rootProject.name='blowpipes-neoforge-"+mc+"'\n")
            (dir/'build.gradle').write_text(
                "plugins { id 'java-library'; id 'idea'; id 'net.neoforged.moddev' version '2.0.148' }\n"
                "group='dev.minemods'\nversion='1.4.1'\nbase.archivesName='blowpipes-neoforge-"+mc+"'\n"
                "java.toolchain.languageVersion=JavaLanguageVersion.of(25)\n"
                "neoForge { version='"+version+"'\n"
                "  runs {client {client()};server {server()}}\n"
                "  mods {blowpipes {sourceSet(sourceSets.main)}}\n}\n"
                "tasks.withType(JavaCompile).configureEach { options.encoding='UTF-8';options.release=25 }\n")
            meta=r/'META-INF'
            meta.mkdir(parents=True,exist_ok=True)
            (meta/'neoforge.mods.toml').write_text(
                'license="MIT"\n'
                '[[mods]]\nmodId="blowpipes"\nversion="1.4.1"\n'
                'displayName="Blowpipes"\nauthors="Minemods"\n'
                'description="Three blowpipes, six 3D dart variants and real monster sleep."\n'
                '[[dependencies.blowpipes]]\nmodId="neoforge"\ntype="required"\n'
                'versionRange="['+version+',)"\nordering="NONE"\nside="BOTH"\n'
                '[[dependencies.blowpipes]]\nmodId="minecraft"\ntype="required"\n'
                'versionRange="['+mc+']"\nordering="NONE"\nside="BOTH"\n')
        else:
            shutil.copyfile(here/'BlowpipesFabric.java',j/'Blowpipes.java')
            shutil.copyfile(here/'BlowpipesFabricClient.java',j/'BlowpipesClient.java')
            loom='1.17.21' if mc=='26.2' else '1.18-SNAPSHOT'
            api='0.161.0+26.2' if mc=='26.2' else '0.164.0+26.3'
            (dir/'settings.gradle').write_text(
                "pluginManagement { repositories { maven {url='https://maven.fabricmc.net/'}; gradlePluginPortal(); mavenCentral() } }\n"
                "rootProject.name='blowpipes-fabric-"+mc+"'\n")
            (dir/'build.gradle').write_text(
                "plugins { id 'net.fabricmc.fabric-loom' version '"+loom+"'; id 'maven-publish' }\n"
                "group='dev.minemods'\nversion='1.4.1'\nbase.archivesName='blowpipes-fabric-"+mc+"'\n"
                "java.toolchain.languageVersion=JavaLanguageVersion.of(25)\n"
                "dependencies {minecraft 'com.mojang:minecraft:"+mc+"'\n"
                " implementation 'net.fabricmc:fabric-loader:0.19.5'\n"
                " implementation 'net.fabricmc.fabric-api:fabric-api:"+api+"'\n}\n"
                "tasks.withType(JavaCompile).configureEach { options.encoding='UTF-8';options.release=25 }\n")
            (r/'fabric.mod.json').write_text(json.dumps({
                'schemaVersion':1,'id':'blowpipes','version':'1.4.1',
                'name':'Blowpipes','license':'MIT',
                'environment':'*',
                'entrypoints':{'main':['dev.minemods.blowpipes.Blowpipes'],
                               'client':['dev.minemods.blowpipes.BlowpipesClient']},
                'depends':{'fabricloader':'>=0.19.5','minecraft':mc,'java':'>=25','fabric-api':'*'},
                'suggests':{'jei':'*','emi':'*'}
            },indent=2)+'\n')
        (dir/'gradle.properties').write_text('org.gradle.jvmargs=-Xmx4G\norg.gradle.daemon=false\n')
        (dir/'README.md').write_text(
            '# Blowpipes 1.4.1 for '+loader+' '+mc+'\n\n'
            'Java 25. Gradle 9.2.1. All nine shaped recipes use the current 26.x format.\n'
            'Six dart effects including server-side sleep that interrupts zombie/skeleton AI.\n'
            'Hold RMB to aim for 0.5s, release to fire; Shift+RMB cycles ammo.\n'
            'JEI/EMI are optional; recipe JSON is vanilla and compatible with both.\n'
            'The recipe-book advancements from 1.21.1 were deliberately removed\n'
            'rather than shipping stale triggers that can break 26.x data loading.\n'
            'Upgrade bamboo -> hunting -> reinforced via recipes, as in 1.4.0.\n'
            'Flight-specific item models are geometric, separate from 16px inventory icons.\n'
            'Gradle build: gradle clean build; find resulting jar in build/libs/.\n'
            'Runtime player testing is still required.\n')
        for name in all_names:
            assert (dest_assets/'textures/item'/(name+'.png')).is_file(),name
            assert (items_json/(name+'.json')).is_file(),name
        print('GENERATED',dir,'9 recipes, 15 item-model dispatchers, Java25, pack formats',pack['pack'])
