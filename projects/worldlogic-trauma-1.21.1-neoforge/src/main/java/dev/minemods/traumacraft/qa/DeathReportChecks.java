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
            var history=new TraumaHistory();
            var before=TraumaHistory.Snapshot.of(b);b.damage(BodyPart.LEFT_ARM,10);
            history.observe(100,TraumaHistory.Kind.IMPACT,Origin.ATTACK,before,b);
            var events=history.snapshot(140);
            require(events.size()==1&&events.get(0).ageSeconds()==2&&events.get(0).hpLoss()==10&&events.get(0).regions()==(1<<BodyPart.LEFT_ARM.ordinal()),"observed hit delta and age");
            before=TraumaHistory.Snapshot.of(b);history.observe(141,TraumaHistory.Kind.IMPACT,Origin.FALL,before,b);
            require(history.snapshot(141).size()==1,"no invented damage event");
            for(int i=0;i<20;i++){before=TraumaHistory.Snapshot.of(b);b.damage(BodyPart.RIGHT_ARM,1);history.observe(150+i,TraumaHistory.Kind.IMPACT,Origin.ATTACK,before,b);}
            require(history.snapshot(180).size()==12&&history.snapshot(3000).isEmpty(),"bounded history and expiry");
            b.reset();DeathReports.resetHistory(p);before=TraumaHistory.Snapshot.of(b);b.damage(BodyPart.HEAD,35);
            DeathReports.observe(p,TraumaHistory.Kind.IMPACT,Origin.FALL,before,b);
            var archived=DeathReports.attachHistory(p,DeathReport.capture(b,Cause.DIRECT_TRAUMA,Origin.FALL,"fall"));
            require(!archived.history().isEmpty(),"server timeline attached");
            DeathReports.remember(p,archived);b.reset();DeathReports.resetHistory(p);
            var restored=DeathReports.last(p).orElseThrow();
            require(restored.history().equals(archived.history())&&restored.critical(BodyPart.HEAD)&&restored.body().hp()[0]==0&&b.health(BodyPart.HEAD)==35,"archive independent of new body");
            var archiveBuffer=new RegistryFriendlyByteBuf(io.netty.buffer.Unpooled.buffer(),p.registryAccess());
            try{DeathReportPacket.CODEC.encode(archiveBuffer,new DeathReportPacket(restored,true));var decoded=DeathReportPacket.CODEC.decode(archiveBuffer);require(decoded.archived()&&decoded.report().history().equals(restored.history()),"archive flag and history packet");}finally{archiveBuffer.release();}
            p.getPersistentData().putByteArray("traumacraft_last_death_v1",new byte[]{1,2});
            require(DeathReports.last(p).isEmpty(),"corrupt archive safely ignored");
            p.getPersistentData().remove("traumacraft_last_death_v1");
            b.reset();
            var event=new net.neoforged.neoforge.event.entity.living.LivingDamageEvent.Pre(p,new net.neoforged.neoforge.common.damagesource.DamageContainer(kill,100f));
            net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(event);
            require(event.getNewDamage()==100f&&b.health(BodyPart.CHEST)==85&&!b.hasInjury(),"native kill preserves true pre-death anatomy");
        }finally{DeathReports.clear(p);b.reset();}
    }
}
