package dev.minemods.thermal.content;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.*;
/** Single batch. 0 empty, 1 working, 2 ready. Fixed recipes allow compact persisted blockstate. */
public final class TextileStationBlock extends Block {
    public static final IntegerProperty PHASE=IntegerProperty.create("phase",0,2);
    public enum Kind{LOOM,RACK,HIDE,FELT}
    private final Kind kind;
    public TextileStationBlock(Kind k,Properties p){super(p);kind=k;registerDefaultState(stateDefinition.any().setValue(PHASE,0));}
    @Override protected VoxelShape getShape(BlockState s,BlockGetter l,BlockPos p,CollisionContext c){
        return switch(kind){
            case LOOM -> Shapes.or(Block.box(0,0,0,3,16,16),Block.box(13,0,0,16,16,16),Block.box(3,13,1,13,16,4),Block.box(3,2,10,13,5,13),Block.box(3,4,8,13,12,10));
            case RACK -> Shapes.or(Block.box(1,0,1,3,15,3),Block.box(13,0,1,15,15,3),Block.box(1,0,13,3,15,15),Block.box(13,0,13,15,15,15),Block.box(1,13,1,15,15,3),Block.box(1,13,13,15,15,15));
            case HIDE -> Shapes.or(Block.box(0,0,5,2,16,7),Block.box(14,0,5,16,16,7),Block.box(2,0,5,14,2,7),Block.box(2,14,5,14,16,7),s.getValue(PHASE)==0?Shapes.empty():Block.box(2,2,5,14,14,6));
            case FELT -> Shapes.or(Block.box(1,0,1,4,9,4),Block.box(12,0,1,15,9,4),Block.box(1,0,12,4,9,15),Block.box(12,0,12,15,9,15),Block.box(0,9,0,16,12,16));
        };
    }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState>b){b.add(PHASE);}
    public ItemStack input(){return switch(kind){
        case LOOM -> new ItemStack(ThermalContent.THREAD.get(),3);
        case RACK -> new ItemStack(ThermalContent.WET_CLOTH.get());
        case HIDE -> new ItemStack(Items.LEATHER);
        case FELT -> new ItemStack(ThermalContent.COMBED_WOOL.get(),4);
    };}
    public ItemStack output(){return switch(kind){
        case LOOM -> new ItemStack(ThermalContent.MAT.get(),2);
        case RACK -> new ItemStack(ThermalContent.WET_CLOTH.get());
        case HIDE -> new ItemStack(ThermalContent.PREPARED_HIDE.get());
        case FELT -> new ItemStack(ThermalContent.FELT.get(),2);
    };}
    public int duration(){return switch(kind){case LOOM->100;case RACK->600;case HIDE->200;case FELT->200;};}
    public boolean begin(ServerLevel l,BlockPos pos,BlockState state,ItemStack s,boolean creative){
        ItemStack needed=input();
        if(!state.is(this)||state.getValue(PHASE)!=0||!s.is(needed.getItem())||s.getCount()<needed.getCount())return false;
        if(kind==Kind.RACK&&(!(s.getItem() instanceof ThermalPackItem item)||item.remaining(s,ThermalPackItem.clock(l))==0))return false;
        if(!l.setBlockAndUpdate(pos,state.setValue(PHASE,1)))return false;l.scheduleTick(pos,this,duration());
        if(!creative)s.shrink(needed.getCount());return true;
    }
    @Override protected ItemInteractionResult useItemOn(ItemStack s,BlockState state,Level l,BlockPos pos,Player p,InteractionHand hand,BlockHitResult hit){
        if(state.getValue(PHASE)==2)return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        if(state.getValue(PHASE)!=0||!s.is(input().getItem()))return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        if(l instanceof ServerLevel server&&!begin(server,pos,state,s,p.getAbilities().instabuild))return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        return ItemInteractionResult.sidedSuccess(l.isClientSide);
    }
    public ItemStack collect(ServerLevel l,BlockPos pos,BlockState s){
        if(s.getValue(PHASE)!=2)return ItemStack.EMPTY;
        l.setBlockAndUpdate(pos,s.setValue(PHASE,0));return output();
    }
    @Override protected InteractionResult useWithoutItem(BlockState s,Level l,BlockPos pos,Player p,BlockHitResult hit){
        if(s.getValue(PHASE)!=2)return InteractionResult.PASS;
        if(l instanceof ServerLevel server){ItemStack result=collect(server,pos,s);if(!p.getInventory().add(result))p.drop(result,false);}
        return InteractionResult.sidedSuccess(l.isClientSide);
    }
    @Override protected void tick(BlockState s,ServerLevel l,BlockPos p,RandomSource random){
        if(s.getValue(PHASE)==1)l.setBlockAndUpdate(p,s.setValue(PHASE,2));
    }
    @Override protected void onRemove(BlockState old,Level l,BlockPos p,BlockState next,boolean moving){
        if(!old.is(next.getBlock())&&!l.isClientSide&&old.getValue(PHASE)!=0)Block.popResource(l,p,old.getValue(PHASE)==2?output():input());
        super.onRemove(old,l,p,next,moving);
    }
}
