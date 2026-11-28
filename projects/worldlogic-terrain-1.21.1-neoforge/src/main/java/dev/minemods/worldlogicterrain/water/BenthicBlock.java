package dev.minemods.worldlogicterrain.water;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Every segment contains the precise source fluid; vanilla water is never substituted for native water. */
public final class BenthicBlock extends Block {
    public static final IntegerProperty WATER=IntegerProperty.create("water",0,8),SEGMENT=IntegerProperty.create("segment",0,5);
    private static final String[] FLUIDS={"","sea_water","brackish_water","river_water","lake_water","pond_water","spring_water","wetland_water","groundwater"};
    private final BenthicSpecies species;
    public BenthicBlock(BenthicSpecies species,Properties p){super(p);this.species=species;registerDefaultState(stateDefinition.any().setValue(WATER,0).setValue(SEGMENT,0));}
    public BenthicSpecies species(){return species;}
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> b){b.add(WATER,SEGMENT);}
    static Fluid fluid(int index){return index==0?Fluids.WATER:BuiltInRegistries.FLUID.get(ResourceLocation.fromNamespaceAndPath("worldlogic",FLUIDS[index]));}
    static int index(Fluid f){
        if(f==Fluids.WATER)return 0;
        var id=BuiltInRegistries.FLUID.getKey(f);if(!id.getNamespace().equals("worldlogic"))return -1;
        for(int i=1;i<FLUIDS.length;i++)if(FLUIDS[i].equals(id.getPath()))return i;
        return -1;
    }
    static int nativeIndex(AquaticHabitat.Kind kind){return switch(kind){
        case SEA->1;case BRACKISH->2;case RIVER->3;case LAKE->4;case POND->5;case SPRING->6;case WETLAND->7;case GROUNDWATER->8;default->-1;};}
    static AquaticHabitat.Kind kind(int idx){return switch(idx){
        case 1->AquaticHabitat.Kind.SEA;case 2->AquaticHabitat.Kind.BRACKISH;case 3->AquaticHabitat.Kind.RIVER;
        case 4->AquaticHabitat.Kind.LAKE;case 5->AquaticHabitat.Kind.POND;case 6->AquaticHabitat.Kind.SPRING;
        case 7->AquaticHabitat.Kind.WETLAND;case 8->AquaticHabitat.Kind.GROUNDWATER;default->AquaticHabitat.Kind.UNKNOWN;};}
    static boolean nativeAvailable(){return fluid(1)!=Fluids.EMPTY;}
    static int worldgenIndex(BlockState water,AquaticHabitat.Kind kind,boolean prediction){
        if(!AquaticHabitat.water(water)||!water.getFluidState().isSource())return -1;
        int idx=index(water.getFluidState().getType());
        if(idx!=0)return idx;
        if(prediction&&nativeAvailable()){int nativeId=nativeIndex(kind);return nativeId>0&&fluid(nativeId)!=Fluids.EMPTY?nativeId:-1;}
        return 0;
    }
    @Override protected FluidState getFluidState(BlockState s){return fluid(s.getValue(WATER)).defaultFluidState();}
    private static BlockState liquid(BlockState s){return s.getFluidState().createLegacyBlock();}
    @Override protected boolean canSurvive(BlockState s,LevelReader l,BlockPos p){
        int n=s.getValue(SEGMENT);int f=s.getValue(WATER);
        if(fluid(f)==Fluids.EMPTY||n>=species.height||f>0&&!species.accepts(kind(f)))return false;
        if(n==0)return species.supports(l,p.below());
        var below=l.getBlockState(p.below());
        return below.is(this)&&below.getValue(WATER)==f&&below.getValue(SEGMENT)==n-1;
    }
    @Override protected BlockState updateShape(BlockState s,Direction direction,BlockState neighbor,LevelAccessor l,BlockPos p,BlockPos np){
        if(direction==Direction.DOWN&&!canSurvive(s,l,p))return liquid(s);
        return s;
    }
    @Override public BlockState getStateForPlacement(BlockPlaceContext c){
        var l=c.getLevel();var p=c.getClickedPos();var source=l.getBlockState(p);
        if(!AquaticHabitat.water(source)||!source.getFluidState().isSource())return null;
        var kind=AquaticHabitat.hint(source,l.getBiome(p));if(!species.accepts(kind))return null;
        int f=index(source.getFluidState().getType());if(f<0)return null;
        var s=defaultBlockState().setValue(WATER,f);
        return s.canSurvive(l,p)?s:null;
    }
    @Override protected VoxelShape getShape(BlockState s,BlockGetter l,BlockPos p,CollisionContext ctx){
        int h=species.height>1?16:switch(species){case RED_SEA_STAR,BLUE_SEA_STAR,CORALLINE_ALGAE,RIVER_MOSS,MUSSEL_BED->4;case SEA_URCHIN->8;default->14;};
        return Block.box(2,0,2,14,h,14);
    }
    @Override protected boolean isRandomlyTicking(BlockState s){return species.growth>0&&s.getValue(SEGMENT)<species.height-1;}
    @Override protected void randomTick(BlockState s,ServerLevel l,BlockPos p,RandomSource random){
        if(species.growth>0&&random.nextInt(species.growth)==0)grow(l,p,s);
    }
    boolean grow(ServerLevel l,BlockPos p,BlockState s){
        int segment=s.getValue(SEGMENT),f=s.getValue(WATER);
        if(species.growth==0||segment>=species.height-1||!l.hasChunkAt(p.above())||l.isOutsideBuildHeight(p.above()))return false;
        var upper=l.getBlockState(p.above());
        if(!AquaticHabitat.water(upper)||!upper.getFluidState().isSource()||index(upper.getFluidState().getType())!=f)return false;
        var kind=AquaticHabitat.hint(upper,l.getBiome(p.above()));
        if(!species.accepts(kind)||l.getBiome(p).value().getBaseTemperature()<species.minTemp||l.getBiome(p).value().getBaseTemperature()>species.maxTemp)return false;
        return l.setBlock(p.above(),s.setValue(SEGMENT,segment+1),Block.UPDATE_ALL);
    }
}
