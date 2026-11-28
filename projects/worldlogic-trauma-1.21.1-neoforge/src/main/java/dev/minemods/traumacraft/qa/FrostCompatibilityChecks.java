package dev.minemods.traumacraft.qa;

import dev.minemods.traumacraft.api.TraumaApi;
import dev.minemods.traumacraft.health.*;
import dev.minemods.traumacraft.integration.LegacyFrostExposure;
import dev.minemods.traumacraft.event.TraumaEvents;
import dev.minemods.traumacraft.network.SyncBodyStatePacket;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;

public final class FrostCompatibilityChecks {
    private FrostCompatibilityChecks(){}
    private static void require(boolean value,String label){if(!value)throw new IllegalStateException("Frost QA: "+label);}
    public static void verify(ServerPlayer p){
        require(LegacyFrostExposure.legacyVersion("0.7.0-rc1")&&LegacyFrostExposure.legacyVersion("0.3.1-rc1"),"legacy sender absent versions");
        require(!LegacyFrostExposure.legacyVersion("0.8.0-rc1")&&!LegacyFrostExposure.legacyVersion("0.8.1-rc1")&&!LegacyFrostExposure.legacyVersion("1.0.0")&&!LegacyFrostExposure.legacyVersion("unknown"),"modern and unknown providers never duplicate");
        var b=BodyStates.get(p);b.reset();
        var doses=new CompoundTag();float hearts=p.getHealth();
        for(int i=0;i<51;i++)require(!LegacyFrostExposure.advance(b,doses,-17,0,0,0),"no early injury");
        require(LegacyFrostExposure.advance(b,doses,-17,0,0,0),"52 seconds unprotected -17 C produces local injury");
        for(var limb:LegacyFrostExposure.LIMBS){
            require(b.health(limb)==limb.maximum-1.5f&&(b.wounds(limb)&WoundKind.FROSTBITE.bit)!=0&&b.bleeding(limb)==0,"frost injury "+limb);
            require(doses.getDouble(limb.name())==60,"post-injury dose");
        }
        require(b.health(BodyPart.HEAD)==35&&b.health(BodyPart.CHEST)==85&&p.getHealth()==hearts,"no torso frost or duplicate hearts");
        var packet=SyncBodyStatePacket.from(b,0);
        require((packet.wounds()[BodyPart.LEFT_ARM.ordinal()]&WoundKind.FROSTBITE.bit)!=0,"body HUD packet contains frost wound");
        b.reset();doses=new CompoundTag();
        for(int i=0;i<90;i++)LegacyFrostExposure.advance(b,doses,-17,0,1,1);
        require(!b.hasInjury(),"insulated limbs protected");
        doses=new CompoundTag();
        for(int i=0;i<52;i++)LegacyFrostExposure.advance(b,doses,-17,0,0,1);
        require(b.health(BodyPart.LEFT_ARM)==58.5f&&b.health(BodyPart.LEFT_LEG)==65,"separate arm and leg insulation");
        double before=doses.getDouble("LEFT_ARM");LegacyFrostExposure.advance(b,doses,20,0,0,1);
        require(doses.getDouble("LEFT_ARM")<before,"warming reduces dose");
        b.reset();
        for(var zone:new TraumaApi.Zone[]{TraumaApi.Zone.LEFT_ARM,TraumaApi.Zone.RIGHT_ARM,TraumaApi.Zone.LEFT_LEG,TraumaApi.Zone.RIGHT_LEG})
            require(TraumaApi.reportInjury(p,zone,TraumaApi.Injury.FROSTBITE,1.5f,0),"modern public sender accepted "+zone);
        require(TraumaApi.status(p).orElseThrow().regions().stream().filter(r->r.injuries().contains(TraumaApi.Injury.FROSTBITE)).count()==4,"four modern API limb injuries");
        require(p.getHealth()==hearts,"modern local injury never adds vanilla damage");
        b.reset();
        TraumaEvents.applyDamage(p,b,p.damageSources().freeze(),2);
        for(var limb:LegacyFrostExposure.LIMBS)require(b.health(limb)==limb.maximum-1.5f&&(b.wounds(limb)&WoundKind.FROSTBITE.bit)!=0,"vanilla freeze local "+limb);
        require(b.health(BodyPart.CHEST)==85,"vanilla freeze no torso duplication");
    }
}
