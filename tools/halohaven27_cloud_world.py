#!/usr/bin/env python3
"""Halo Haven 2.7: walkable translucent clouds and grounded NBT structures.
Patches generated source AFTER 2.6 scripts, BEFORE Gradle. No edits to prior release.
"""
from pathlib import Path
import json,tarfile,tempfile,random,math
from PIL import Image,ImageDraw
CORE=Path("projects/halohaven")
JAVA=CORE/"src/main/java/dev/minemods/halohaven"
RES=CORE/"src/main/resources"
BUNDLE=Path("projects/halohaven-alpha21/bundle.tar.xz")
def obj(p):return json.loads(p.read_text(encoding="utf8"))
def save(p,x):
    p.parent.mkdir(parents=True,exist_ok=True)
    p.write_text(json.dumps(x,indent=2,ensure_ascii=False)+"\n",encoding="utf8")
def put_source(name,content): (JAVA/name).write_text(content,encoding="utf8")

# The cloud is a genuine solid, walkable block with non-occluding, alpha-blended visuals.
# Using default full-cube collision and a translucency model guarantees no fall-through.
halo=JAVA/"HaloHaven.java"
text=halo.read_text(encoding="utf8")
marker='    public static final DeferredBlock<Block> CLOUDSTONE = BLOCKS.registerSimpleBlock("cloudstone",'
assert marker in text
text=text.replace(marker,'''    public static final DeferredBlock<Block> SKY_MIST = BLOCKS.register("sky_mist",
        () -> new HavenMistBlock(BlockBehaviour.Properties.of().strength(0.35f)
             .noOcclusion().isSuffocating((s,w,p)->false).isViewBlocking((s,w,p)->false)
             .sound(SoundType.WOOL)));
'''+marker)
marker2='    public static final DeferredItem<BlockItem> CLOUDSTONE_ITEM = ITEMS.registerSimpleBlockItem("cloudstone", CLOUDSTONE);'
assert marker2 in text
text=text.replace(marker2,'''    public static final DeferredItem<BlockItem> SKY_MIST_ITEM = ITEMS.registerSimpleBlockItem("sky_mist", SKY_MIST);
'''+marker2)
assert '        HavenBiomeBlocks.register(bus);' in text
text=text.replace('        HavenBiomeBlocks.register(bus);','        HavenBiomeBlocks.register(bus);\n        HavenTerrainFeatures.register(bus);')
halo.write_text(text,encoding="utf8")

put_source("HavenMistBlock.java",'''package dev.minemods.halohaven;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/** Full collision, non-occluding walkable atmosphere; no fragile glass outlines. */
public final class HavenMistBlock extends Block {
    public HavenMistBlock(Properties props){super(props);}
    @Override protected boolean skipRendering(BlockState current, BlockState other, Direction dir){
        return other.is(this) || super.skipRendering(current,other,dir);
    }
}
''')

put_source("HavenTerrainFeatures.java",'''package dev.minemods.halohaven;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredHolder;
public final class HavenTerrainFeatures {
    private HavenTerrainFeatures(){}
    public static final DeferredRegister<Feature<?>> FEATURES=DeferredRegister.create(Registries.FEATURE,HaloHaven.ID);
    public static final DeferredHolder<Feature<?>,HavenMistFeature> SKY_CLOUDS=
        FEATURES.register("sky_clouds",()->new HavenMistFeature(NoneFeatureConfiguration.CODEC));
    public static final DeferredHolder<Feature<?>,HavenGroundedTemplateFeature> GROUNDED_GROVE=
        FEATURES.register("grounded_grove",()->new HavenGroundedTemplateFeature(NoneFeatureConfiguration.CODEC,true));
    public static final DeferredHolder<Feature<?>,HavenGroundedTemplateFeature> GROUNDED_RUINS=
        FEATURES.register("grounded_ruins",()->new HavenGroundedTemplateFeature(NoneFeatureConfiguration.CODEC,false));
    public static void register(IEventBus bus){FEATURES.register(bus);}
}
''')

put_source("HavenMistFeature.java",'''package dev.minemods.halohaven;
import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/** Variable cloud islets in the sky void, physically walkable and transparent.
 * Cloud position is sampled in Y=99..114 and placed only into empty air.
 * Asymmetric lobes preserve a readable boundary while still bridging islands.
 */
public final class HavenMistFeature extends Feature<NoneFeatureConfiguration> {
    private static final java.util.concurrent.atomic.AtomicBoolean FIRST_CLOUD = new java.util.concurrent.atomic.AtomicBoolean(false);
    public HavenMistFeature(Codec<NoneFeatureConfiguration> codec){super(codec);}
    @Override public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> ctx) {
        WorldGenLevel world=ctx.level();
        RandomSource random=ctx.random();
        BlockPos origin=ctx.origin();
        int x=origin.getX(),y=origin.getY(),z=origin.getZ();
        if(y<95 || y>119)return false;
        // Don't lay a transparent carpet over existing island ground or temples.
        if(!world.getBlockState(origin).isAir() || !world.getBlockState(origin.above()).isAir())
            return false;
        int rx=7+random.nextInt(6),rz=7+random.nextInt(6);
        double phase=random.nextDouble()*Math.PI*2.0;
        int placed=0;
        for(int dx=-rx;dx<=rx;dx++)for(int dz=-rz;dz<=rz;dz++){
            double az=dx*Math.cos(phase)+dz*Math.sin(phase);
            double bz=-dx*Math.sin(phase)+dz*Math.cos(phase);
            double distortion=.065*Math.sin(dx*.43+phase)+.08*Math.cos(dz*.57-phase);
            double shape=(az*az)/(rx*rx)+(bz*bz)/(rz*rz)+distortion;
            if(shape>1.0)continue;
            // Thin floating mist sheets with gentle bumps, not solid rectangular rafts.
            int yy=y+((dx+dz)%17==0?1:0);
            BlockPos p=new BlockPos(x+dx,yy,z+dz);
            if(!world.ensureCanWrite(p) || !world.getBlockState(p).isAir())continue;
            world.setBlock(p,HaloHaven.SKY_MIST.get().defaultBlockState(),2);
            placed++;
            if(shape<.36 && random.nextInt(6)==0){
                BlockPos below=p.below();
                if(world.ensureCanWrite(below) && world.getBlockState(below).isAir())
                    world.setBlock(below,HaloHaven.SKY_MIST.get().defaultBlockState(),2);
            }
        }
        if(placed>35 && FIRST_CLOUD.compareAndSet(false,true)){
            // Print exactly once per server for the automated real-worldgen probe.
            System.out.println("HALOHAVEN_CLOUD_GENERATED "+x+" "+(y+1)+" "+z);
        }
        return placed>35;
    }
}
''')

# Native-worldgen features must check the REAL stone surface, not a Y=0 offset
# from a heightmap reading into a void column.
put_source("HavenGroundedTemplateFeature.java",'''package dev.minemods.halohaven;

import com.mojang.serialization.Codec;
import java.util.Map;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.BlockIgnoreProcessor;

/** Renders existing NBT templates only when solid biome-specific land supports them.
 * A one-per-cell anchor prevents duplicate colossal trees and overlapping monuments.
 */
public final class HavenGroundedTemplateFeature extends Feature<NoneFeatureConfiguration>{
    private final boolean grove;
    public HavenGroundedTemplateFeature(Codec<NoneFeatureConfiguration> codec, boolean grove){
        super(codec);this.grove=grove;
    }
    private static final Map<String,String> GROUND=Map.of(
        "dawn_gardens","halohaven:sunlit_loam",
        "chalk_terraces","halohaven:chalkstone",
        "eternal_canopy","halohaven:canopy_loam",
        "ringing_gorges","halohaven:resonant_shale",
        "forgotten_choirs","halohaven:choirstone",
        "lower_veil","halohaven:umbral_silt");
    private static final Map<String,String> IDS=Map.of(
        "dawn_gardens","dawn","chalk_terraces","chalk",
        "eternal_canopy","canopy","ringing_gorges","gorge",
        "forgotten_choirs","choir","lower_veil","veil");

    @Override public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> ctx){
        WorldGenLevel level=ctx.level();
        BlockPos origin=ctx.origin();
        int grid=grove?64:128;
        int cellX=Math.floorDiv(origin.getX(),grid),cellZ=Math.floorDiv(origin.getZ(),grid);
        int anchorX=cellX*grid+(grove?40:72);
        int anchorZ=cellZ*grid+(grove?40:72);
        // Only one generating chunk per region cell is allowed to place a template.
        if((origin.getX()>>4)!=(anchorX>>4) || (origin.getZ()>>4)!=(anchorZ>>4))
            return false;
        int y=level.getHeight(Heightmap.Types.WORLD_SURFACE_WG,anchorX,anchorZ);
        if(y<82 || y>121)return false;
        BlockPos groundPos=new BlockPos(anchorX,y-1,anchorZ);
        String biome=level.getBiome(groundPos).unwrapKey()
                    .map(key->key.location().getPath()).orElse("");
        if(!GROUND.containsKey(biome))return false;
        if(grove && !biome.equals("eternal_canopy"))return false;
        Block soil=BuiltInRegistries.BLOCK.get(ResourceLocation.parse(GROUND.get(biome)));
        if(!level.getBlockState(groundPos).is(soil))return false;
        int radius=grove?11:biome.equals("forgotten_choirs")?15:6;
        for(int dx=-radius;dx<=radius;dx+=radius)for(int dz=-radius;dz<=radius;dz+=radius){
            int px=anchorX+dx,pz=anchorZ+dz;
            int sy=level.getHeight(Heightmap.Types.WORLD_SURFACE_WG,px,pz);
            if(sy<82 || sy>121 || Math.abs(sy-y)>3)return false;
            if(!level.getBlockState(new BlockPos(px,sy-1,pz)).is(soil))return false;
            if(!level.getBiome(new BlockPos(px,sy-1,pz)).unwrapKey().equals(level.getBiome(groundPos).unwrapKey()))return false;
        }
        RandomSource rng=ctx.random();
        String path;
        if(grove){
            path="colossal_canopy_"+(1+rng.nextInt(4));
        }else if(biome.equals("forgotten_choirs") && rng.nextInt(3)!=0){
            String[] choices={"choir_ziggurat","choir_sanctuary","choir_processional_way","choir_ossuary"};
            path=choices[rng.nextInt(choices.length)];
        }else{
            String[] kinds={"house","archive","observatory"};
            path="regional_"+IDS.get(biome)+"_"+kinds[rng.nextInt(kinds.length)];
        }
        Optional<StructureTemplate> found=level.getLevel().getStructureManager()
                   .get(ResourceLocation.fromNamespaceAndPath("halohavenalpha2",path));
        if(found.isEmpty())return false;
        StructureTemplate template=found.get();
        int w=template.getSize().getX(),d=template.getSize().getZ();
        // Enforce all four corners of the construction footprint, not only center.
        int halfX=w/2,halfZ=d/2;
        for(int dx:new int[]{-halfX+3,halfX-3})
            for(int dz:new int[]{-halfZ+3,halfZ-3}){
                int px=anchorX+dx,pz=anchorZ+dz;
                int hy=level.getHeight(Heightmap.Types.WORLD_SURFACE_WG,px,pz);
                if(hy<82 || hy>121 || Math.abs(hy-y)>4)return false;
                if(!level.getBlockState(new BlockPos(px,hy-1,pz)).is(soil))return false;
            }
        // Existing NBT house floors are local Y+3, trees root at local Y+0.
        int baseY=y-(grove?1:3);
        BlockPos start=new BlockPos(anchorX-halfX,baseY,anchorZ-halfZ);
        StructurePlaceSettings placement=new StructurePlaceSettings()
                 .setIgnoreEntities(true)
                 .addProcessor(BlockIgnoreProcessor.STRUCTURE_BLOCK);
        return template.placeInWorld(level,start,start,placement,rng,2);
    }
}
''')

# A 16-pixel meaningful cluster texture with pale semi-transparent banks, no
# empty large expanses and no randomly scattered isolated noise pixels.
tex=RES/"assets/halohaven/textures/block/sky_mist.png"
tex.parent.mkdir(parents=True,exist_ok=True)
im=Image.new("RGBA",(16,16),(218,228,232,104));d=ImageDraw.Draw(im)
for x,y,w,h,rgb in [
 (0,1,5,3,(233,239,242,151)),(6,0,6,3,(245,247,249,132)),
 (11,4,5,4,(198,208,217,105)),(2,5,7,3,(250,251,251,170)),
 (6,9,6,4,(222,235,240,150)),(0,11,5,3,(195,210,221,90)),
 (11,13,5,3,(238,244,246,165)),(4,14,6,2,(245,247,248,140)),
 (8,6,4,2,(190,211,220,80))]:
    d.rectangle((x,y,x+w-1,y+h-1),fill=rgb)
for y in (3,8,13):
    d.line((0,y,15,y),fill=(216,227,232,94))
im.save(tex)
asset=RES/"assets/halohaven"
save(asset/"models/block/sky_mist.json",{
    "parent":"minecraft:block/cube_all","ambientocclusion":False,
    "render_type":"minecraft:translucent",
    "textures":{"all":"halohaven:block/sky_mist"}})
save(asset/"blockstates/sky_mist.json",{"variants":{"":{"model":"halohaven:block/sky_mist"}}})
save(asset/"models/item/sky_mist.json",{"parent":"halohaven:block/sky_mist"})
save(RES/"data/halohaven/loot_table/blocks/sky_mist.json",{"type":"minecraft:block","pools":[
    {"rolls":1,"entries":[{"type":"minecraft:item","name":"halohaven:sky_mist"}]}]})
# Widen the natural island margins, without relaxing one-biome-per-island air seams.
settings_file=RES/"data/halohaven/worldgen/noise_settings/heaven_islands.json"
settings=obj(settings_file)
for key in ("final_density","initial_density_without_jaggedness"):
    root=settings["noise_router"][key]
    touched=[0]
    def nudge(node):
        if isinstance(node,list):
            for val in node:nudge(val)
        elif isinstance(node,dict):
            if (node.get("type")=="minecraft:min" and
                isinstance(node.get("argument2"),dict) and
                node["argument2"].get("type")=="minecraft:add" and
                isinstance(node["argument2"].get("argument2"),dict) and
                node["argument2"]["argument2"].get("type")=="minecraft:mul" and
                node["argument2"]["argument2"].get("argument1")==-1):
                field=node["argument2"]["argument1"]
                node["argument2"]["argument1"]={"type":"minecraft:add",
                   "argument1":field,"argument2":0.072}
                touched[0]+=1
            for val in list(node.values()):nudge(val)
    nudge(root)
    assert touched[0]==6,(key,touched)
save(settings_file,settings)

tab=JAVA/"HavenCreativeTab.java"
creative=tab.read_text(encoding="utf8")
assert 'output.accept(HaloHaven.CLOUDSTONE_ITEM.get());' in creative
creative=creative.replace('output.accept(HaloHaven.CLOUDSTONE_ITEM.get());',
                        'output.accept(HaloHaven.CLOUDSTONE_ITEM.get());\n                output.accept(HaloHaven.SKY_MIST_ITEM.get());')
tab.write_text(creative,encoding="utf8")

# Data-driven placed features on every one of the 6 regions.
for name,feature,placement in [
 ("sky_clouds","sky_clouds",[{"type":"minecraft:count","count":2},
      {"type":"minecraft:in_square"},{"type":"minecraft:height_range",
       "height":{"type":"minecraft:uniform","min_inclusive":{"absolute":98},
                 "max_inclusive":{"absolute":116}}},
      {"type":"minecraft:biome"}]),
 ("grounded_grove","grounded_grove",[{"type":"minecraft:count","count":1},
      {"type":"minecraft:in_square"},
      {"type":"minecraft:heightmap","heightmap":"WORLD_SURFACE_WG"},
      {"type":"minecraft:biome"}]),
 ("grounded_ruins","grounded_ruins",[{"type":"minecraft:count","count":1},
      {"type":"minecraft:in_square"},
      {"type":"minecraft:heightmap","heightmap":"WORLD_SURFACE_WG"},
      {"type":"minecraft:biome"}]),
]:
    save(RES/("data/halohaven/worldgen/configured_feature/"+name+".json"),
         {"type":"halohaven:"+feature,"config":{}})
    save(RES/("data/halohaven/worldgen/placed_feature/"+name+".json"),
         {"feature":"halohaven:"+name,"placement":placement})
biomes=("dawn_gardens","chalk_terraces","eternal_canopy",
        "ringing_gorges","forgotten_choirs","lower_veil")
for biome in biomes:
    p=RES/("data/halohaven/worldgen/biome/"+biome+".json")
    j=obj(p)
    decorations=j["features"][9]
    decorations.insert(0,"halohaven:grounded_ruins")
    if biome=="eternal_canopy":
        decorations.insert(0,"halohaven:grounded_grove")
    decorations.append("halohaven:sky_clouds")
    save(p,j)

# Unregistered / floating jigsaw positions from previous releases must no
# longer generate. Their template NBT files are RETAINED for vetted placement.
with tempfile.TemporaryDirectory() as td:
    root=Path(td)
    with tarfile.open(BUNDLE,"r:xz") as z:
        for entry in z:
            if entry.isfile():
                p=Path(entry.name)
                assert p.parts[0]=="src" and ".." not in p.parts
                dst=root/p;dst.parent.mkdir(parents=True,exist_ok=True)
                dst.write_bytes(z.extractfile(entry).read())
    structure_sets=root/"src/main/resources/data/halohavenalpha2/worldgen/structure_set"
    removed=[]
    for file in list(structure_sets.glob("*.json")):
        # Remove automatic jigsaw locations: no Y=0/void starts.
        removed.append(file.name);file.unlink()
    # Also remove unreferenced jigsaw structures? Keeping their definitions
    # preserves debug access and template pools, without automatic spawning.
    assert len(removed)>=8,removed
    with tarfile.open(BUNDLE,"w:xz",preset=6) as z:
        for path in sorted((root/"src").rglob("*")):
            if path.is_file():z.add(path,arcname=path.relative_to(root).as_posix())
print("PASS: registered walkable translucent mist, cloud fields and substrate-validated template features")
print("PASS: blocked",len(removed),"legacy jigsaw structure sets; kept all NBTs")
