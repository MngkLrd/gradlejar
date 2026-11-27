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
        l.setBlockAndUpdate(p,s.setValue(PHASE,1));l.scheduleTick(p,this,kind.burn);return true;
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
