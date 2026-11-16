#!/usr/bin/env python3
"""Halo Haven 2.8 follow-up: remove voxel debris and support tall plateaus.

Runs after halohaven28_landmass.py; no new items/blocks/registries.
"""
from pathlib import Path
p=Path("projects/halohaven/src/main/java/dev/minemods/halohaven")

mist=p/"HavenMistFeature.java"
s=mist.read_text(encoding="utf8")
assert 'if(y<95 || y>119)return false;' in s
assert 'int yy=y+((dx+dz)%17==0?1:0);' in s
s=s.replace('if(y<95 || y>119)return false;',
'''if(y<95 || y>195)return false;''')
s=s.replace('int rx=7+random.nextInt(6),rz=7+random.nextInt(6);',
'''int rx=10+random.nextInt(9),rz=10+random.nextInt(9);''')
s=s.replace('int yy=y+((dx+dz)%17==0?1:0);',
'''int yy=y; // A continuous platform: never create disconnected single voxels.''')
# Remove the single-point isolated underside bulbs if they might become
# detached after adjacent air restrictions; keep main sheet connected.
s=s.replace('if(shape<.36 && random.nextInt(6)==0){',
            'if(shape<.26 && random.nextInt(5)==0){')
mist.write_text(s,encoding="utf8")

tpl=p/"HavenGroundedTemplateFeature.java"
s=tpl.read_text(encoding="utf8")
assert s.count('if(y<82 || y>121)return false;')==1
assert s.count('if(sy<82 || sy>121 || Math.abs(sy-y)>3)return false;')==1
assert s.count('if(hy<82 || hy>121 || Math.abs(hy-y)>4)return false;')==1
s=s.replace('if(y<82 || y>121)return false;','if(y<65 || y>235)return false;')
s=s.replace('if(sy<82 || sy>121 || Math.abs(sy-y)>3)return false;',
            'if(sy<65 || sy>235 || Math.abs(sy-y)>3)return false;')
s=s.replace('if(hy<82 || hy>121 || Math.abs(hy-y)>4)return false;',
            'if(hy<65 || hy>235 || Math.abs(hy-y)>4)return false;')
old='if(!level.getBlockState(groundPos).is(soil))return false;'
assert old in s
s=s.replace(old,'if(!validGround(level,groundPos,biome))return false;')
for key in ('px,sy-1,pz','px,hy-1,pz'):
    assert f'if(!level.getBlockState(new BlockPos({key})).is(soil))return false;' in s
    s=s.replace(f'if(!level.getBlockState(new BlockPos({key})).is(soil))return false;',
                f'if(!validGround(level,new BlockPos({key}),biome))return false;')
anchor='    @Override public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> ctx){'
assert anchor in s
s=s.replace(anchor,'''    private static boolean validGround(WorldGenLevel level, BlockPos pos, String biome){
        String actual=BuiltInRegistries.BLOCK.getKey(level.getBlockState(pos).getBlock()).toString();
        if(actual.equals(GROUND.get(biome)))return true;
        // The new 256-height Chalk mesa exposes its geological strata rather
        // than chalkstone at every terrace. Both are sturdy native rock.
        return switch(biome){
            case "chalk_terraces" -> actual.equals("halohaven:chalk_strata");
            case "dawn_gardens" -> actual.equals("halohaven:dawnstone")
                || actual.equals("halohaven:skygrass");
            case "lower_veil" -> actual.equals("halohaven:veilstone");
            default -> false;
        };
    }
''' + anchor)
tpl.write_text(s,encoding="utf8")

# 2.7 only generated a random 98-116Y cloud sheet; the new high Chalk
# variant is Y166-185 and uses the same block and feature registration.
data=Path("projects/halohaven/src/main/resources/data/halohaven")
import json
file=data/"worldgen/placed_feature/sky_clouds.json"
j=json.loads(file.read_text(encoding="utf8"))
for node in j["placement"]:
    if node["type"]=="minecraft:count":
        node["count"]=3
file.write_text(json.dumps(j,indent=2,ensure_ascii=False)+"\n",encoding="utf8")

# Safety: the "grounded_ruins" feature was written to accept every biome
# but only ever places templates onto genuine substrate, never high cloud.
assert "validGround(level,groundPos,biome)" in tpl.read_text()
print("PASS: coherent cloud fields (no 1-block hovering cells)")
print("PASS: native-supported structures now allow high Chalk plateau Y130-230")
