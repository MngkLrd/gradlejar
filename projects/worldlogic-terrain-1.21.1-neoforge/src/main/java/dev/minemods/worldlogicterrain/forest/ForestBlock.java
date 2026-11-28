package dev.minemods.worldlogicterrain.forest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public final class ForestBlock extends Block {
    public static final DirectionProperty FACING=BlockStateProperties.HORIZONTAL_FACING;
    public static final IntegerProperty SEGMENT=IntegerProperty.create("segment",0,3);
    private final ForestSpecies species;
    public ForestBlock(ForestSpecies species,Properties properties){
        super(properties);this.species=species;
        registerDefaultState(stateDefinition.any().setValue(FACING,Direction.NORTH).setValue(SEGMENT,0));
    }
    public ForestSpecies species(){return species;}
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> b){b.add(FACING,SEGMENT);}
    @Override public BlockState getStateForPlacement(BlockPlaceContext c){
        Direction d=species.wall()?c.getClickedFace().getOpposite():c.getHorizontalDirection().getOpposite();
        if(d.getAxis().isVertical())return null;
        var s=defaultBlockState().setValue(FACING,d);
        return s.canSurvive(c.getLevel(),c.getClickedPos())?s:null;
    }
    @Override protected boolean canSurvive(BlockState s,LevelReader l,BlockPos p){
        return l.getFluidState(p).isEmpty()&&species.supports(l,p,s.getValue(FACING));
    }
    @Override protected BlockState updateShape(BlockState s,Direction d,BlockState neighbor,LevelAccessor l,BlockPos p,BlockPos other){
        if(d==(species.wall()?s.getValue(FACING):Direction.DOWN)&&!s.canSurvive(l,p))return Blocks.AIR.defaultBlockState();
        return super.updateShape(s,d,neighbor,l,p,other);
    }
    @Override protected VoxelShape getShape(BlockState s,BlockGetter l,BlockPos p,CollisionContext ctx){
        int n=species.extent;var facing=s.getValue(FACING);
        if(species.wall())return switch(facing){
            case NORTH->Block.box(0,0,0,16,16,n);
            case SOUTH->Block.box(0,0,16-n,16,16,16);
            case WEST->Block.box(0,0,0,n,16,16);
            case EAST->Block.box(16-n,0,0,16,16,16);
            default->Block.box(0,0,0,16,16,n);
        };
        if(species.kind==ForestSpecies.Kind.ROOT)return facing.getAxis()==Direction.Axis.Z?Block.box(4,0,0,12,n,16):Block.box(0,0,4,16,n,12);
        return species.kind==ForestSpecies.Kind.LITTER?Block.box(0,0,0,16,1,16):Block.box(2,0,2,14,n,14);
    }
    /** Each segment must have its own natural host; max four blocks per generated vine. */
    public boolean growDown(ServerLevel l,BlockPos p,BlockState s){
        if(species.kind!=ForestSpecies.Kind.VINE||!s.is(this)||!l.getBlockState(p).equals(s)||s.getValue(SEGMENT)>=3||!s.canSurvive(l,p))return false;
        var next=p.below();var host=next.relative(s.getValue(FACING));
        if(l.isOutsideBuildHeight(next)||!l.hasChunkAt(next)||!l.hasChunkAt(host)||!l.getBlockState(next).isAir())return false;
        var child=s.setValue(SEGMENT,s.getValue(SEGMENT)+1);
        return child.canSurvive(l,next)&&l.setBlock(next,child,Block.UPDATE_ALL);
    }
    @Override public void randomTick(BlockState s,ServerLevel l,BlockPos p,RandomSource r){
        if(species.kind==ForestSpecies.Kind.VINE&&r.nextInt(20)==0)growDown(l,p,s);
    }
    @Override protected void entityInside(BlockState s,Level l,BlockPos p,Entity entity){
        if(l.isClientSide||species.contactDamage<=0||!(entity instanceof LivingEntity living)||entity.tickCount%20!=0)return;
        if(entity instanceof Player player&&(player.isCreative()||player.isSpectator()))return;
        if(entity.getDeltaMovement().horizontalDistanceSqr()<.0001)return;
        living.hurt(l.damageSources().sweetBerryBush(),species.contactDamage);
    }
}
