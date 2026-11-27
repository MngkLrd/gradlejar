package dev.minemods.worldlogicterrain.worldgen;

import com.mojang.serialization.Codec;
import dev.minemods.worldlogicterrain.tree.CoreHabitat;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Local populations of existing wild food plants, sampled once per species/habitat chunk. */
public final class ForagePopulationFeature extends Feature<ForagePopulationFeature.Config> {
    public record Config(String species) implements FeatureConfiguration {
        public static final Codec<Config> CODEC=Codec.STRING.fieldOf("species").xmap(Config::new,Config::species).codec();
    }
    private record Rule(String habitat,int rarity,int cap,double temperature,double moisture){}
    private record Candidate(BlockPos pos,double fitness,boolean mappedTaiga){}
    private static final Map<String,Rule> RULES=Map.ofEntries(
        Map.entry("strawberry",new Rule("temperate_meadow",4,6,16,0.55)),
        Map.entry("blueberry",new Rule("cool_woodland",4,7,9,0.62)),
        Map.entry("raspberry",new Rule("temperate_woodland",5,6,15,0.6)),
        Map.entry("blackberry",new Rule("temperate_woodland",5,6,18,0.55)),
        Map.entry("grape",new Rule("warm_woodland",5,6,23,0.57)),
        Map.entry("pineapple",new Rule("tropical",4,5,27,0.73)),
        Map.entry("tomato",new Rule("warm_field",6,5,24,0.53)),
        Map.entry("cucumber",new Rule("warm_field",6,5,23,0.65)),
        Map.entry("radish",new Rule("temperate_meadow",5,6,14,0.48)),
        Map.entry("onion",new Rule("open_field",5,6,17,0.42)),
        Map.entry("cabbage",new Rule("cool_field",6,5,12,0.6)),
        Map.entry("bell_pepper",new Rule("warm_field",6,5,24,0.57))
    );
    public static final DeferredRegister<Feature<?>> FEATURES=DeferredRegister.create(BuiltInRegistries.FEATURE,"worldlogicterrain");
    public static final Supplier<Feature<?>> POPULATION=FEATURES.register("forage_population",ForagePopulationFeature::new);
    public ForagePopulationFeature(){super(Config.CODEC);}
    private static TagKey<Biome> habitat(Rule rule){
        return TagKey.create(Registries.BIOME,ResourceLocation.fromNamespaceAndPath("worldlogicterrain",rule.habitat()));
    }
    private static BlockState plant(String species){
        return BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("worldlogicterrain","wild_"+species)).defaultBlockState();
    }
    private static BlockPos surface(WorldGenLevel level,int x,int z){
        var pos=LandscapeMosaic.surface(level,x,z);
        // Only a thin snow layer can yield to a plant; never remove a snow block or ice.
        if(level.getBlockState(pos.below()).is(Blocks.SNOW))return pos.below();
        return pos;
    }
    private static boolean usable(WorldGenLevel level,BlockPos pos,BlockState plant){
        if(level.isOutsideBuildHeight(pos)||!level.ensureCanWrite(pos)||!level.ensureCanWrite(pos.below()))return false;
        BlockState current=level.getBlockState(pos);
        return (current.isAir()||current.is(Blocks.SHORT_GRASS)||current.is(Blocks.SNOW))
            &&level.getFluidState(pos).isEmpty()&&level.getFluidState(pos.below()).isEmpty()
            &&plant.canSurvive(level,pos);
    }
    static double fitness(String species,Optional<CoreHabitat.Sample> env){
        var r=RULES.get(species);
        if(env.isEmpty())return .85; // RC20 legacy biome-based fallback
        var h=env.get();
        if(h.zone().equals("MARINE")||h.zone().equals("SUBTERRANEAN")||h.slope()>55||h.waterlogging()>.94)return 0;
        double thermal=1-Math.min(.72,Math.abs(h.temperature()-r.temperature())/42);
        double moisture=1-Math.min(.65,Math.abs(h.moisture()-r.moisture())*1.1);
        double fertile=.45+.75*LandscapeMosaic.clamp(h.fertility(),0,1);
        return LandscapeMosaic.clamp(thermal*moisture*fertile*(1-.35*h.dryness())*(1-.30*h.wind()),.18,1.25);
    }
    @Override public boolean place(FeaturePlaceContext<Config> context){
        String species=context.config().species();
        Rule rule=RULES.get(species);
        if(rule==null)throw new IllegalArgumentException("Unknown forage population species "+species);
        var level=context.level();var random=context.random();var plant=plant(species);
        var habitat=habitat(rule);var sampler=CoreHabitat.batch(level,context.chunkGenerator());
        int sx=context.origin().getX()&~15,sz=context.origin().getZ()&~15;
        int offset=random.nextInt(16);Candidate best=null;double bestScore=-1;
        // A stratified search catches suitable quart-biome fragments without crossing chunk boundaries.
        for(int i=0;i<16;i++){
            int cell=(i+offset)&15;
            int x=sx+(cell&3)*4+random.nextInt(4),z=sz+(cell>>2)*4+random.nextInt(4);
            var pos=surface(level,x,z);
            var biome=LandscapeMosaic.habitat(level,pos,habitat);
            if(biome==null||!usable(level,pos,plant))continue;
            double fit=fitness(species,sampler.sample(pos,biome,level.getBlockState(pos.below())));
            if(fit<=0)continue;
            boolean taiga=species.equals("blueberry")&&biome.unwrapKey().map(k->k.location().toString().equals("worldlogic:continental_taiga")).orElse(false);
            // The broad field organizes populations; local soil selects the actual group center.
            double field=LandscapeMosaic.population(level.getSeed(),species,x,z);
            double score=fit*(.55+field*.9)+(taiga?.35:0);
            if(score>bestScore){bestScore=score;best=new Candidate(pos,fit,taiga);}
        }
        if(best==null)return false;
        BlockPos center=best.pos();
        double field=LandscapeMosaic.population(level.getSeed(),species,center.getX(),center.getZ());
        double chance=LandscapeMosaic.clamp((.55+1.5*field)*best.fitness()/rule.rarity(),.025,.6);
        // Blueberry is the characteristic cold-forest food. A valid mapped taiga
        // pocket gets additional recruitment even when its interpolated climate is marginal.
        if(best.mappedTaiga())chance=Math.max(chance,.72);
        if(random.nextDouble()>=chance)return false;
        int cap=Math.max(3,(int)Math.round(rule.cap()*(.65+.35*field)));
        return populate(level,random,center,sx,sz,cap,plant,(x,z)->{
            BlockPos pos=surface(level,x,z);
            return LandscapeMosaic.habitat(level,pos,habitat)!=null?pos:null;
        })>0;
    }
    static int populate(WorldGenLevel level,RandomSource random,BlockPos center,int sx,int sz,int cap,BlockState plant,
                        BiFunction<Integer,Integer,BlockPos> ground){
        int count=0;
        for(int i=0;i<36&&count<cap;i++){
            int x=i==0?center.getX():center.getX()+random.nextInt(4)-random.nextInt(4);
            int z=i==0?center.getZ():center.getZ()+random.nextInt(4)-random.nextInt(4);
            if((x&~15)!=sx||(z&~15)!=sz)continue;
            BlockPos pos=ground.apply(x,z);
            if(pos==null||Math.abs(pos.getY()-center.getY())>2||!usable(level,pos,plant))continue;
            if(level.setBlock(pos,plant,Block.UPDATE_CLIENTS))count++;
        }
        return count;
    }
    private static void require(boolean value,String msg){if(!value)throw new IllegalStateException(msg);}
    public static void verify(ServerLevel l){
        LandscapeMosaic.verify();
        // All 12 actual registered plants must share the same bounded population placement.
        var saved=new HashMap<BlockPos,BlockState>();
        BlockPos center=new BlockPos(8,231,8);
        for(int x=4;x<=12;x++)for(int z=4;z<=12;z++)for(int y=230;y<=232;y++){
            var pos=new BlockPos(x,y,z);saved.put(pos,l.getBlockState(pos));
        }
        BiFunction<Integer,Integer,BlockPos> fixture=(x,z)->x>=4&&x<=12&&z>=4&&z<=12?new BlockPos(x,231,z):null;
        try{
            for(var entry:RULES.entrySet()){
                String species=entry.getKey();
                BlockState state=plant(species);
                for(int x=4;x<=12;x++)for(int z=4;z<=12;z++){
                    var pos=new BlockPos(x,231,z);
                    l.setBlock(pos,Blocks.AIR.defaultBlockState(),Block.UPDATE_ALL);
                    l.setBlock(pos.above(),Blocks.AIR.defaultBlockState(),Block.UPDATE_ALL);
                    l.setBlock(pos.below(),Blocks.GRASS_BLOCK.defaultBlockState(),Block.UPDATE_ALL);
                }
                BlockPos protectedFlower=center.east();
                l.setBlock(protectedFlower,Blocks.DANDELION.defaultBlockState(),Block.UPDATE_ALL);
                int cap=entry.getValue().cap(),count=populate(l,RandomSource.create(124),center,0,0,cap,state,fixture);
                int actual=0;
                for(int x=4;x<=12;x++)for(int z=4;z<=12;z++){
                    if(l.getBlockState(new BlockPos(x,231,z)).is(state.getBlock())){
                        actual++;
                        require(Math.abs(x-8)<=3&&Math.abs(z-8)<=3,"Population escaped group "+species);
                    }
                }
                require(count==actual&&count>=3&&count<=cap,"Population count mismatch "+species+" "+count);
                require(l.getBlockState(protectedFlower).is(Blocks.DANDELION),"Population overwrote flower "+species);
                for(int x=4;x<=12;x++)for(int z=4;z<=12;z++){
                    var pos=new BlockPos(x,231,z);
                    l.setBlock(pos,Blocks.AIR.defaultBlockState(),Block.UPDATE_ALL);
                    l.setBlock(pos.below(),Blocks.STONE.defaultBlockState(),Block.UPDATE_ALL);
                }
                require(populate(l,RandomSource.create(124),center,0,0,cap,state,fixture)==0,"Population on stone "+species);
                require(fitness(species,Optional.empty())>0,"Legacy population fallback absent "+species);
            }
            var marine=new CoreHabitat.Sample(20,.8,.5,.1,.9,.1,.5,"MARINE","BOGGY",0,.1,1,0,0);
            var fertile=new CoreHabitat.Sample(23,.65,.85,.1,.2,0,.7,"OPEN_PLAINS","NORMAL",2,.1,1,0,0);
            var dry=new CoreHabitat.Sample(36,.08,.15,.9,.1,0,.1,"DRYLAND","DRY",20,.8,1,0,0);
            require(fitness("cucumber",Optional.of(marine))==0,"Marine population accepted");
            require(fitness("cucumber",Optional.of(fertile))>fitness("cucumber",Optional.of(dry)),"Climate did not influence cucumber habitat");
        }finally{
            saved.entrySet().stream().sorted(java.util.Comparator.comparingInt(v->v.getKey().getY())).forEach(v->l.setBlock(v.getKey(),v.getValue(),Block.UPDATE_ALL));
        }
    }
}
