package dev.minemods.worldlogicterrain.worldgen;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.Heightmap;

/** Shared deterministic spatial fields, with no mutable world state or chunk loads. */
public final class LandscapeMosaic {
    private LandscapeMosaic(){}
    public static double field(long seed,int x,int z,int scale){
        int gx=Math.floorDiv(x,scale),gz=Math.floorDiv(z,scale);
        double fx=Math.floorMod(x,scale)/(double)scale,fz=Math.floorMod(z,scale)/(double)scale;
        fx=fx*fx*(3-2*fx);fz=fz*fz*(3-2*fz);
        return (1-fz)*((1-fx)*noise(seed,gx,gz)+fx*noise(seed,gx+1,gz))
            +fz*((1-fx)*noise(seed,gx,gz+1)+fx*noise(seed,gx+1,gz+1));
    }
    private static double noise(long seed,int x,int z){
        long h=seed^(x*0x9E3779B97F4A7C15L)^(z*0xC2B2AE3D27D4EB4FL);
        h=(h^(h>>>30))*0xBF58476D1CE4E5B9L;h=(h^(h>>>27))*0x94D049BB133111EBL;h^=h>>>31;
        return (h>>>11)*0x1.0p-53;
    }
    public static double clamp(double value,double low,double high){
        return Math.max(low,Math.min(high,value));
    }
    public static double forest(long seed,int x,int z){
        return .72*field(seed^0x52CA7L,x,z,112)+.28*field(seed^0x19D3B7L,x,z,36);
    }
    public static double meadow(long seed,int x,int z){
        return .68*field(seed^0xEF211L,x,z,96)+.32*field(seed^0x5821CDL,x,z,28);
    }
    public static double population(long seed,String species,int x,int z){
        long salt=seed^(species.hashCode()*0x9E3779B97F4A7C15L);
        return .7*field(salt,x,z,80)+.3*field(salt^0x5482A9L,x,z,24);
    }
    public static double forestDensity(double cover){
        return .08+1.22*clamp((cover-.22)/.48,0,1);
    }
    public static int grassLimit(double cover,boolean prairie){
        // Keep ordinary short grass in low-cover openings and avoid a uniform tall-grass carpet.
        return (int)Math.round((prairie?44:28)*clamp(.24+1.1*(cover-.18),.10,1));
    }
    public static BlockPos surface(WorldGenLevel level,int x,int z){
        return new BlockPos(x,level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,x,z),z);
    }
    /** Biome zoom at a vertical edge may disagree with the biome at the actual soil.
     * Check both, then the stored quart cells. Never use a remote chunk's biome.
     */
    public static Holder<Biome> habitat(WorldGenLevel level,BlockPos pos,TagKey<Biome> tag){
        Holder<Biome> above=level.getBiome(pos);
        if(above.is(tag))return above;
        Holder<Biome> ground=level.getBiome(pos.below());
        if(ground.is(tag))return ground;
        int x=pos.getX()>>2,z=pos.getZ()>>2;
        Holder<Biome> cell=level.getNoiseBiome(x,pos.getY()>>2,z);
        if(cell.is(tag))return cell;
        Holder<Biome> lower=level.getNoiseBiome(x,(pos.getY()-1)>>2,z);
        return lower.is(tag)?lower:null;
    }
    public static void verify(){
        double min=1,max=0;boolean dense=false,gap=false;
        for(int x=-512;x<=512;x+=8)for(int z=-512;z<=512;z+=8){
            double f=forest(24,x,z);
            if(f!=forest(24,x,z)||f<0||f>1||Math.abs(f-forest(24,x+1,z))>.1)
                throw new IllegalStateException("Forest mosaic discontinuity");
            min=Math.min(min,f);max=Math.max(max,f);
            double m=meadow(24,x,z);
            dense|=grassLimit(m,true)>=32;
            gap|=grassLimit(m,true)<=12;
            double p=population(24,"blueberry",x,z);
            if(p<0||p>1||p!=population(24,"blueberry",x,z)||Math.abs(p-population(24,"blueberry",x+1,z))>.12)
                throw new IllegalStateException("Population field discontinuity");
        }
        if(min>.25||max<.75||!dense||!gap||forestDensity(.15)>=forestDensity(.8))
            throw new IllegalStateException("Missing coherent stands, meadows or clearings");
    }
}
