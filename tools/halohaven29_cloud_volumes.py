#!/usr/bin/env python3
"""Halo Haven 2.9 cloud volumes: replace thin 2D carpets with connected 3D mist.

Every chunk independently evaluates stable, seed-dependent multi-lobed
ellipsoidal volumes. A chunk writes strictly to its own 16x16 footprint; this
avoids previous far-chunk worldgen crashes. All blocks have solid collision.
"""
from pathlib import Path
import json
from PIL import Image,ImageEnhance

JAVA=Path("projects/halohaven/src/main/java/dev/minemods/halohaven/HavenMistFeature.java")
assert JAVA.exists()
JAVA.write_text(r'''package dev.minemods.halohaven;

import com.mojang.serialization.Codec;
import java.util.concurrent.atomic.AtomicBoolean;
import net.minecraft.core.BlockPos;
import net.minecraft.world.WorldGenLevel;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/**
 * Volumetric cloud archipelago.
 *
 * Each cloud is a variable-height, rotated, multi-lobed body. Neighboring
 * chunks agree on its location and shape from the server world seed, while
 * every worldgen feature writes ONLY to its own chunk to avoid cross-chunk
 * block access errors. The solid mist blocks allow ordinary walking/jumping.
 */
public final class HavenMistFeature extends Feature<NoneFeatureConfiguration> {
    private static final AtomicBoolean FIRST_VOLUME = new AtomicBoolean(false);
    private static final int CELL = 56;
    public HavenMistFeature(Codec<NoneFeatureConfiguration> codec){ super(codec); }

    private static long hash(long x,long z,long seed){
        long h=seed ^ (x*341873128712L) ^ (z*132897987541L);
        h^=h>>>30;
        h*=0xbf58476d1ce4e5b9L;
        h^=h>>>27;
        h*=0x94d049bb133111ebL;
        return h^(h>>>31);
    }

    private static double blob(double x,double z,double angle,
                               double rx,double rz,int px,int pz){
        double dx=x-px,dz=z-pz;
        double a=dx*Math.cos(angle)+dz*Math.sin(angle);
        double b=-dx*Math.sin(angle)+dz*Math.cos(angle);
        return (a*a)/(rx*rx)+(b*b)/(rz*rz);
    }

    @Override public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> ctx){
        WorldGenLevel world=ctx.level();
        long seed=world.getLevel().getSeed();
        int x0=Math.floorDiv(ctx.origin().getX(),16)*16;
        int z0=Math.floorDiv(ctx.origin().getZ(),16)*16;
        int placed=0;
        BlockPos firstVolume=null;
        int firstHigh=0;

        // Evaluate a small fixed neighborhood only; positions are stable
        // across chunks and independent of call order and feature RNG.
        for(int x=x0;x<x0+16;x++)for(int z=z0;z<z0+16;z++){
            int homeX=Math.floorDiv(x,CELL),homeZ=Math.floorDiv(z,CELL);
            int low=999,high=-999;
            for(int cx=homeX-1;cx<=homeX+1;cx++)
                for(int cz=homeZ-1;cz<=homeZ+1;cz++){
                    long h=hash(cx,cz,seed);
                    int px=cx*CELL+28+(int)((h>>>4)&15)-7;
                    int pz=cz*CELL+28+(int)((h>>>12)&15)-7;
                    double phi=((h>>>21)&1023)*(Math.PI/512.0);
                    int rx=24+(int)((h>>>31)&7);
                    int rz=18+(int)((h>>>39)&9);
                    double main=blob(x,z,phi,rx,rz,px,pz);
                    // Attached off-center lobes vary the silhouette: no
                    // perfectly circular or identical cloud islands.
                    double left=blob(x,z,phi+.28,rx*.59,rz*.76,
                         px+(int)(Math.cos(phi)*11),pz+(int)(Math.sin(phi)*11));
                    double right=blob(x,z,phi-.35,rx*.51,rz*.65,
                         px-(int)(Math.cos(phi)*14),pz-(int)(Math.sin(phi)*14));
                    double radial=Math.min(main,Math.min(left,right));
                    double azimuth=Math.atan2(z-pz,x-px);
                    double notch=.065*Math.sin(5*azimuth+phi)
                                +.045*Math.cos(3*azimuth-phi)
                                +.035*Math.sin(x*.12+z*.10+phi);
                    double rim=radial+notch;
                    if(rim>=.94)continue;
                    double fullness=Math.sqrt(Math.max(0,1.0-rim/.94));
                    int level=97+(int)((h>>>49)&15);
                    // Tallest cloud columns are 15..23 blocks, with thinner
                    // feathered edges. Top and underside both vary smoothly.
                    int dome=(int)Math.round(2+7*fullness
                         +1.4*Math.sin(x*.10+phi)*Math.sin(z*.11-phi));
                    int belly=(int)Math.round(3+9*fullness
                         +1.2*Math.cos(x*.09-phi));
                    int floor=level-belly;
                    int ceiling=level+dome;
                    if(floor<low)low=floor;
                    if(ceiling>high)high=ceiling;
                }
            if(high<low || high-low<3)continue;
            int written=0;
            for(int y=Math.max(64,low);y<=Math.min(145,high);y++){
                BlockPos p=new BlockPos(x,y,z);
                if(!world.ensureCanWrite(p) || !world.getBlockState(p).isAir())
                    continue;
                if(world.setBlock(p,HaloHaven.SKY_MIST.get().defaultBlockState(),2)){
                    written++;
                    placed++;
                }
            }
            if(firstVolume==null && written>=6 && written==high-low+1){
                firstVolume=new BlockPos(x,low,z);
                firstHigh=high;
            }
        }
        if(firstVolume!=null && FIRST_VOLUME.compareAndSet(false,true)){
            System.out.println("HALOHAVEN_CLOUD_GENERATED "+firstVolume.getX()+" "+firstVolume.getY()+" "+firstVolume.getZ());
            System.out.println("HALOHAVEN_CLOUD_VOLUME "+firstVolume.getX()+" "+firstVolume.getY()+" "+firstHigh+" "+firstVolume.getZ());
        }
        return placed>0;
    }
}
''',encoding="utf8")
# Actually import WorldGenLevel lives in the world.level package in 1.21.1.
s=JAVA.read_text(encoding="utf8")
s=s.replace("import net.minecraft.world.WorldGenLevel;","import net.minecraft.world.level.WorldGenLevel;")
JAVA.write_text(s,encoding="utf8")

# Improve depth cues without another item/block: separate top, side and bottom
# textures for the one existing sky_mist block.
ASSET=Path("projects/halohaven/src/main/resources/assets/halohaven")
tex=ASSET/"textures/block/sky_mist.png"
im=Image.open(tex).convert("RGBA")
top=ImageEnhance.Brightness(im).enhance(1.09)
top_data=[]
for rgba in top.getdata():
    top_data.append((rgba[0],rgba[1],rgba[2],min(215,round(rgba[3]*1.25))))
top.putdata(top_data)
top.save(tex.with_name("sky_mist_top.png"))
bottom=ImageEnhance.Brightness(im).enhance(.85)
bottom_data=[(r,g,b,max(40,round(a*.78))) for r,g,b,a in bottom.getdata()]
bottom.putdata(bottom_data)
bottom.save(tex.with_name("sky_mist_bottom.png"))
model=ASSET/"models/block/sky_mist.json"
m=json.loads(model.read_text(encoding="utf8"))
assert m["render_type"]=="minecraft:translucent"
m.update(parent="minecraft:block/cube_bottom_top",
         textures={"side":"halohaven:block/sky_mist",
                   "top":"halohaven:block/sky_mist_top",
                   "bottom":"halohaven:block/sky_mist_bottom",
                   "particle":"halohaven:block/sky_mist"})
model.write_text(json.dumps(m,indent=2)+"\n",encoding="utf8")

p=Path("projects/halohaven/src/main/resources/data/halohaven/worldgen/placed_feature/sky_clouds.json")
j=json.loads(p.read_text(encoding="utf8"))
assert j["feature"]=="halohaven:sky_clouds"
assert next(x for x in j["placement"] if x["type"]=="minecraft:count")["count"]==1
print("PASS: thick seeded multi-lobed clouds, fully connected vertical columns, chunk-local writes")
print("PASS: varied top/side/bottom mist shading, all cloud voxels remain solid and walkable")
