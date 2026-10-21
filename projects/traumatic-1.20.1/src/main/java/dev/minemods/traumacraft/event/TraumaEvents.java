package dev.minemods.traumacraft.event;
import dev.minemods.traumacraft.combat.HitLocation;
import dev.minemods.traumacraft.health.BodyPart;
import dev.minemods.traumacraft.health.BodyState;
import dev.minemods.traumacraft.health.BodyStates;
import dev.minemods.traumacraft.network.ModNetwork;
import java.util.UUID;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;

public final class TraumaEvents {
    private static final UUID LIMPING=UUID.fromString("44d8a143-8d2e-47d8-94d7-32a114080001");
    private static final ThreadLocal<Boolean> BLOOD=ThreadLocal.withInitial(()->false);
    private TraumaEvents() {}
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent e){
        if(e.getEntity() instanceof ServerPlayer p)ModNetwork.sync(p);
    }
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent e){
        if(e.getEntity() instanceof ServerPlayer p)ModNetwork.sync(p);
    }
    public static void onClone(PlayerEvent.Clone e){
        BodyStates.copy(e.getOriginal(),e.getEntity(),e.isWasDeath());
    }
    public static void onDamage(LivingDamageEvent event) {
        if(!(event.getEntity() instanceof ServerPlayer player)||BLOOD.get())return;
        float amount=event.getAmount();
        if(amount<=0)return;
        BodyState state=BodyStates.get(player);
        boolean falling=event.getSource().is(DamageTypeTags.IS_FALL);
        boolean explosive=event.getSource().is(DamageTypeTags.IS_EXPLOSION);
        boolean thermal=event.getSource().is(DamageTypeTags.IS_FIRE);
        if(falling){
            apply(state,player,BodyPart.LEFT_LEG,amount*5f,true,false);
            apply(state,player,BodyPart.RIGHT_LEG,amount*5f,true,false);
        }else if(explosive){
            for(BodyPart part:BodyPart.values()){
                float factor=part==BodyPart.CHEST?1.5f:part==BodyPart.HEAD?0.7f:0.6f;
                apply(state,player,part,amount*factor,false,true);
            }
        }else{
            BodyPart part=thermal?BodyPart.CHEST:HitLocation.locate(player,event.getSource());
            apply(state,player,part,amount*8f,false,thermal);
        }
        if(state.health(BodyPart.HEAD)<=0||state.health(BodyPart.CHEST)<=0)
            event.setAmount(Math.max(amount,player.getHealth()+player.getAbsorptionAmount()+1f));
        BodyStates.save(player,state);
        ModNetwork.sync(player);
        if(state.hasBleeding()||state.hasFracturedLeg())
            player.displayClientMessage(Component.literal("Trauma: "+summary(state)+" | /trauma"),true);
    }
    private static void apply(BodyState state,ServerPlayer p,BodyPart part,float value,boolean falling,boolean blunt){
        state.damage(part,value);
        if(falling&&part.isLeg()&&value>=17)state.setFractured(part,true);
        else if(!falling&&blunt&&part.isLeg()&&value>=22&&p.getRandom().nextFloat()<0.4f)
            state.setFractured(part,true);
        if(!falling&&!blunt&&value>=12){
            float roll=p.getRandom().nextFloat();
            if(value>=24&&roll<0.35f)state.setBleeding(part,2);
            else if(roll<0.6f)state.setBleeding(part,1);
        }
    }
    public static void onTick(LivingEvent.LivingTickEvent event){
        if(!(event.getEntity() instanceof ServerPlayer player)||player.tickCount%20!=0)return;
        BodyState state=BodyStates.get(player);
        updateMovement(player,state.legPenalty());
        if(player.tickCount%40==0&&state.hasBleeding()){
            int severity=0;
            for(BodyPart zone:BodyPart.values())severity+=state.bleeding(zone);
            try{BLOOD.set(true);player.hurt(player.damageSources().magic(),Math.min(3f,severity*.5f));}
            finally{BLOOD.set(false);}
        }
        if(player.tickCount%40==0)ModNetwork.sync(player);
        if(player.tickCount%100==0&&(state.hasBleeding()||state.hasFracturedLeg()))
            player.displayClientMessage(Component.literal("Trauma: "+summary(state)+" | /trauma"),true);
    }
    public static void updateMovement(ServerPlayer p,float penalty){
        AttributeInstance attribute=p.getAttribute(Attributes.MOVEMENT_SPEED);
        if(attribute==null)return;
        AttributeModifier old=attribute.getModifier(LIMPING);
        if(penalty==0){if(old!=null)attribute.removeModifier(LIMPING);}
        else if(old==null||Math.abs(old.getAmount()+penalty)>0.0001){
            if(old!=null)attribute.removeModifier(LIMPING);
            attribute.addTransientModifier(new AttributeModifier(LIMPING,"Traumatic limping",-penalty,AttributeModifier.Operation.MULTIPLY_TOTAL));
        }
    }
    private static String summary(BodyState state){
        int b=0;for(BodyPart zone:BodyPart.values())b+=state.bleeding(zone);
        return (b>0?"bleeding x"+b:"no bleeding")+(state.hasFracturedLeg()?", fractured leg":"");
    }
}
