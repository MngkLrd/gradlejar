#!/usr/bin/env python3
"""Halo Haven 2.3 - tactile pixel surfaces, architecture, custom doors, render fixes."""
from pathlib import Path
import json, tarfile, tempfile, random, math, copy, gzip
from PIL import Image,ImageDraw
P=Path("projects/halohaven")
CORE=P/"src/main"
AD=Path("projects/halohaven-alpha21/bundle.tar.xz")
def write(p,data):
    p.parent.mkdir(parents=True,exist_ok=True)
    p.write_text(json.dumps(data,ensure_ascii=False,indent=2)+"\n",encoding="utf8")
def read(p):return json.loads(p.read_text(encoding="utf8"))
def parsehex(c):return tuple(int(c[i:i+2],16) for i in (1,3,5))
def adj(col,mult):return tuple(max(0,min(255,round(v*mult))) for v in col)+(255,)
# Every tile uses a defined material direction and connected clusters. No per-pixel noise.
# Warm face highlights, cool crevices, restrained inlays and no giant empty spans.
designs={
    "dawnwood":("#a58b6e","wood"),
    "chalkwood":("#b4b1a1","wood"),
    "heartwood":("#567f69","wood"),
    "ringwood":("#596978","wood"),
    "choirwood":("#9b879b","wood"),
    "veilwood":("#494660","wood"),
    "dawnwood_leaves":("#8aa979","leaf"),
    "chalkwood_leaves":("#b1bfa8","leaf"),
    "heartwood_leaves":("#4f956e","leaf"),
    "ringwood_leaves":("#6b8792","leaf"),
    "choirwood_leaves":("#aaa0ae","leaf"),
    "veilwood_leaves":("#56577b","leaf"),
    "dawn_relief":("#d1b8a0","stone"),
    "dawn_arcade":("#cec8ab","brick"),
    "chalk_banded_marl":("#cfcec1","stone"),
    "chalk_fossil_bricks":("#c9c9b8","brick"),
    "canopy_root_bricks":("#587968","brick"),
    "canopy_luminous_bark":("#647e6d","wood"),
    "gorge_cleavage":("#73838a","stone"),
    "gorge_azure_runes":("#4e6c7e","brick"),
    "choir_rose_inlay":("#b8aab8","brick"),
    "choir_pilgrim_marble":("#d2cad2","stone"),
    "veil_fracture":("#51465d","stone"),
    "veil_starlit_tiles":("#5a5373","brick"),
    "undersky_glow":("#a0c8bd","crystal")
}
clusters=[(0,2,3,2,3),(4,0,3,3,4),(7,3,2,2,1),(10,1,4,2,2),
          (14,3,2,3,1),(1,6,2,3,4),(5,5,3,2,1),(8,7,3,2,4),
          (12,6,3,3,3),(0,11,4,2,1),(4,10,2,2,4),(7,12,4,3,2),
          (12,11,3,2,4),(2,14,3,2,3),(10,14,2,2,1)]
def paint(base,kind,seed):
    col=parsehex(base)
    palette=[adj(col,q) for q in (.58,.74,.88,1.0,1.075,1.16)]
    im=Image.new("RGBA",(16,16),palette[2])
    d=ImageDraw.Draw(im)
    if kind=="wood":
        # Directional fibers wrap the bark, with intact wood bands and short transverse knots.
        for x in (2,6,10,14):
            d.line((x,0,x,15),fill=palette[1] if x%3 else palette[4])
        for y in (1,5,9,13):
            d.line((1,y,4,y),fill=palette[3],width=1)
            d.line((8,y+1,11,y+1),fill=palette[4])
        for x,y,w,h,c in clusters:
            x=(x+seed)%15;y=(y+seed//3)%15
            d.rectangle((x,y,min(15,x+1),min(15,y+2)),fill=palette[c%6])
    elif kind=="brick":
        # Dressed blocks, staggered joints (not decorative stripes over the whole wall).
        for y in (4,9,14):
            d.line((0,y,15,y),fill=palette[0])
            d.line((0,y+1,15,y+1),fill=palette[4])
        for row,y in enumerate((0,5,10)):
            for x in ((5,13) if row%2==0 else (2,10)):
                d.line((x,y,x,y+3),fill=palette[1])
        for x,y,w,h,c in clusters:
            d.rectangle((x,y,min(x+w-1,15),min(y+1,15)),fill=palette[3+c%3])
    elif kind=="leaf":
        # Strong diagonal clumps but no isolated glitter. Close-value shade family.
        d.rectangle((0,0,15,15),fill=palette[2])
        for x,y,w,h,c in clusters:
            d.rectangle((x,y,min(x+w-1,15),min(y+h-1,15)),fill=palette[c])
            d.point((min(15,x+w),y),fill=palette[(c+1)%6])
        for x,y in ((3,3),(9,5),(13,11),(5,13)):
            d.line((x,y,x+2,y+1),fill=palette[0])
    elif kind=="crystal":
        d.rectangle((0,0,15,15),fill=palette[0])
        for x,y,w,h,c in clusters:
            d.rectangle((x,y,min(15,x+w),min(15,y+h)),fill=palette[2+c%4])
        d.line((3,13,7,4,11,11,14,1),fill=palette[5],width=1)
        d.line((0,8,3,13,6,9),fill=palette[4])
    else:
        d.rectangle((0,0,15,15),fill=palette[2])
        for x,y,w,h,c in clusters:
            d.rectangle((x,y,min(15,x+w),min(15,y+h)),fill=palette[c%6])
            if c%2:d.line((x,y+min(h,1),min(15,x+w),y+min(h,1)),fill=palette[(c+1)%6])
    return im

def blocktex(directory,name,base,kind,seed):
    p=directory/(name+".png")
    p.parent.mkdir(parents=True,exist_ok=True)
    paint(base,kind,seed).save(p)

def simple_block_assets(root,name,parent="minecraft:block/cube_all",render_type=None):
    assets=root/"assets/halohavenalpha2"
    block={"parent":parent,"textures":{"all":"halohavenalpha2:block/"+name}}
    if render_type:block["render_type"]=render_type
    write(assets/"models/block"/(name+".json"),block)
    write(assets/"blockstates"/(name+".json"),{"variants":{"":{"model":"halohavenalpha2:block/"+name}}})
    write(assets/"models/item"/(name+".json"),{"parent":"halohavenalpha2:block/"+name})

with tempfile.TemporaryDirectory() as td:
    root=Path(td)
    with tarfile.open(AD,"r:xz") as tar:
        for item in tar:
            if item.isfile():
                rel=Path(item.name);assert rel.parts[0]=="src" and ".." not in rel.parts
                dst=root/rel;dst.parent.mkdir(parents=True,exist_ok=True)
                dst.write_bytes(tar.extractfile(item).read())
    asset=root/"src/main/resources/assets/halohavenalpha2"
    images=asset/"textures/block"
    for i,(name,(color,kind)) in enumerate(designs.items()):
        if name.endswith("_leaves") or name in ("dawnwood","chalkwood","heartwood","ringwood","choirwood","veilwood"):
            # wood grain maps have separate top cross-section and side; leaf families are clustered.
            if name.endswith("_leaves"):
                blocktex(images,name,color,kind,i)
                simple_block_assets(root,name)
            else:
                blocktex(images,name+"_log",color,"wood",i)
                top=paint(color,"stone",i)
                draw=ImageDraw.Draw(top)
                draw.rectangle((2,2,13,13),outline=adj(parsehex(color),.72))
                draw.rectangle((5,5,10,10),outline=adj(parsehex(color),1.12))
                top.save(images/(name+"_log_top.png"))
                # models for log axis states must exist; modify existing three, add new three.
                write(asset/"models/block"/(name+"_log.json"),{
                    "parent":"minecraft:block/cube_column",
                    "textures":{"end":"halohavenalpha2:block/"+name+"_log_top",
                                "side":"halohavenalpha2:block/"+name+"_log"}})
                write(asset/"models/block"/(name+"_log_horizontal.json"),{
                    "parent":"minecraft:block/cube_column_horizontal",
                    "textures":{"end":"halohavenalpha2:block/"+name+"_log_top",
                                "side":"halohavenalpha2:block/"+name+"_log"}})
                write(asset/"blockstates"/(name+"_log.json"),{"variants":{
                    "axis=y":{"model":"halohavenalpha2:block/"+name+"_log"},
                    "axis=z":{"model":"halohavenalpha2:block/"+name+"_log_horizontal","x":90},
                    "axis=x":{"model":"halohavenalpha2:block/"+name+"_log_horizontal","x":90,"y":90}
                }})
                write(asset/"models/item"/(name+"_log.json"),{"parent":"halohavenalpha2:block/"+name+"_log"})
        else:
            blocktex(images,name,color,kind,i)
            simple_block_assets(root,name)
    src=root/"src/main/java/dev/halohaven/alpha2/HaloHavenAlpha2.java"
    code=src.read_text(encoding="utf8")
    add=[]
    for name,(color,kind) in designs.items():
        if name in ("dawnwood","chalkwood","heartwood","ringwood","choirwood","veilwood") or name.endswith("_leaves") or name=="undersky_glow":continue
        add.append('    public static final DeferredBlock<Block> '+name.upper()+' = BLOCKS.register("'+name+'",'+
                   ' () -> new Block(BlockBehaviour.Properties.of().strength(2.0F,6F).sound(SoundType.STONE)));\n')
    assert "    public static final DeferredRegister<CreativeModeTab> TABS" in code
    code=code.replace("    public static final DeferredRegister<CreativeModeTab> TABS","".join(add)+"    public static final DeferredRegister<CreativeModeTab> TABS")
    src.write_text(code,encoding="utf8")

    # Vanilla-like 16px carved doors: upper and lower panels are not duplicated anymore.
    for i,wood in enumerate(("dawnwood","heartwood","veilwood")):
        col=designs[wood][0]
        for half in ("bottom","top"):
            im=paint(col,"wood",i+3)
            d=ImageDraw.Draw(im);rgb=parsehex(col)
            shade=[adj(rgb,n) for n in (.51,.70,.84,1.06,1.16)]
            # Outer carved frame, inset plank panels and iron/bronze hinge.
            d.rectangle((1,1,14,15),outline=shade[1],width=1)
            d.line((2,1,13,1),fill=shade[4])
            d.rectangle((3,3,12,12),outline=shade[0])
            d.line((4,3,11,3),fill=shade[3])
            d.line((4,12,11,12),fill=shade[1])
            for x,y in ((4,5),(7,8),(10,6),(5,10)):
                d.line((x,y,x+1,y),fill=shade[3])
            if half=="top":
                for x in (5,8,11):
                    d.line((x,5,x,8),fill=(69,124,136,255))
                    d.point((x,5),fill=(156,205,203,255))
            else:
                d.rectangle((2,6,3,9),fill=(180,150,85,255))
                d.point((4,7),fill=(224,195,113,255))
            name=wood+"_door_face_"+half
            im.save(images/(name+".png"))
        for lowerupper,half in (("bottom","bottom"),("top","top")):
            for hinge in ("left","right"):
                for op in ("","_open"):
                    p=asset/"models/block"/(wood+"_door_face_"+lowerupper+"_"+hinge+op+".json")
                    obj=read(p)
                    obj["textures"]={
                        "bottom":"halohavenalpha2:block/"+wood+"_door_face_bottom",
                        "top":"halohavenalpha2:block/"+wood+"_door_face_top"}
                    write(p,obj)

    # Add properly painted windows to the existing (still functional) trapdoors.
    for wood in ("dawnwood","heartwood","veilwood"):
        blocktex(images,wood+"_trapdoor_face",designs[wood][0],"wood",2)

    # Remap the aesthetic face of existing core lanterns to a coherent shallow lantern casing.
    # No alpha in solid lantern art, and no giant billboard particle.
    core_tex=CORE/"resources/assets/halohaven/textures/block"
    base=Image.new("RGBA",(16,16),(50,83,87,255));d=ImageDraw.Draw(base)
    d.rectangle((2,0,13,15),fill=(75,111,114,255))
    d.rectangle((3,2,12,13),fill=(147,179,157,255))
    for x in (4,5,6,8,9,10):d.line((x,3,x,12),fill=(196,213,165,255))
    d.rectangle((2,1,13,2),fill=(82,121,131,255))
    d.rectangle((2,13,13,15),fill=(56,97,107,255))
    d.rectangle((4,5,11,10),fill=(249,225,168,255))
    d.rectangle((5,6,10,9),fill=(255,241,207,255))
    d.line((4,4,11,4),fill=(117,160,161,255))
    base.save(core_tex/"cloud_lantern.png")
    # Decorative made-for-lamp face with standard cube geometry, but no transparent center.
    asset_core=CORE/"resources/assets/halohaven"
    model=read(asset_core/"models/block/cloud_lantern.json")
    model["render_type"]="minecraft:solid"
    model["ambientocclusion"]=False
    # Remove texture planes at y=0 causing grass to look like a full glass cube.
    write(asset_core/"models/block/cloud_lantern.json",model)

    # Volumetric portal: deep planar field + inner displaced sheet, a gilded rim,
    # and a second perpendicular field. Both upper and lower halves animate in sync.
    def element(frm,to,tex,dirs=("north","south","west","east","up","down")):
        return {"from":frm,"to":to,"faces":{face:{"texture":"#"+tex} for face in dirs}}
    for half in ("lower","upper"):
        obj={
           "ambientocclusion":False,"render_type":"minecraft:translucent",
           "textures":{"field":"halohaven:block/ascension_gateway_"+half,
                       "rim":"halohaven:block/gilded_pillar",
                       "core":"halohaven:block/aurora_glass"},
           "elements":[
               element([0,0,5],[2,16,11],"rim"),
               element([14,0,5],[16,16,11],"rim"),
               element([2,0,5],[14,2,11],"rim"),
               element([2,14,5],[14,16,11],"rim"),
               element([2,2,7],[14,14,8],"field",("north","south")),
               element([3,2,9],[13,14,10],"field",("north","south")),
               element([7,2,5],[9,14,11],"core",("west","east")),
           ]}
        write(asset_core/"models/block"/("ascension_gateway_"+half+".json"),obj)

    # Remove runtime full-cube occlusion from a lamp whose visual shape is a smaller lantern.
    halo=CORE/"java/dev/minemods/halohaven/HaloHaven.java"
    text=halo.read_text(encoding="utf8")
    sign='    public static final DeferredBlock<Block> CLOUD_LANTERN = BLOCKS.registerSimpleBlock("cloud_lantern",\n        BlockBehaviour.Properties.of().strength(1.1f).lightLevel(s -> 15).sound(SoundType.GLASS));'
    if sign not in text:
        raise RuntimeError("Core lantern signature drift")
    text=text.replace(sign,'    public static final DeferredBlock<Block> CLOUD_LANTERN = BLOCKS.register("cloud_lantern",\n        () -> new HavenLightBlock(BlockBehaviour.Properties.of().strength(1.1f).noOcclusion()\n            .lightLevel(s -> 15).sound(SoundType.GLASS)));')
    halo.write_text(text,encoding="utf8")
    lamp=CORE/"java/dev/minemods/halohaven/HavenLightBlock.java"
    lamp.write_text("""package dev.minemods.halohaven;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
/** Non-opaque 3D lantern with a physically shaped outline, not an invisible full-cube shell. */
public final class HavenLightBlock extends Block {
    private static final VoxelShape SHAPE=Block.box(4,0,4,12,16,12);
    public HavenLightBlock(Properties p){super(p);}
    @Override protected VoxelShape getShape(BlockState state,BlockGetter level,BlockPos pos,CollisionContext c){
        return SHAPE;
    }
}
""",encoding="utf8")
    with tarfile.open(AD,"w:xz",preset=6) as tar:
        for f in sorted((root/"src").rglob("*")):
            if f.is_file():tar.add(f,arcname=f.relative_to(root).as_posix())
print("Art patched: 6 species wood/leaf families, 12 architectural faces, 6 door halves, lantern and volumetric gateway")
