#!/usr/bin/env python3
"""WorldGen-only v2.4; run after the v2.3 world generator, before Gradle."""
from pathlib import Path
import json
D=Path("projects/halohaven/src/main/resources/data/halohaven")
J=Path("projects/halohaven/src/main/java/dev/minemods/halohaven")
def read(p):return json.loads(p.read_text())
def save(p,o):p.parent.mkdir(parents=True,exist_ok=True);p.write_text(json.dumps(o,indent=2)+"\n")
def noise(n,xz,y=0):return {"type":"minecraft:noise","noise":n,"xz_scale":xz,"y_scale":y}
def add(a,b):return {"type":"minecraft:add","argument1":a,"argument2":b}
def mul(a,b):return {"type":"minecraft:mul","argument1":a,"argument2":b}
def absn(a):return {"type":"minecraft:abs","argument":a}
def clamp(a):return {"type":"minecraft:clamp","input":a,"min":0,"max":1}
def rc(a,lo,hi,yes,no):return {"type":"minecraft:range_choice","input":a,"min_inclusive":lo,"max_exclusive":hi,"when_in_range":yes,"when_out_of_range":no}
def plus(*args):
 r=args[0]
 for a in args[1:]:r=add(r,a)
 return r
def pos(a):return clamp(mul(3.3,add(a,-0.19)))
def neg(a):return clamp(mul(-3.3,add(a,0.19)))
def mid(a):return clamp(mul(3.1,add(0.56,mul(-1,absn(a)))))
T="halohaven:climate_temperature";V="halohaven:climate_vegetation"
warm,cool,cold=pos(T),mid(T),neg(T)
humid,dry=pos(V),neg(V)
weights=[mul(warm,humid),mul(warm,dry),mul(cool,humid),
         mul(cool,dry),mul(cold,dry),mul(cold,humid)]
# Irregular lotus petal edges, flat central area and low ring ramp.
lotus=clamp(mul(2.85,plus(absn(noise("minecraft:continentalness",1.45)),
                       mul(.73,absn(noise("minecraft:erosion",3.8))),-.26)))
dawn=plus(-.092,mul(.238,lotus),mul(-.056,absn(noise("minecraft:vegetation",3.15))))
# Eight variable plateau levels. Hard shelves are intentional only in Chalk.
tiernoise=plus(mul(.89,noise("minecraft:erosion",1.72)),
              mul(.22,noise("minecraft:continentalness",3.31)))
levels=[(-.74,-.23),(-.53,-.15),(-.33,-.07),(-.1,.008),(.12,.09),(.36,.175),(.61,.268)]
tier=.35
for limit,value in reversed(levels):tier=rc(tiernoise,-2,limit,value,tier)
chalk=plus(tier,mul(.04,noise("minecraft:vegetation",3.15)))
canopy=plus(.095,mul(.055,noise("minecraft:vegetation",2.65)))
# Deep continuous fissures along noise zero-lines, flanked by rock ridges.
fracture=absn(noise("minecraft:erosion",1.4))
rift=rc(fracture,0,.105,-.62,rc(fracture,0,.185,-.32,0))
gorge=plus(-.045,rift,mul(.24,absn("minecraft:overworld/ridges")),
           mul(.075,noise("minecraft:continentalness",3.6)))
choir=plus(-.015,mul(.105,noise("minecraft:continentalness",2.3)))
# 3D spatial carving within Veil (y_scale != 0).
caves=absn(noise("minecraft:spaghetti_3d_1",1.55,1.35))
veils=rc(caves,0,.18,-.58,rc(caves,0,.29,-.26,0))
veil=plus(-.135,mul(.095,noise("minecraft:erosion",2.35)),veils)
morph=plus(*(mul(w,v) for w,v in zip(weights,[dawn,chalk,canopy,gorge,choir,veil])))
settings_path=D/"worldgen/noise_settings/heaven_islands.json"
settings=read(settings_path)
for key in ("final_density","initial_density_without_jaggedness"):
 settings["noise_router"][key]=add(settings["noise_router"][key],morph)
save(settings_path,settings)
# A pale gray-white translucent waterfall tint through vanilla fluid.
dawnpath=D/"worldgen/biome/dawn_gardens.json"
b=read(dawnpath)
b["effects"]["water_color"]=int("D9DEE2",16)
b["effects"]["water_fog_color"]=int("C2CBD1",16)
b["features"][9].append("halohaven:dawn_waterfalls")
save(dawnpath,b)
veilpath=D/"worldgen/biome/lower_veil.json"
b=read(veilpath)
b["effects"].update({"fog_color":int("343041",16),"sky_color":int("383445",16),
 "water_color":int("554D70",16),"water_fog_color":int("302B41",16),
 "grass_color":int("4C4D68",16),"foliage_color":int("53526F",16)})
save(veilpath,b)
# Dedicated placed feature; no added blocks, items, liquids, or textures.
save(D/"worldgen/configured_feature/dawn_waterfall.json",
 {"type":"halohaven:dawn_waterfall","config":{}})
save(D/"worldgen/placed_feature/dawn_waterfalls.json",
 {"feature":"halohaven:dawn_waterfall",
  "placement":[{"type":"minecraft:rarity_filter","chance":3},
               {"type":"minecraft:in_square"},
               {"type":"minecraft:heightmap","heightmap":"WORLD_SURFACE_WG"},
               {"type":"minecraft:biome"}]})
(J/"DawnWaterfallFeature.java").write_text("""package dev.minemods.halohaven;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.world.level.material.Fluids;
/** Natural sky-island spring; vanilla transparent water gets the Dawn biome tint. */
public final class DawnWaterfallFeature extends Feature<NoneFeatureConfiguration> {
    public DawnWaterfallFeature(){super(NoneFeatureConfiguration.CODEC);}
    @Override public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context){
        WorldGenLevel world=context.level();
        RandomSource rng=context.random();
        BlockPos start=context.origin();
        for(int attempt=0;attempt<10;attempt++){
            int x=start.getX()+rng.nextInt(7)-3,z=start.getZ()+rng.nextInt(7)-3;
            int surface=world.getHeight(Heightmap.Types.WORLD_SURFACE_WG,x,z)-1;
            if(surface<47 || surface>121)continue;
            BlockPos floor=new BlockPos(x,surface,z);
            if(!world.getBlockState(floor).is(HavenBiomeBlocks.SUNLIT_LOAM.get()))continue;
            if(!world.getBlockState(floor.above()).isAir())continue;
            Direction direction=Direction.Plane.HORIZONTAL.getRandomDirection(rng);
            BlockPos behind=floor.relative(direction.getOpposite());
            BlockPos edge=floor.relative(direction,2);
            int edgeHeight=world.getHeight(Heightmap.Types.WORLD_SURFACE_WG,edge.getX(),edge.getZ())-1;
            if(surface-edgeHeight<9)continue;
            if(!world.getBlockState(behind).isSolidRender(world,behind))continue;
            BlockPos source=floor.above(),upper=behind.above();
            if(!world.getBlockState(upper).isAir())continue;
            world.setBlock(upper,Blocks.WATER.defaultBlockState(),3);
            world.setBlock(source,Blocks.WATER.defaultBlockState(),3);
            world.scheduleTick(source,Fluids.WATER,1);
            world.scheduleTick(upper,Fluids.WATER,1);
            return true;
        }
        return false;
    }
}
""")
halo=J/"HaloHaven.java";s=halo.read_text()
needle="import net.minecraft.core.registries.Registries;"
assert needle in s
s=s.replace(needle,needle+"""
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.neoforged.neoforge.registries.DeferredHolder;""")
needle="    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(ID);"
assert needle in s
s=s.replace(needle,needle+"""
    public static final DeferredRegister<Feature<?>> FEATURES =
        DeferredRegister.create(BuiltInRegistries.FEATURE,ID);
    public static final DeferredHolder<Feature<?>, DawnWaterfallFeature> DAWN_WATERFALL_FEATURE =
        FEATURES.register("dawn_waterfall",DawnWaterfallFeature::new);""")
needle="        ITEMS.register(bus);";assert needle in s
s=s.replace(needle,needle+"\n        FEATURES.register(bus);")
halo.write_text(s)
print("2.4 six distinct terrain profiles, pale-water Dawn falls, eight Chalk shelves, deep Gorge rifts, Veil 3D caves")
