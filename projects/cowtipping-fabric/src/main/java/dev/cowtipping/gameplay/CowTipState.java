package dev.cowtipping.gameplay;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.entity.player.Player;
import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;
public final class CowTipState {
    public static final int FALL_TICKS=8, HOLD_UNTIL_TICK=55, ANIMATION_TICKS=73, ATTENTION_TICKS=95, COOLDOWN_TICKS=150;
    public static final byte TIP_LEFT_EVENT=110, TIP_RIGHT_EVENT=111;
    private record TipInfo(int startedAt,int direction){}
    private static final Map<Cow,TipInfo> STATES=Collections.synchronizedMap(new WeakHashMap<>());
    private CowTipState(){}
    public static boolean canTip(Cow cow){
        if(!cow.isAlive() || !cow.onGround() || cow.isInWaterOrBubble() || cow.isPassenger() || cow.isVehicle()) return false;
        TipInfo info=STATES.get(cow);
        return info==null || cow.tickCount-info.startedAt>=COOLDOWN_TICKS;
    }
    public static void tip(Cow cow,Player player){
        if(!(cow.level() instanceof ServerLevel level) || !canTip(cow)) return;
        double yaw=Math.toRadians(cow.getYRot());
        double side=(player.getX()-cow.getX())*Math.cos(yaw)+(player.getZ()-cow.getZ())*Math.sin(yaw);
        int direction=side>=0?1:-1;
        STATES.put(cow,new TipInfo(cow.tickCount,direction));
        cow.getNavigation().stop();
        level.broadcastEntityEvent(cow,direction<0?TIP_LEFT_EVENT:TIP_RIGHT_EVENT);
        level.playSound(null,cow.blockPosition(),SoundEvents.WOOL_HIT,SoundSource.NEUTRAL,0.85F,0.73F+cow.getRandom().nextFloat()*0.12F);
    }
    public static void onEntityEvent(Cow cow,byte eventId){
        STATES.put(cow,new TipInfo(cow.tickCount,eventId==TIP_LEFT_EVENT?-1:1));
    }
    public static boolean isTipEvent(byte id){return id==TIP_LEFT_EVENT || id==TIP_RIGHT_EVENT;}
    public static int age(Cow cow){
        TipInfo info=STATES.get(cow);
        return info==null?Integer.MAX_VALUE:Math.max(0,cow.tickCount-info.startedAt);
    }
    public static boolean isTipped(Cow cow){return age(cow)<ANIMATION_TICKS;}
    public static boolean attractsAttention(Cow cow){return cow.isAlive() && age(cow)<ATTENTION_TICKS;}
    public static float rotationDegrees(Cow cow,float partialTick){
        TipInfo info=STATES.get(cow);
        if(info==null) return 0F;
        float elapsed=cow.tickCount-info.startedAt+partialTick;
        if(elapsed>=ANIMATION_TICKS) return 0F;
        float amount;
        if(elapsed<FALL_TICKS){
            float t=Mth.clamp(elapsed/FALL_TICKS,0F,1F);
            amount=1F-(1F-t)*(1F-t);
        }else if(elapsed<HOLD_UNTIL_TICK) amount=1F;
        else{
            float t=Mth.clamp((elapsed-HOLD_UNTIL_TICK)/(ANIMATION_TICKS-HOLD_UNTIL_TICK),0F,1F);
            amount=1F-t*t*(3F-2F*t);
        }
        return 88F*info.direction*amount;
    }
}
