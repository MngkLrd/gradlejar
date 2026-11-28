package dev.minemods.treechopper;

import com.mojang.serialization.Codec;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Late, chunk-local decoration. No per-chunk ticking or persistent generation index. */
public final class GroundResourcesFeature extends Feature<NoneFeatureConfiguration> {
    private static final DeferredRegister<Feature<?>> FEATURES=DeferredRegister.create(Registries.FEATURE,TreeChopper.ID);
    static{FEATURES.register("ground_resources",()->new GroundResourcesFeature(NoneFeatureConfiguration.CODEC));}
    static void register(IEventBus bus){FEATURES.register(bus);}
    public GroundResourcesFeature(Codec<NoneFeatureConfiguration> codec){super(codec);}
    @Override public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> ctx){
        return populate(ctx.level(),new ChunkPos(ctx.origin()),ctx.random())>0;
    }
    static final int TWIG_LIMIT=5, ROCK_LIMIT=4, ATTEMPTS=64;
    static int populate(WorldGenLevel l,ChunkPos chunk,RandomSource rng){
        int twigs=0,rocks=0;boolean grassAllowed=rng.nextInt(8)==0;
        List<BlockPos> twigSites=new ArrayList<>(TWIG_LIMIT),rockSites=new ArrayList<>(ROCK_LIMIT);
        // Distinct deterministic cells, no cross-chunk writes or scanning neighboring chunks.
        int start=rng.nextInt(256),stride=2*rng.nextInt(128)+1;
        for(int i=0;i<ATTEMPTS&&(twigs<TWIG_LIMIT||rocks<ROCK_LIMIT);i++){
            int cell=(start+i*stride)&255;
            int x=chunk.getMinBlockX()+(cell&15),z=chunk.getMinBlockZ()+(cell>>>4);
            if(twigs<TWIG_LIMIT){
                var site=ForestSticks.site(l,x,z,rng);
                if(site!=null&&spaced(twigSites,site.ground(),2)&&l.setBlock(site.ground(),site.twig(),Block.UPDATE_CLIENTS)){
                    twigSites.add(site.ground());twigs++;
                }
            }
            if(rocks<ROCK_LIMIT){
                var site=LooseRocks.site(l,x,z,grassAllowed);
                if(site!=null&&spaced(rockSites,site.pos(),3)&&l.setBlock(site.pos(),site.state(),Block.UPDATE_CLIENTS)){
                    rockSites.add(site.pos());rocks++;
                    if(site.grass())grassAllowed=false;
                }
            }
        }
        return twigs+rocks;
    }
    private static boolean spaced(List<BlockPos> placed,BlockPos candidate,int distance){
        for(BlockPos p:placed)if(Math.abs(p.getX()-candidate.getX())<distance&&Math.abs(p.getZ()-candidate.getZ())<distance&&Math.abs(p.getY()-candidate.getY())<3)return false;
        return true;
    }
}
