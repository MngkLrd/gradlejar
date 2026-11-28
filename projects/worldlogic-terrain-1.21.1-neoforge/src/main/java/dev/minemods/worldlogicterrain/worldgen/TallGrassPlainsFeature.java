package dev.minemods.worldlogicterrain.worldgen;

import java.util.function.BiFunction;
import java.util.function.Supplier;
import java.util.HashMap;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Extra vanilla tall grass in broad low-relief open grasslands. */
public final class TallGrassPlainsFeature extends Feature<NoneFeatureConfiguration> {
    public static final DeferredRegister<Feature<?>> FEATURES=DeferredRegister.create(BuiltInRegistries.FEATURE,"worldlogicterrain");
    public static final Supplier<Feature<?>> TALL_GRASS=FEATURES.register("tall_grass_plains",TallGrassPlainsFeature::new);
    private static final TagKey<Biome> OPEN=TagKey.create(Registries.BIOME,ResourceLocation.fromNamespaceAndPath("worldlogicterrain","tall_grass_plains"));
    private static final ResourceLocation PRAIRIE=ResourceLocation.fromNamespaceAndPath("worldlogic","tallgrass_prairie");
    public TallGrassPlainsFeature(){super(NoneFeatureConfiguration.CODEC);}
    static boolean sufficientlyFlat(int[] heights,int openSamples){
        if(heights.length!=9||openSamples<7)return false;
        int min=heights[0],max=heights[0];
        for(int y:heights){min=Math.min(min,y);max=Math.max(max,y);}
        return max-min<=3;
    }
    private static BlockPos surface(WorldGenLevel level,int x,int z){
        return new BlockPos(x,level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,x,z),z);
    }
    private static boolean openGround(WorldGenLevel level,BlockPos pos){
        return !level.isOutsideBuildHeight(pos)&&level.ensureCanWrite(pos.below())
            && LandscapeMosaic.habitat(level,pos,OPEN)!=null
            && level.getFluidState(pos.below()).isEmpty()
            && Blocks.TALL_GRASS.defaultBlockState().canSurvive(level,pos);
    }
    @Override public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context){
        WorldGenLevel level=context.level();
        int startX=context.origin().getX()&~15,startZ=context.origin().getZ()&~15;
        int[] heights=new int[9];int samples=0,prairie=0,i=0;
        for(int dx:new int[]{2,8,13})for(int dz:new int[]{2,8,13}){
            BlockPos pos=surface(level,startX+dx,startZ+dz);
            heights[i++]=pos.getY();
            if(openGround(level,pos)){
                samples++;
                if(level.getBiome(pos.below()).unwrapKey().map(k->k.location().equals(PRAIRIE)).orElse(false)
                    ||level.getBiome(pos).unwrapKey().map(k->k.location().equals(PRAIRIE)).orElse(false))prairie++;
            }
        }
        if(!sufficientlyFlat(heights,samples))return false;
        var sampler=dev.minemods.worldlogicterrain.tree.CoreHabitat.batch(level,context.chunkGenerator());
        BlockPos middle=surface(level,startX+8,startZ+8);
        var biome=LandscapeMosaic.habitat(level,middle,OPEN);
        double cover=LandscapeMosaic.meadow(level.getSeed(),middle.getX(),middle.getZ());
        if(biome!=null){
            var env=sampler.sample(middle,biome,level.getBlockState(middle.below()));
            if(env.isPresent()){
                var h=env.get();
                cover=LandscapeMosaic.clamp(cover+.18*(h.moisture()-.5)+.22*(h.fertility()-.5)-.16*h.dryness(),0,1);
            }
        }
        int limit=LandscapeMosaic.grassLimit(cover,prairie>=5);
        return populate(level,context.random(),startX,startZ,limit,(x,z)->{
            BlockPos pos=surface(level,x,z);
            return openGround(level,pos)?pos:null;
        })>0;
    }
    static int populate(WorldGenLevel level,RandomSource random,int startX,int startZ,int limit,
                        BiFunction<Integer,Integer,BlockPos> ground){
        int placed=0;
        for(int patch=0;patch<5&&placed<limit;patch++){
            int centerX=startX+random.nextInt(16),centerZ=startZ+random.nextInt(16);
            for(int attempt=0;attempt<24&&placed<limit;attempt++){
                int x=centerX+random.nextInt(6)-random.nextInt(6),z=centerZ+random.nextInt(6)-random.nextInt(6);
                if((x&~15)!=startX||(z&~15)!=startZ)continue;
                BlockPos pos=ground.apply(x,z);
                if(pos==null||level.isOutsideBuildHeight(pos.above())
                    ||!level.ensureCanWrite(pos)||!level.ensureCanWrite(pos.above()))continue;
                var state=level.getBlockState(pos);
                if((!state.isAir()&&!state.is(Blocks.SHORT_GRASS))
                    ||!level.getBlockState(pos.above()).isAir()
                    ||!Blocks.TALL_GRASS.defaultBlockState().canSurvive(level,pos))continue;
                DoublePlantBlock.placeAt(level,Blocks.TALL_GRASS.defaultBlockState(),pos,Block.UPDATE_CLIENTS);
                placed++;
            }
        }
        return placed;
    }
    static void require(boolean value,String message){if(!value)throw new IllegalStateException(message);}
    public static void registerChecks(RegisterCommandsEvent e){
        e.getDispatcher().register(Commands.literal("worldlogicterrain").requires(s->s.hasPermission(2))
          .then(Commands.literal("tall_grass_check").executes(c->{
            try{
                check(c.getSource().getLevel());
                c.getSource().sendSuccess(()->Component.literal("Tall grass plains verified: flat relief, open samples, double-block plants, density cap, stone rejection and plant preservation"),false);
                return 1;
            }catch(IllegalStateException ex){c.getSource().sendFailure(Component.literal(ex.getMessage()));return 0;}
          })));
    }
    private static void reset(ServerLevel l,BlockState floor,boolean grass){
        for(int x=1;x<=14;x++)for(int z=1;z<=14;z++){
            l.setBlock(new BlockPos(x,232,z),Blocks.AIR.defaultBlockState(),Block.UPDATE_ALL);
            l.setBlock(new BlockPos(x,231,z),Blocks.AIR.defaultBlockState(),Block.UPDATE_ALL);
            l.setBlock(new BlockPos(x,230,z),floor,Block.UPDATE_ALL);
            if(grass)l.setBlock(new BlockPos(x,231,z),Blocks.SHORT_GRASS.defaultBlockState(),Block.UPDATE_ALL);
        }
    }
    static void check(ServerLevel l){
        require(sufficientlyFlat(new int[]{64,65,66,64,65,66,65,66,67},9),"flat plains rejected");
        require(!sufficientlyFlat(new int[]{64,65,66,64,65,66,65,66,68},9),"steep surface accepted");
        require(!sufficientlyFlat(new int[]{64,64,64,64,64,64,64,64,64},6),"insufficient open area accepted");
        require(l.hasChunkAt(new BlockPos(8,231,8)),"tall grass fixture not loaded");
        var saved=new HashMap<BlockPos,BlockState>();
        for(int x=1;x<=14;x++)for(int z=1;z<=14;z++)for(int y=230;y<=232;y++){
            var pos=new BlockPos(x,y,z);saved.put(pos,l.getBlockState(pos));
        }
        BiFunction<Integer,Integer,BlockPos> ground=(x,z)->x>=1&&x<=14&&z>=1&&z<=14?new BlockPos(x,231,z):null;
        try{
            reset(l,Blocks.GRASS_BLOCK.defaultBlockState(),true);
            BlockPos flower=new BlockPos(8,231,8);l.setBlock(flower,Blocks.DANDELION.defaultBlockState(),Block.UPDATE_ALL);
            int count=populate(l,RandomSource.create(810),0,0,28,ground),actual=0;
            for(int x=1;x<=14;x++)for(int z=1;z<=14;z++){
                var pos=new BlockPos(x,231,z);var b=l.getBlockState(pos);
                if(b.is(Blocks.TALL_GRASS)){
                    require(b.getValue(DoublePlantBlock.HALF)==DoubleBlockHalf.LOWER,"wrong lower half");
                    var upper=l.getBlockState(pos.above());
                    require(upper.is(Blocks.TALL_GRASS)&&upper.getValue(DoublePlantBlock.HALF)==DoubleBlockHalf.UPPER,"missing upper half");
                    actual++;
                }
            }
            require(count==actual&&count>=12&&count<=28,"tall grass density or placement mismatch: "+actual);
            require(l.getBlockState(flower).is(Blocks.DANDELION),"existing flower was overwritten");
            reset(l,Blocks.STONE.defaultBlockState(),false);
            require(populate(l,RandomSource.create(810),0,0,28,ground)==0,"tall grass generated on stone");
        }finally{
            saved.entrySet().stream().sorted(java.util.Comparator.comparingInt(v->v.getKey().getY())).forEach(v->l.setBlock(v.getKey(),v.getValue(),Block.UPDATE_ALL));
        }
    }
}
