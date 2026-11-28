package dev.minemods.worldlogicterrain.botany;

import dev.minemods.worldlogicterrain.forest.ForestEcology;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.*;

public final class TreeResourceBlock extends Block {
    public enum Resource {
        BARK("willow_bark_node","willow_log","willow_bark",48),
        RESIN("pine_resin_node","pine_log","pine_resin",32);
        public final String node,host,output;
        public final int regrowthDivisor;
        Resource(String n,String h,String o,int d){node=n;host=h;output=o;regrowthDivisor=d;}
        public Block block(){return BuiltInRegistries.BLOCK.get(id(node));}
        public ItemStack stack(){return new ItemStack(BuiltInRegistries.ITEM.get(id(output)));}
    }
    public static final DirectionProperty FACING=BlockStateProperties.HORIZONTAL_FACING;
    public static final BooleanProperty RIPE=BooleanProperty.create("ripe");
    private final Resource resource;
    private static ResourceLocation id(String path){return ResourceLocation.fromNamespaceAndPath("worldlogicterrain",path);}
    public TreeResourceBlock(Resource resource,Properties props){
        super(props);this.resource=resource;
        registerDefaultState(stateDefinition.any().setValue(FACING,Direction.NORTH).setValue(RIPE,true));
    }
    public Resource resource(){return resource;}
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> b){b.add(FACING,RIPE);}
    @Override protected boolean canSurvive(BlockState s,LevelReader l,BlockPos p){
        var host=l.getBlockState(p.relative(s.getValue(FACING)));
        return host.is(BuiltInRegistries.BLOCK.get(id(resource.host)))&&host.hasProperty(RotatedPillarBlock.AXIS)
            &&host.getValue(RotatedPillarBlock.AXIS)==Direction.Axis.Y&&l.getFluidState(p).isEmpty();
    }
    @Override protected BlockState updateShape(BlockState s,Direction d,BlockState neighbor,LevelAccessor l,BlockPos p,BlockPos other){
        if(d==s.getValue(FACING)&&!s.canSurvive(l,p))return Blocks.AIR.defaultBlockState();
        return super.updateShape(s,d,neighbor,l,p,other);
    }
    @Override protected VoxelShape getShape(BlockState s,BlockGetter l,BlockPos p,CollisionContext ctx){
        return switch(s.getValue(FACING)){
            case NORTH->Block.box(3,3,0,13,13,2);
            case SOUTH->Block.box(3,3,14,13,13,16);
            case WEST->Block.box(0,3,3,2,13,13);
            case EAST->Block.box(14,3,3,16,13,13);
            default->Shapes.empty();
        };
    }
    /** Server-only, state-backed single harvest; no host mutation. */
    public ItemStack harvest(ServerLevel l,BlockPos p){
        if(!l.hasChunkAt(p))return ItemStack.EMPTY;
        var s=l.getBlockState(p);
        if(!s.is(this)||!s.getValue(RIPE)||!l.hasChunkAt(p.relative(s.getValue(FACING)))||!s.canSurvive(l,p))return ItemStack.EMPTY;
        return l.setBlock(p,s.setValue(RIPE,false),Block.UPDATE_ALL)?resource.stack():ItemStack.EMPTY;
    }
    @Override public InteractionResult useWithoutItem(BlockState s,Level l,BlockPos p,Player player,BlockHitResult hit){
        if(!s.getValue(RIPE)||!player.mayBuild()||player.isSpectator())return InteractionResult.PASS;
        if(l.isClientSide)return InteractionResult.SUCCESS;
        if(!(l instanceof ServerLevel server))return InteractionResult.PASS;
        var result=harvest(server,p);
        if(result.isEmpty())return InteractionResult.PASS;
        Block.popResource(l,p,result);return InteractionResult.CONSUME;
    }
    public boolean regrow(ServerLevel l,BlockPos p,BlockState s){
        return l.hasChunkAt(p)&&s.is(this)&&l.getBlockState(p).equals(s)&&!s.getValue(RIPE)&&l.hasChunkAt(p.relative(s.getValue(FACING)))&&s.canSurvive(l,p)&&l.setBlock(p,s.setValue(RIPE,true),Block.UPDATE_ALL);
    }
    @Override protected void randomTick(BlockState s,ServerLevel l,BlockPos p,RandomSource random){
        if(!s.getValue(RIPE)&&random.nextInt(resource.regrowthDivisor)==0)regrow(l,p,s);
    }
    public static boolean attach(WorldGenLevel l,BlockPos trunk,String wood,RandomSource r,int x0,int z0){
        Resource resource=wood.equals("willow")?Resource.BARK:wood.equals("pine")?Resource.RESIN:null;
        if(resource==null||r.nextInt(2)!=0)return false;
        int offset=r.nextInt(4);
        for(int i=0;i<4;i++){
            var outward=Direction.from2DDataValue((offset+i)%4);var host=trunk.above(1+i%2);var p=host.relative(outward);
            if(!ForestEcology.inside(l,p,x0,z0)||!ForestEcology.inside(l,host,x0,z0)||!l.getBlockState(p).isAir()||!l.getFluidState(p).isEmpty())continue;
            var state=resource.block().defaultBlockState().setValue(FACING,outward.getOpposite());
            if(state.canSurvive(l,p)&&l.setBlock(p,state,Block.UPDATE_CLIENTS))return true;
        }
        return false;
    }
}
