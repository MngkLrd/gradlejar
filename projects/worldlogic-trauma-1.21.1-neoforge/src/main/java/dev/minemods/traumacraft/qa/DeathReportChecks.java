package dev.minemods.traumacraft.qa;
import dev.minemods.traumacraft.health.*;
import dev.minemods.traumacraft.health.DeathReport.Cause;
import dev.minemods.traumacraft.health.DeathReport.Origin;
import dev.minemods.traumacraft.event.DeathReports;
import dev.minemods.traumacraft.event.TraumaEvents;
import dev.minemods.traumacraft.network.DeathReportPacket;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;

public final class DeathReportChecks {
    private static void require(boolean ok,String message){if(!ok)throw new IllegalStateException("Death report: "+message);}
    public static void verify(ServerPlayer p,BodyState b){
        var fall=p.damageSources().fall();var kill=p.damageSources().genericKill();
        try{
            b.reset();DeathReports.clear(p);
            TraumaEvents.applyDamage(p,b,fall,100);
            require(b.vitalDestroyed(),"fall fixture lethal");
            DeathReports.hit(p,b,fall,false);
            var report=DeathReports.consume(p,b,fall);
            require(report.cause()==Cause.DIRECT_TRAUMA&&report.origin()==Origin.FALL,"direct fall cause and source");
            require(report.critical(BodyPart.CHEST)&&!report.critical(BodyPart.LEFT_LEG)&&report.body().hp()[BodyPart.LEFT_LEG.ordinal()]==0,"fatal torso distinguished from destroyed leg");
            b.reset();
            require(report.body().hp()[BodyPart.LEFT_LEG.ordinal()]==0&&report.critical(BodyPart.CHEST),"frozen snapshot survives reset");
            b.reset();b.wound(BodyPart.LEFT_ARM,60,WoundKind.CUT);b.setBleeding(BodyPart.LEFT_ARM,2);b.loseBlood(4200);
            DeathReports.collapse(p,b,Cause.EXTERNAL_BLEEDING);
            var external=DeathReports.consume(p,b,kill);
            require(external.cause()==Cause.EXTERNAL_BLEEDING&&external.origin()==Origin.SYSTEMIC&&external.bleeding(BodyPart.LEFT_ARM)&&external.criticalRegions()==0&&external.body().bloodMl()==800,"external bleed no invented lethal zone");
            b.reset();b.impact(BodyPart.STOMACH,48,WoundKind.LACERATION,OrganState.Organ.LIVER);b.loseBlood(4200);
            DeathReports.collapse(p,b,Cause.INTERNAL_BLEEDING);
            var internal=DeathReports.consume(p,b,kill);
            require(internal.cause()==Cause.INTERNAL_BLEEDING&&internal.bleeding(OrganState.Organ.LIVER)&&!internal.bleeding(OrganState.Organ.HEART)&&internal.criticalOrgans()==0,"internal bleeding source distinguished");
            b.reset();for(int i=0;i<180&&!b.vitalDestroyed();i++)b.physiologySecond(false,false,1);
            require(b.organs().brainDestroyed()&&b.health(BodyPart.HEAD)==BodyPart.HEAD.maximum,"hypoxic death without skull damage");
            DeathReports.collapse(p,b,Cause.HYPOXIA);
            var hypoxia=DeathReports.consume(p,b,kill);
            require(hypoxia.cause()==Cause.HYPOXIA&&hypoxia.critical(OrganState.Organ.BRAIN)&&hypoxia.critical(BodyPart.HEAD)&&!hypoxia.critical(BodyPart.CHEST),"brain failure not falsely attributed to chest");
            var buffer=new RegistryFriendlyByteBuf(io.netty.buffer.Unpooled.buffer(),p.registryAccess());
            try{
                DeathReportPacket.CODEC.encode(buffer,new DeathReportPacket(hypoxia));
                var decoded=DeathReportPacket.CODEC.decode(buffer).report();
                require(decoded.cause()==hypoxia.cause()&&decoded.origin()==hypoxia.origin()&&decoded.criticalOrgans()==hypoxia.criticalOrgans()&&decoded.body().clinical().integrity()[0]==0&&decoded.body().hp()[0]==35,"actual report codec roundtrip");
            }finally{buffer.release();}
            b.reset();b.damage(BodyPart.HEAD,35);DeathReports.hit(p,b,fall,false);
            var nativeKill=DeathReports.consume(p,b,kill);
            require(nativeKill.cause()==Cause.VANILLA&&nativeKill.origin()==Origin.COMMAND&&nativeKill.criticalRegions()==0,"unrelated command kill not attributed to pending fall");
            b.reset();b.damage(BodyPart.HEAD,35);DeathReports.hit(p,b,fall,false);DeathReports.collapse(p,b,null);
            require(DeathReports.consume(p,b,kill).origin()==Origin.FALL,"engine collapse retains direct fatal hit context");
            b.reset();DeathReports.hit(p,b,fall,false);
            require(DeathReports.consume(p,b,kill).cause()==Cause.VANILLA,"nonlethal hit cannot become cause");
            b.reset();
            var event=new net.neoforged.neoforge.event.entity.living.LivingDamageEvent.Pre(p,new net.neoforged.neoforge.common.damagesource.DamageContainer(kill,100f));
            net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(event);
            require(event.getNewDamage()==100f&&b.health(BodyPart.CHEST)==85&&!b.hasInjury(),"native kill preserves true pre-death anatomy");
        }finally{DeathReports.clear(p);b.reset();}
    }
}
