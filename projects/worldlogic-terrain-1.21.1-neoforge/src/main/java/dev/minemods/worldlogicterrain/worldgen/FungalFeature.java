package dev.minemods.worldlogicterrain.worldgen;

import com.mojang.serialization.Codec;
import dev.minemods.worldlogicterrain.growth.FungalColonyBlock;
import dev.minemods.worldlogicterrain.growth.FungalEcology;
import dev.minemods.worldlogicterrain.growth.FungalEcology.Species;
import dev.minemods.worldlogicterrain.tree.CoreHabitat;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class FungalFeature extends Feature<FungalFeature.Config> {
    public record Config(String species) implements FeatureConfiguration {
        public static final Codec<Config> CODEC=Codec.STRING.fieldOf("species").xmap(Config::new,Config::species).codec();
    }
    public static final DeferredRegister<Feature<?>> FEATURES=DeferredRegister.create(BuiltInRegistries.FEATURE,"worldlogicterrain");
    public static final Supplier<Feature<?>> COLONY=FEATURES.register("fungal_colony",FungalFeature::new);
    public FungalFeature(){super(Config.CODEC);}
    public static TagKey<Biome> habitat(Species s){return TagKey.create(Registries.BIOME,ResourceLocation.fromNamespaceAndPath("worldlogicterrain",s.habitat));}
    static boolean acceptable(WorldGenLevel l,BlockPos p,Species s){
        return !l.isOutsideBuildHeight(p)&&l.ensureCanWrite(p)&&l.ensureCanWrite(p.below())&&
            l.getBlockState(p).isAir()&&l.getFluidState(p).isEmpty()&&l.getFluidState(p.below()).isEmpty()&&s.supports(l.getBlockState(p.below()));
    }
    private static boolean climate(Species s,Optional<CoreHabitat.Sample> input,boolean precipitation){
        if(input.isEmpty())return precipitation; // legacy biome-tag selection; no second climate model
        var h=input.get();
        return !h.zone().equals("MARINE")&&!h.zone().equals("SUBTERRANEAN")&&h.waterlogging()<.94&&
            FungalEcology.rate(s,h.temperature(),h.moisture(),8)>0;
    }
    @Override public boolean place(FeaturePlaceContext<Config> c){
        Species s=Species.byId(c.config().species()).orElseThrow();
        var l=c.level();var r=c.random();
        if(r.nextInt(s.rarity)!=0)return false;
        int sx=c.origin().getX()&~15,sz=c.origin().getZ()&~15;
        var habitat=habitat(s);var sampler=CoreHabitat.batch(l,c.chunkGenerator());
        BiFunction<Integer,Integer,BlockPos> site=(x,z)->{
            var p=LandscapeMosaic.surface(l,x,z);
            if(!acceptable(l,p,s))return null;
            var b=LandscapeMosaic.habitat(l,p,habitat);
            return b!=null&&climate(s,sampler.sample(p,b,l.getBlockState(p.below())),b.value().hasPrecipitation())?p:null;
        };
        int offset=r.nextInt(16);
        for(int i=0;i<16;i++){
            int cell=(i+offset)&15;
            var center=site.apply(sx+(cell&3)*4+r.nextInt(4),sz+(cell>>2)*4+r.nextInt(4));
            if(center!=null)return populate(l,r,center,sx,sz,s,site)>0;
        }
        return false;
    }
    /** Bounded clusters share the same placement method with the executable fixture. */
    public static int populate(WorldGenLevel l,RandomSource r,BlockPos center,int sx,int sz,Species s,
                               BiFunction<Integer,Integer,BlockPos> site){
        int count=0,cap=2+r.nextInt(3);
        for(int i=0;i<24&&count<cap;i++){
            int x=i==0?center.getX():center.getX()+r.nextInt(3)-r.nextInt(3);
            int z=i==0?center.getZ():center.getZ()+r.nextInt(3)-r.nextInt(3);
            if((x&~15)!=sx||(z&~15)!=sz)continue;
            var p=site.apply(x,z);
            if(p==null||Math.abs(p.getY()-center.getY())>1||!acceptable(l,p,s))continue;
            int roll=r.nextInt(100),age=roll<20?0:roll<55?1:2;
            if(l.setBlock(p,s.block().defaultBlockState().setValue(FungalColonyBlock.AGE,age),Block.UPDATE_CLIENTS))count++;
        }
        return count;
    }
}
