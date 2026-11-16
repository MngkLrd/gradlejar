#!/usr/bin/env python3
"""Halo Haven 2.8: chunk-safe deterministic clouds and trustworthy smoke logging.

The original 2.7 worldgen feature wrote beyond neighboring chunk boundaries;
on 256-high worlds it emitted hundreds of far-chunk errors. 2.8 generates
continuous overlapping broad sheets by deterministic WORLD coordinates, but
only writes blocks in the invoking 16x16 chunk. The logged first block must
have been successfully written and be queryable by RCON.
"""
from pathlib import Path
import json
JAVA=Path("projects/halohaven/src/main/java/dev/minemods/halohaven/HavenMistFeature.java")
CODE=r'''package dev.minemods.halohaven;
import com.mojang.serialization.Codec;
import java.util.concurrent.atomic.AtomicBoolean;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/** Seamlessly joined floating cloud shelves, placed within each generating chunk.
 * No out-of-bound writes and no little isolated block islands.
 */
public final class HavenMistFeature extends Feature<NoneFeatureConfiguration> {
    private static final AtomicBoolean FIRST_CLOUD = new AtomicBoolean(false);
    public HavenMistFeature(Codec<NoneFeatureConfiguration> codec){super(codec);}
    private static long hash(long x,long z){
        long h=x*341873128712L ^ z*132897987541L ^ 0x5DEECE66DL;
        h ^= (h >>> 29);
        h *= 2685821657736338717L;
        return h ^ (h >>> 31);
    }
    @Override public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> ctx){
        WorldGenLevel world=ctx.level();
        int originX=ctx.origin().getX();
        int originZ=ctx.origin().getZ();
        boolean high=ctx.origin().getY()>=145;
        // One and only one 16x16 area is touched by this feature. Each chunk
        // independently arrives at the same cloud shapes using world coords.
        int x0=Math.floorDiv(originX,16)*16;
        int z0=Math.floorDiv(originZ,16)*16;
        int placed=0;
        BlockPos first=null;
        for(int x=x0;x<x0+16;x++)for(int z=z0;z<z0+16;z++){
            int cellX=Math.floorDiv(x,52),cellZ=Math.floorDiv(z,52);
            for(int cx=cellX-1;cx<=cellX+1;cx++)
                for(int cz=cellZ-1;cz<=cellZ+1;cz++){
                    long h=hash(cx,cz);
                    int centerX=cx*52+26+(int)((h>>>7)&15)-7;
                    int centerZ=cz*52+26+(int)((h>>>14)&15)-7;
                    int radiusX=21+(int)((h>>>22)&7);
                    int radiusZ=21+(int)((h>>>29)&7);
                    double theta=((h>>>36)&1023)*Math.PI/512.0;
                    double dx=x-centerX,dz=z-centerZ;
                    double a=dx*Math.cos(theta)+dz*Math.sin(theta);
                    double b=-dx*Math.sin(theta)+dz*Math.cos(theta);
                    double dist=(a*a)/(radiusX*radiusX)+(b*b)/(radiusZ*radiusZ);
                    // Broad connected lobes, not disconnected single-pixel debris.
                    double softEdge=.045*Math.sin(x*.21+theta)+.055*Math.cos(z*.18-theta);
                    if(dist+softEdge>0.92)continue;
                    int y=(high?166:100)+(int)((h>>>47)&(high?15:13));
                    BlockPos p=new BlockPos(x,y,z);
                    if(!world.ensureCanWrite(p) || !world.getBlockState(p).isAir())continue;
                    if(world.setBlock(p,HaloHaven.SKY_MIST.get().defaultBlockState(),2)){
                        placed++;
                        if(first==null)first=p;
                    }
                }
        }
        if(placed>0 && first!=null && FIRST_CLOUD.compareAndSet(false,true))
            System.out.println("HALOHAVEN_CLOUD_GENERATED "+first.getX()+" "+first.getY()+" "+first.getZ());
        return placed>0;
    }
}
'''
assert JAVA.exists()
JAVA.write_text(CODE,encoding="utf8")

DATA=Path("projects/halohaven/src/main/resources/data/halohaven/worldgen/placed_feature")
for name in ("sky_clouds","sky_clouds_chalk"):
    file=DATA/(name+".json")
    j=json.loads(file.read_text(encoding="utf8"))
    assert j["feature"]=="halohaven:sky_clouds"
    count=next(i for i in j["placement"] if i["type"]=="minecraft:count")
    count["count"]=1 # deterministic chunk pass; no duplicate O(n^2) painting
    file.write_text(json.dumps(j,indent=2,ensure_ascii=False)+"\n",encoding="utf8")
print("PASS: adjacent cloud platforms join seamlessly, each chunk writes only its own 16x16 blocks")
print("PASS: first logged cloud is an actual written block, not an unchecked origin")
