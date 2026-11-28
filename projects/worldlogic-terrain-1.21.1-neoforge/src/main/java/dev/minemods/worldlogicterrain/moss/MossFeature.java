package dev.minemods.worldlogicterrain.moss;

import dev.minemods.worldlogicterrain.tree.CoreHabitat;
import dev.minemods.worldlogicterrain.worldgen.LandscapeMosaic;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class MossFeature extends Feature<NoneFeatureConfiguration> {
    public static final DeferredRegister<Feature<?>> FEATURES=DeferredRegister.create(BuiltInRegistries.FEATURE,"worldlogicterrain");
    public static final Supplier<Feature<?>> MOSS=FEATURES.register("moss_ecology",MossFeature::new);
    public MossFeature(){super(NoneFeatureConfiguration.CODEC);}
    public static boolean tryPlace(WorldGenLevel l,BlockPos p,MossSpecies s,CoreHabitat.Sample climate){
        if(l.isOutsideBuildHeight(p)||!l.ensureCanWrite(p)||!l.ensureCanWrite(p.below())||!l.getBlockState(p).isAir()
            ||!l.getFluidState(p).isEmpty()||!s.suitable(climate))return false;
        var state=s.block().defaultBlockState();
        return state.canSurvive(l,p)&&l.setBlock(p,state,Block.UPDATE_CLIENTS);
    }
    @Override public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context){
        var l=context.level();var r=context.random();
        int sx=context.origin().getX()&~15,sz=context.origin().getZ()&~15;
        if(r.nextInt(2)!=0)return false;
        var sampler=CoreHabitat.batch(l,context.chunkGenerator());
        var species=MossSpecies.values();
        int offset=r.nextInt(species.length);
        for(int k=0;k<species.length;k++){
            var s=species[(offset+k)%species.length];
            var tag=TagKey.create(Registries.BIOME,ResourceLocation.fromNamespaceAndPath("worldlogicterrain",s.habitat));
            int count=0,cx=sx+r.nextInt(16),cz=sz+r.nextInt(16);
            for(int i=0;i<16&&count<5;i++){
                int x=cx+r.nextInt(7)-3,z=cz+r.nextInt(7)-3;
                if((x&~15)!=sx||(z&~15)!=sz)continue;
                var p=LandscapeMosaic.surface(l,x,z);
                if(l.isOutsideBuildHeight(p)||!l.ensureCanWrite(p)||!l.ensureCanWrite(p.below())||!l.getBlockState(p).isAir())continue;
                var b=LandscapeMosaic.habitat(l,p,tag);
                if(b==null)continue;
                var climate=sampler.sample(p,b,l.getBlockState(p.below()));
                // No guessed climate on unsupported older Core world generators.
                if(climate.isPresent()&&tryPlace(l,p,s,climate.get()))count++;
            }
            if(count>0)return true;
        }
        return false;
    }
}
