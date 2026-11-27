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
    public enum Kind{CHIMNEY,DAMPER,CANOPY}
    private final Kind kind;
    public VentBlock(Kind k,Properties p){super(p);kind=k;registerDefaultState(stateDefinition.any().setValue(OPEN,true));}
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState>b){b.add(OPEN);}
    @Override protected VoxelShape getShape(BlockState s,BlockGetter l,BlockPos pos,CollisionContext c){
        return switch(kind){case CHIMNEY->Block.box(4,0,4,12,16,12);case DAMPER->Block.box(2,0,2,14,4,14);case CANOPY->Block.box(0,13,0,16,16,16);};
    }
    @Override protected InteractionResult useWithoutItem(BlockState s,Level l,BlockPos p,Player player,BlockHitResult hit){
        if(kind!=Kind.DAMPER)return InteractionResult.PASS;
        if(!l.isClientSide)l.setBlockAndUpdate(p,s.cycle(OPEN));
        return InteractionResult.sidedSuccess(l.isClientSide);
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
