package dev.minemods.thermal.content;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import java.util.List;

/** Rechargeable thermal capacity; damage component is display only, expiry is authoritative. */
public final class ThermalPackItem extends Item {
    public enum Kind {
        STONE(90,8,true,false), POUCH(180,12,true,true), CHARCOAL(300,14,true,false),
        FAN(5,5,false,true), CLOTH(90,8,false,true), FLASK(420,10,true,true);
        public final int seconds; public final double power; public final boolean hot,cold;
        Kind(int s,double p,boolean h,boolean c){seconds=s;power=p;hot=h;cold=c;}
    }
    private final Kind kind;
    public ThermalPackItem(Kind k){super(new Properties().durability(k.seconds+1));kind=k;}
    public Kind kind(){return kind;}
    public static long clock(ServerLevel l){return l.getServer().overworld().getGameTime();}
    private static CompoundTag data(ItemStack s){return s.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag();}
    public int mode(ItemStack s){int m=data(s).getInt("thermal_mode");return m==1&&kind.hot?1:m==-1&&kind.cold?-1:0;}
    public double remaining(ItemStack s,long now){
        if(mode(s)==0)return 0;
        long expiry=data(s).getLong("thermal_expiry");
        if(expiry<=now)return 0;
        return Math.min(1,(expiry-(double)now)/(kind.seconds*20.0));
    }
    public double displayCharge(ItemStack s){return mode(s)==0?0:Math.max(0,Math.min(1,1-s.getDamageValue()/(double)kind.seconds));}
    public boolean charge(ItemStack s,int mode,long now){
        if(mode!=1&&mode!=-1||mode==1&&!kind.hot||mode==-1&&!kind.cold)return false;
        if(mode(s)==mode&&remaining(s,now)>.95)return false;
        CompoundTag n=data(s);n.putInt("thermal_mode",mode);n.putLong("thermal_expiry",now+kind.seconds*20L);
        s.set(DataComponents.CUSTOM_DATA,CustomData.of(n));s.setDamageValue(0);return true;
    }
    public void refresh(ItemStack s,long now){
        int damage=kind.seconds-(int)Math.ceil(remaining(s,now)*kind.seconds);
        if(s.getDamageValue()!=damage)s.setDamageValue(damage);
    }
    public void neutralize(ItemStack s){
        CompoundTag n=data(s);n.remove("thermal_mode");n.remove("thermal_expiry");
        s.set(DataComponents.CUSTOM_DATA,CustomData.of(n));s.setDamageValue(kind.seconds);
    }
    @Override public void inventoryTick(ItemStack s,Level level,Entity entity,int slot,boolean selected){
        if(level instanceof ServerLevel l&&entity.tickCount%20==0)refresh(s,clock(l));
    }
    @Override public boolean isBarVisible(ItemStack s){return mode(s)!=0;}
    @Override public int getBarWidth(ItemStack s){return (int)Math.round(13*displayCharge(s));}
    public static int barColor(int mode,double charge){
        double t=Math.max(0,Math.min(1,charge));
        int low=mode<0?0x193b82:0x9b271c,mid=mode<0?0x398cd0:0xe98b36,high=mode<0?0xa9e7ff:0xffd47a;
        int a=t<.5?low:mid,b=t<.5?mid:high;double f=t<.5?t*2:(t-.5)*2;
        return mix(a,b,16,f)<<16|mix(a,b,8,f)<<8|mix(a,b,0,f);
    }
    private static int mix(int a,int b,int shift,double f){return (int)Math.round(((a>>shift)&255)*(1-f)+((b>>shift)&255)*f);}
    @Override public int getBarColor(ItemStack s){return barColor(mode(s),displayCharge(s));}
    @Override public void appendHoverText(ItemStack s,TooltipContext c,List<Component> lines,TooltipFlag f){
        lines.add(Component.translatable("tooltip.worldlogic_thermal.hotbar").withStyle(ChatFormatting.RED));
        lines.add(Component.translatable("tooltip.worldlogic_thermal."+kind.name().toLowerCase(java.util.Locale.ROOT)).withStyle(ChatFormatting.GRAY));
        int charge=(int)Math.round(100*displayCharge(s));String state=charge==0?"neutral":mode(s)>0?"hot":"cold";
        lines.add(Component.translatable("tooltip.worldlogic_thermal."+state,charge).withStyle(charge==0?ChatFormatting.GRAY:mode(s)>0?ChatFormatting.GOLD:ChatFormatting.AQUA));
    }
    @Override public InteractionResultHolder<ItemStack> use(Level level,Player p,InteractionHand hand){
        ItemStack s=p.getItemInHand(hand);
        if(hand!=InteractionHand.MAIN_HAND||kind!=Kind.FAN)return InteractionResultHolder.pass(s);
        if(level instanceof ServerLevel l&&charge(s,-1,clock(l)))p.causeFoodExhaustion(.12f);
        return InteractionResultHolder.sidedSuccess(s,level.isClientSide);
    }
    public static boolean heatSource(BlockState s){
        if(s.getBlock() instanceof FixtureBlock f)return !f.cooling()&&s.getValue(FixtureBlock.ACTIVE);
        if(s.getBlock() instanceof ClimateDeviceBlock f)return f.heat(s)>0;
        return s.hasProperty(BlockStateProperties.LIT)&&s.getValue(BlockStateProperties.LIT)
            &&(s.is(Blocks.CAMPFIRE)||s.is(Blocks.SOUL_CAMPFIRE)||s.is(Blocks.FURNACE)||s.is(Blocks.BLAST_FURNACE)||s.is(Blocks.SMOKER));
    }
    public static boolean coldSource(BlockState s){return s.is(Blocks.ICE)||s.is(Blocks.PACKED_ICE)||s.is(Blocks.BLUE_ICE)||s.is(Blocks.SNOW_BLOCK);}
    public static void interact(PlayerInteractEvent.RightClickBlock e){
        if(e.getHand()!=InteractionHand.MAIN_HAND||e.getEntity().isSpectator()||!(e.getItemStack().getItem() instanceof ThermalPackItem item))return;
        if(item.kind==Kind.FAN)return;
        var p=e.getEntity();var s=e.getLevel().getBlockState(e.getPos());int mode=0;
        boolean water=item.kind==Kind.CLOTH&&s.is(Blocks.WATER_CAULDRON);
        if(item.kind.hot&&heatSource(s))mode=1;
        else if(item.kind.cold&&(item.kind!=Kind.CLOTH&&coldSource(s)||water))mode=-1;
        if(mode==0)return;
        if(e.getLevel() instanceof ServerLevel l){
            if(!l.mayInteract(p,e.getPos()))return;
            ItemStack fuel=p.getOffhandItem();
            boolean coal=item.kind==Kind.CHARCOAL;
            if(coal&&!fuel.is(Items.COAL)&&!fuel.is(Items.CHARCOAL))return;
            if(item.charge(e.getItemStack(),mode,clock(l))&&!p.getAbilities().instabuild){
                if(coal)fuel.shrink(1);
                if(water){
                    int fill=s.getValue(LayeredCauldronBlock.LEVEL);
                    l.setBlockAndUpdate(e.getPos(),fill<=1?Blocks.CAULDRON.defaultBlockState():s.setValue(LayeredCauldronBlock.LEVEL,fill-1));
                }
            }
        }
        e.setCancellationResult(InteractionResult.SUCCESS);e.setCanceled(true);
    }
    public record Power(double heat,double cool){}
    public static Power hotbar(ServerPlayer p,double humidity){
        long now=clock(p.serverLevel());double heat=0,cool=0;
        for(int i=0;i<9;i++){
            ItemStack s=p.getInventory().getItem(i);
            if(!(s.getItem() instanceof ThermalPackItem item))continue;
            double remaining=item.remaining(s,now),power=remaining*item.kind.power;
            if(item.mode(s)>0)heat=Math.max(heat,power);
            else if(item.mode(s)<0){
                if(item.kind==Kind.CLOTH||item.kind==Kind.FAN)power*=Math.max(.15,1-humidity/100);
                cool=Math.max(cool,power);
            }
        }
        return new Power(heat,cool);
    }
    public static Power comfort(Power p,double effective){
        // No portable-item contribution when already comfortably warm/cool.
        return new Power(Math.min(p.heat(),Math.max(0,22-effective)),Math.min(p.cool(),Math.max(0,effective-26)));
    }
}
