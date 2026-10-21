package dev.minemods.traumacraft.event;
import dev.minemods.traumacraft.TraumaCraft;
import dev.minemods.traumacraft.combat.HitLocation;
import dev.minemods.traumacraft.health.BodyPart;
import dev.minemods.traumacraft.health.BodyState;
import dev.minemods.traumacraft.health.BodyStates;
import dev.minemods.traumacraft.network.ModNetwork;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

public final class TraumaEvents {
    private static final Identifier LIMPING_ID=Identifier.fromNamespaceAndPath(TraumaCraft.MOD_ID,"limping");
    private static final ThreadLocal<Boolean> BLEEDING_DAMAGE=ThreadLocal.withInitial(()->false);
    private TraumaEvents(){}
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent e){
        if(e.getEntity() instanceof ServerPlayer p)ModNetwork.sync(p);
    }
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent e){
        if(e.getEntity() instanceof ServerPlayer p)ModNetwork.sync(p);
    }
    public static void onClone(PlayerEvent.Clone e){
        BodyStates.copy(e.getOriginal(),e.getEntity(),e.isWasDeath());
    }
    public static void onDamage(LivingDamageEvent.Pre event) {
        if(!(event.getEntity() instanceof ServerPlayer player)||BLEEDING_DAMAGE.get())return;
        float amount=event.getNewDamage();
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
            event.setNewDamage(Math.max(amount,player.getHealth()+player.getAbsorptionAmount()+1f));
        BodyStates.save(player,state);
        ModNetwork.sync(player);
        if(state.hasBleeding()||state.hasFracturedLeg())
            player.sendSystemMessage(Component.literal("Trauma: "+summary(state)+" | /trauma"));
    }
    private static void apply(BodyState state,ServerPlayer p,BodyPart part,float value,boolean falling,boolean blunt) {
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
    public static void onTick(EntityTickEvent.Post event){
        if(!(event.getEntity() instanceof ServerPlayer player)||player.tickCount%20!=0)return;
        BodyState state=BodyStates.get(player);
        updateMovement(player,state.legPenalty());
        if(player.tickCount%40==0&&state.hasBleeding()){
            int severity=0;for(BodyPart zone:BodyPart.values())severity+=state.bleeding(zone);
            try{BLEEDING_DAMAGE.set(true);player.hurtServer((ServerLevel)player.level(),player.damageSources().magic(),Math.min(3f,severity*.5f));}
            finally{BLEEDING_DAMAGE.set(false);}
        }
        if(player.tickCount%40==0)ModNetwork.sync(player);
        if(player.tickCount%100==0&&(state.hasBleeding()||state.hasFracturedLeg()))
            player.sendSystemMessage(Component.literal("Trauma: "+summary(state)+" | /trauma"));
    }
    public static void updateMovement(ServerPlayer player,float penalty){
        AttributeInstance attr=player.getAttribute(Attributes.MOVEMENT_SPEED);
        if(attr==null)return;
        var old=attr.getModifier(LIMPING_ID);
        if(penalty==0){if(old!=null)attr.removeModifier(LIMPING_ID);}
        else if(old==null||Math.abs(old.amount()+penalty)>0.0001)
            attr.addOrUpdateTransientModifier(new AttributeModifier(LIMPING_ID,-penalty,AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
    }
    private static String summary(BodyState state){
        int b=0;for(BodyPart zone:BodyPart.values())b+=state.bleeding(zone);
        return (b>0?"bleeding x"+b:"no bleeding")+(state.hasFracturedLeg()?", fractured leg":"");
    }
}
