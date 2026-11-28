package dev.minemods.traumacraft.qa;

import dev.minemods.traumacraft.api.TraumaApi;
import dev.minemods.traumacraft.health.*;
import dev.minemods.traumacraft.integration.WorldLogicBridge;
import dev.minemods.traumacraft.network.SyncBodyStatePacket;
import dev.minemods.worldlogic.Rules;
import dev.minemods.worldlogic.api.WorldLogicApi;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.level.GameType;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.damagesource.DamageContainer;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import java.util.LinkedHashMap;

/** Real registered damage-event listener on official server; emulates Core ownership switch state. */
public final class OwnershipChecks {
    private OwnershipChecks(){}
    private static void require(boolean value,String label){if(!value)throw new IllegalStateException("Ownership QA: "+label);}
    private static float post(ServerPlayer p,DamageSource source,float amount){
        var e=new LivingDamageEvent.Pre(p,new DamageContainer(source,amount));
        NeoForge.EVENT_BUS.post(e);return e.getNewDamage();
    }
    public static void verify(ServerPlayer p){
        var saved=Rules.snapshot();
        try{
            var disabled=new LinkedHashMap<>(saved);
            disabled.put("preset","custom");
            disabled.put("injuries","false");
            disabled.put("injury_aggravation","false");
            disabled.put("shoulder_impact","false");
            require(Rules.apply(disabled),"fixture rules applied");
            require(!WorldLogicApi.ruleEnabled("injuries")&&!WorldLogicApi.ruleEnabled("shoulder_impact"),"Core legacy rules disabled");
            var b=BodyStates.get(p);b.reset();
            require(WorldLogicBridge.injuryEnabled(p)&&WorldLogicBridge.functionsEnabled(p),"Trauma stays active when Core delegates");
            require(post(p,p.damageSources().fall(),5)==0,"vanilla HP damage disabled for nonlethal anatomical fall");
            require(b.health(BodyPart.LEFT_LEG)==45&&b.health(BodyPart.RIGHT_LEG)==45&&b.fractured(BodyPart.LEFT_LEG)&&b.fractured(BodyPart.RIGHT_LEG),"one registered fall event damages legs exactly once");
            require(b.health(BodyPart.HEAD)==35&&b.health(BodyPart.CHEST)==85,"unrelated regions untouched");
            require(post(p,p.damageSources().inFire(),2)==0,"vanilla damage disabled for nonlethal fire");
            require(b.health(BodyPart.CHEST)==79&&(b.wounds(BodyPart.CHEST)&WoundKind.BURN.bit)!=0,"registered fire event still damages anatomy");
            post(p,p.damageSources().fall(),0);
            require(b.health(BodyPart.LEFT_LEG)==45,"zero damage ignored");
            var packet=SyncBodyStatePacket.from(b,0);
            require(packet.hp()[BodyPart.LEFT_LEG.ordinal()]==45&&packet.fracture()[BodyPart.RIGHT_LEG.ordinal()],"actual body packet has damaged legs");
            require(p.getPersistentData().contains(BodyStates.KEY),"damage event persisted anatomy");
            var restored=new BodyState();
            restored.deserializeNBT(p.registryAccess(),p.getPersistentData().getCompound(BodyStates.KEY));
            require(restored.health(BodyPart.CHEST)==79&&restored.fractured(BodyPart.LEFT_LEG),"persisted damage and fracture");
            require(TraumaApi.reportInjury(p,TraumaApi.Zone.LEFT_ARM,TraumaApi.Injury.FROSTBITE,4,0),"external addon injury accepted under delegation");
            require(TraumaApi.status(p).orElseThrow().regions().stream().anyMatch(r->r.zone()==TraumaApi.Zone.LEFT_ARM&&r.health()==56),"external injury visible");
            require(WorldLogicBridge.mergePenalty(p,0)>=.42f,"limb movement restriction survives delegation");
            p.setHealth(1);VitalityBridge.maintainBackingHealth(p);
            require(p.getHealth()==p.getMaxHealth(),"native backing restored independently of anatomy");
            p.gameMode.changeGameModeForPlayer(GameType.CREATIVE);
            require(post(p,p.damageSources().fall(),5)==5,"creative native damage untouched");
            require(b.health(BodyPart.LEFT_LEG)==45&&!WorldLogicBridge.functionsEnabled(p)&&TraumaApi.status(p).isEmpty(),"creative excluded");
            p.gameMode.changeGameModeForPlayer(GameType.SPECTATOR);
            post(p,p.damageSources().fall(),5);
            require(b.health(BodyPart.LEFT_LEG)==45&&!WorldLogicBridge.functionsEnabled(p),"spectator excluded");
            p.gameMode.changeGameModeForPlayer(GameType.SURVIVAL);b.reset();
            b.damage(BodyPart.LEFT_LEG,65);b.damage(BodyPart.RIGHT_LEG,65);
            require(post(p,p.damageSources().fall(),100)>0&&b.vitalDestroyed(),"vital destruction permits lethal native damage");
            b.reset();
            require(post(p,p.damageSources().fellOutOfWorld(),1)>0,"void damage is not suppressed");
        }finally{
            p.gameMode.changeGameModeForPlayer(GameType.SURVIVAL);
            Rules.apply(saved);
        }
    }
}
