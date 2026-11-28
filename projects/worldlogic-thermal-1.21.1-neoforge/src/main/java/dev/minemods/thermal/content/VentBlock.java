package dev.minemods.thermal.content;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.*;
public final class VentBlock extends Block {
    public static final BooleanProperty OPEN=BooleanProperty.create("open");
    public enum Kind{CHIMNEY,DAMPER,CANOPY,LEAF_CANOPY}
    private final Kind kind;
    public VentBlock(Kind k,Properties p){super(p);kind=k;registerDefaultState(stateDefinition.any().setValue(OPEN,true));}
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState>b){b.add(OPEN);}
    @Override protected VoxelShape getShape(BlockState s,BlockGetter l,BlockPos pos,CollisionContext c){
        return switch(kind){case CHIMNEY->Block.box(4,0,4,12,16,12);case DAMPER->Block.box(2,0,2,14,4,14);case CANOPY,LEAF_CANOPY->Block.box(0,13,0,16,16,16);};
    }
    @Override protected InteractionResult useWithoutItem(BlockState s,Level l,BlockPos p,Player player,BlockHitResult hit){
        if(kind!=Kind.DAMPER)return InteractionResult.PASS;
        if(!l.isClientSide)l.setBlockAndUpdate(p,s.cycle(OPEN));
        return InteractionResult.sidedSuccess(l.isClientSide);
    }
    @Override public int getFlammability(BlockState state,BlockGetter level,BlockPos pos,net.minecraft.core.Direction face){
        return kind==Kind.LEAF_CANOPY?net.minecraft.world.level.block.Blocks.OAK_LEAVES.defaultBlockState().getFlammability(level,pos,face):super.getFlammability(state,level,pos,face);
    }
    @Override public int getFireSpreadSpeed(BlockState state,BlockGetter level,BlockPos pos,net.minecraft.core.Direction face){
        return kind==Kind.LEAF_CANOPY?net.minecraft.world.level.block.Blocks.OAK_LEAVES.defaultBlockState().getFireSpreadSpeed(level,pos,face):super.getFireSpreadSpeed(state,level,pos,face);
    }
    public static boolean canopy(BlockState state){return state.getBlock() instanceof VentBlock v&&(v.kind==Kind.CANOPY||v.kind==Kind.LEAF_CANOPY);}
    /** A woven roof casts shade without requiring a fully enclosed heated room. */
    public static boolean shades(LevelReader l,BlockPos pos){
        for(int dy=1;dy<=8;dy++){
            BlockPos q=pos.above(dy);
            if(q.getY()>=l.getMaxBuildHeight()||!l.hasChunkAt(q))return false;
            if(canopy(l.getBlockState(q)))return true;
        }
        return false;
    }
    /** Leaf canopy gives limited protection against direct rain; it is not an enclosed room. */
    public static boolean leafCover(LevelReader l,BlockPos pos){
        for(int dy=1;dy<=8;dy++){
            BlockPos q=pos.above(dy);
            if(q.getY()>=l.getMaxBuildHeight()||!l.hasChunkAt(q))return false;
            BlockState s=l.getBlockState(q);
            if(s.is(ThermalContent.LEAF_CANOPY.get()))return true;
            if(Block.isShapeFullBlock(s.getCollisionShape(l,q)))return false;
        }
        return false;
    }
    public static double adjustment(LevelReader l,BlockPos pos){
        double multiplier=1;
        BlockPos top=pos.above();
        if(l.hasChunkAt(top)&&l.getBlockState(top).is(ThermalContent.CHIMNEY.get())&&l.canSeeSky(top.above()))multiplier=1.15;
        for(var direction:net.minecraft.core.Direction.Plane.HORIZONTAL){
            BlockPos neighbor=pos.relative(direction);
            if(l.hasChunkAt(neighbor)){var s=l.getBlockState(neighbor);if(s.is(ThermalContent.DAMPER.get())&&!s.getValue(OPEN))return multiplier*.45;}
        }
        return multiplier;
    }
}
