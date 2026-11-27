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
/** 0 empty, 1 active, 2 retained heat. Vanilla persisted scheduled ticks. */
public final class ClimateDeviceBlock extends Block {
    public static final IntegerProperty PHASE=IntegerProperty.create("phase",0,2);
    public enum Kind {
        LAMP(12,7200,0), BRAZIER(58,3600,0), STOVE(85,5400,3600), FIREPLACE(95,4800,1800),
        EVAP_WALL(0,7200,0), ICE_BOX(0,4800,0), WIND_TOWER(0,0,0);
        final double output;final int burn,residual;
        Kind(double output,int burn,int residual){this.output=output;this.burn=burn;this.residual=residual;}
    }
    private final Kind kind;
    public ClimateDeviceBlock(Kind k,Properties p){super(p);kind=k;registerDefaultState(stateDefinition.any().setValue(PHASE,0));}
    public Kind kind(){return kind;}
    public int activeSeconds(){return kind.burn/20;}
    public int residualSeconds(){return kind.residual/20;}
    @Override protected VoxelShape getShape(BlockState s,BlockGetter l,BlockPos p,CollisionContext c){
        return switch(kind){
            case LAMP -> Shapes.or(Block.box(3,0,3,13,2,13),Block.box(4,2,4,12,5,12),Block.box(5,5,5,11,7,11),Block.box(7,7,7,9,9,9));
            case BRAZIER -> Shapes.or(Block.box(3,0,3,13,2,13),Block.box(6,2,6,10,5,10),Block.box(2,5,2,14,9,14));
            case FIREPLACE -> Shapes.or(Block.box(0,0,0,16,3,16),Block.box(0,3,0,3,13,16),Block.box(13,3,0,16,13,16),Block.box(3,3,13,13,13,16),Block.box(3,13,0,13,16,16),Block.box(3,3,3,13,5,13));
            case EVAP_WALL -> Shapes.or(Block.box(0,0,4,16,16,12),Block.box(0,0,2,16,2,14),Block.box(0,14,2,16,16,14));
            case ICE_BOX -> Shapes.or(Block.box(1,0,1,15,3,15),Block.box(1,3,1,3,12,15),Block.box(13,3,1,15,12,15),Block.box(3,3,1,13,12,3),Block.box(3,3,13,13,12,15),Block.box(3,3,3,13,s.getValue(PHASE)==1?10:5,13),Block.box(0,12,0,16,14,3),Block.box(0,12,13,16,14,16),Block.box(0,12,3,3,14,13),Block.box(13,12,3,16,14,13));
            case WIND_TOWER -> Shapes.or(Block.box(0,0,0,16,3,16),Block.box(0,3,0,3,13,16),Block.box(13,3,0,16,13,16),Block.box(3,3,6,13,13,10),Block.box(3,3,1,5,13,3),Block.box(11,3,1,13,13,3),Block.box(0,13,0,16,16,16));
            case STOVE -> Shapes.or(Block.box(0,0,0,16,4,16),Block.box(0,4,0,4,12,16),Block.box(12,4,0,16,12,16),Block.box(4,4,4,12,12,16),Block.box(4,4,0,12,5,4),Block.box(0,12,0,16,16,16));
        };
    }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> b){b.add(PHASE);}
    public double heat(BlockState s){int stage=s.getValue(PHASE);return stage==1?kind.output:stage==2?kind.output*.4:0;}
    public double cooling(BlockState s,double air,double humidity,double distance,boolean sheltered,double wind,boolean openTop){
        if(air<=24)return 0;
        if(kind==Kind.WIND_TOWER)return openTop&&distance<=5?Math.min(air-24,7*wind/(1+.18*distance*distance)):0;
        if(s.getValue(PHASE)!=1)return 0;
        if(kind==Kind.EVAP_WALL)return dev.minemods.thermal.ClimateComfort.cooling(air,humidity,distance,sheltered,true)*1.3;
        if(kind==Kind.ICE_BOX)return Math.min(air-24,13/(1+.18*distance*distance));
        return 0;
    }
    public boolean accepts(ItemStack s){
        return switch(kind){
            case LAMP -> s.is(ThermalContent.OIL.get());
            case BRAZIER,STOVE,FIREPLACE -> s.is(Items.COAL)||s.is(Items.CHARCOAL);
            case EVAP_WALL -> s.is(Items.WATER_BUCKET);
            case ICE_BOX -> s.is(Items.ICE)||s.is(Items.PACKED_ICE)||s.is(Items.BLUE_ICE);
            case WIND_TOWER -> false;
        };
    }
    public boolean activate(ServerLevel l,BlockPos p,BlockState s){
        if(!s.is(this)||kind.burn==0||s.getValue(PHASE)!=0)return false;
        if(!l.setBlockAndUpdate(p,s.setValue(PHASE,1)))return false;l.scheduleTick(p,this,kind.burn);return true;
    }
    @Override protected ItemInteractionResult useItemOn(ItemStack s,BlockState state,Level l,BlockPos pos,Player p,InteractionHand hand,BlockHitResult hit){
        if(!accepts(s)||state.getValue(PHASE)!=0)return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        if(l instanceof ServerLevel server&&activate(server,pos,state)&&!p.getAbilities().instabuild){
            if(kind==Kind.EVAP_WALL)p.setItemInHand(hand,ItemUtils.createFilledResult(s,p,new ItemStack(Items.BUCKET)));
            else if(kind==Kind.LAMP)p.setItemInHand(hand,ItemUtils.createFilledResult(s,p,new ItemStack(Items.GLASS_BOTTLE)));
            else s.shrink(1);
        }
        return ItemInteractionResult.sidedSuccess(l.isClientSide);
    }
    @Override protected void tick(BlockState state,ServerLevel l,BlockPos pos,RandomSource random){
        if(state.getValue(PHASE)==1&&kind.residual>0){
            l.setBlockAndUpdate(pos,state.setValue(PHASE,2));l.scheduleTick(pos,this,kind.residual);
        }else if(state.getValue(PHASE)!=0)l.setBlockAndUpdate(pos,state.setValue(PHASE,0));
    }
}
