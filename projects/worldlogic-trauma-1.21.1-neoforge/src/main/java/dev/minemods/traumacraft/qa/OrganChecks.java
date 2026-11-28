package dev.minemods.traumacraft.qa;
import dev.minemods.traumacraft.health.*;
import dev.minemods.traumacraft.api.TraumaApi;
import dev.minemods.traumacraft.event.TraumaEvents;
import net.minecraft.server.level.ServerPlayer;
public final class OrganChecks {
    private static void require(boolean ok,String message){if(!ok)throw new IllegalStateException("Organ QA: "+message);}
    public static void verify(ServerPlayer p,BodyState b){
        var O=OrganState.Organ.BRAIN;var o=b.organs();
        require(o.oxygen()==1&&o.perfusion(1)==1&&o.internalLossPerSecond()==0&&PhysicalFunction.systemic(b)==1,"healthy baseline");
        var snapshot=TraumaApi.physiology(p).orElseThrow();
        require(snapshot.organs().size()==9&&snapshot.organs().stream().allMatch(x->x.integrity()==1),"nine healthy organs");
        b.impact(BodyPart.CHEST,5,WoundKind.CONTUSION);
        require(o.ventilation()==1&&o.internalLossPerSecond()==0,"minor impact spared organs");
        b.impact(BodyPart.CHEST,20,WoundKind.BURN);
        require(o.ventilation()==1,"burn is not penetrating trauma");
        b.reset();b.impact(BodyPart.LEFT_ARM,30,WoundKind.LACERATION);
        require(o.internalLossPerSecond()==0&&o.integrity(O)==1,"limb wound not an organ wound");
        b.reset();b.impact(BodyPart.CHEST,45,WoundKind.PUNCTURE);
        require(o.ventilation()<1&&o.integrity(OrganState.Organ.HEART)<1&&o.internalLossPerSecond()>0,"chest impact damages heart and lungs");
        require(o.integrity(OrganState.Organ.LIVER)==1&&o.integrity(O)==1,"unrelated organs untouched");
        float volume=b.bloodMl(),loss=o.internalLossPerSecond();
        b.physiologySecond(true,false,1);
        require(Math.abs(b.bloodMl()-(volume-loss))<.002&&!b.hasBleeding()&&b.hasAnyBleeding(),"single internal blood deduction, no external wound flag");
        b.recoverBlood(100);require(Math.abs(b.bloodMl()-(volume-loss))<.002,"no recovery during internal bleed");
        require(PhysicalFunction.systemic(b)<1,"functional compromise");
        var saved=b.serializeNBT(p.registryAccess());var loaded=new BodyState();loaded.deserializeNBT(p.registryAccess(),saved);
        require(loaded.organs().save().equals(o.save())&&loaded.bloodMl()==b.bloodMl(),"persist organ integrity, oxygen, and leaks");
        saved.remove("organs_v1");loaded.deserializeNBT(p.registryAccess(),saved);
        require(loaded.organs().ventilation()==1&&loaded.bloodMl()==b.bloodMl(),"legacy saves preserve HP and blood; no invented organ injury");
        require(snapshot.organs().stream().allMatch(x->x.integrity()==1),"previous API snapshot immutable");
        b.reset();b.impact(BodyPart.STOMACH,48,WoundKind.LACERATION);
        require(o.integrity(OrganState.Organ.LIVER)<1&&o.renalFunction()<1&&o.digestiveFunction()<1&&o.repairFactor(1)<1,"abdominal organ functions");
        require(o.integrity(OrganState.Organ.HEART)==1&&o.ventilation()==1,"abdomen does not damage heart and lungs");
        require(!b.stabilizeInternal(BodyPart.LEFT_ARM),"wrong site cannot stabilize");
        b.reset();b.impact(BodyPart.CHEST,80,WoundKind.PUNCTURE);
        float severe=o.internalLossPerSecond(),heartBefore=o.integrity(OrganState.Organ.HEART);
        b.heal(BodyPart.CHEST,85);
        require(MedicalAction.canApply(b,dev.minemods.traumacraft.item.MedicalItem.Type.TRAUMA_KIT,BodyPart.CHEST),"internal bleed remains treatable despite healed external HP");
        require(MedicalAction.apply(p,b,dev.minemods.traumacraft.item.MedicalItem.Type.TRAUMA_KIT,BodyPart.CHEST)&&o.internalLossPerSecond()<severe&&o.internalLossPerSecond()>0&&o.integrity(OrganState.Organ.HEART)==heartBefore,"advanced support reduces severe leak without magic closure or organ repair");
        b.reset();b.impact(BodyPart.CHEST,32,WoundKind.CONTUSION);
        float mild=o.internalLossPerSecond();require(mild>0&&mild<2.5,"mild leak");
        for(int i=0;i<100;i++)b.physiologySecond(true,true,1);
        require(!o.hasInternalBleeding(),"minor leaks close at rest");
        float heart=o.integrity(OrganState.Organ.HEART);for(int i=0;i<100;i++)b.physiologySecond(true,true,1);
        require(o.integrity(OrganState.Organ.HEART)>heart,"slow organ repair");
        b.reset();TraumaEvents.applyDamage(p,b,p.damageSources().drown(),2);
        require(b.health(BodyPart.CHEST)==85,"drowning does not double damage chest");
        for(int i=0;i<10;i++)b.physiologySecond(false,false,1);
        require(o.oxygen()<.3&&o.oxygenDebt()>0&&o.integrity(O)==1,"oxygen depletion precedes brain injury");
        for(int i=0;i<30;i++)b.physiologySecond(false,false,1);
        require(o.integrity(O)<1&&o.oxygenDebt()>=18,"sustained hypoxia damages brain");
        float debt=o.oxygenDebt();for(int i=0;i<50;i++)b.physiologySecond(true,true,1);
        require(o.oxygen()>.99&&o.oxygenDebt()<debt,"breathing reverses deficit");
        b.reset();b.loseBlood(3000);require(o.perfusion(b.bloodFraction())<.5f,"low volume compromises circulation");
        for(int i=0;i<120&&!b.vitalDestroyed();i++)b.physiologySecond(false,false,1);
        require(b.vitalDestroyed(),"prolonged oxygen failure can kill");
        b.reset();require(o.oxygen()==1&&o.integrity(O)==1&&!b.hasAnyBleeding(),"new life reset");
        var targets=OrganState.Organ.values();
        double[] sides={0,0,.50,-.50,-.50,.50,-.50,.20,0};
        double[] heights={.9,.65,.76,.76,.53,.53,.53,.52,.45};
        for(int i=0;i<targets.length;i++){
            var target=targets[i];b.reset();
            require(OrganTarget.select(target.region,sides[i],heights[i],i==5||i==6)==target,"localized "+target);
            b.impact(target.region,25,WoundKind.PUNCTURE,target);
            for(var organ:targets)require(organ==target?o.integrity(organ)<1:o.integrity(organ)==1,"isolated "+target+" vs "+organ);
            require(b.health(target.region)==target.region.maximum-25,"one external HP deduction");
        }
        require(OrganTarget.select(BodyPart.LEFT_ARM,0,.7,false)==null&&OrganTarget.select(BodyPart.CHEST,Double.NaN,.7,false)==null,"no organ for invalid/limb hit");
        b.reset();
        for(int i=0;i<10;i++)b.physiologySecond(false,false,1);
        require(!o.unconscious(),"brief impairment is not instant faint");
        for(int i=0;i<10;i++)b.physiologySecond(false,false,1);
        require(o.unconscious()&&o.systemicShock()>=2&&PhysicalFunction.systemic(b)==0,"sustained oxygen deficit causes loss of consciousness");
        require(!PhysicalFunction.canUse(b,BodyPart.RIGHT_ARM,false)&&PhysicalFunction.jump(b)==0&&!PhysicalFunction.canSprint(b)&&!dev.minemods.traumacraft.integration.WorldLogicBridge.canTreat(p),"unconscious actions blocked");
        var faint=b.serializeNBT(p.registryAccess());loaded.deserializeNBT(p.registryAccess(),faint);
        require(loaded.organs().unconscious()&&loaded.organs().shockLoad()==o.shockLoad(),"persisted shock and unconsciousness");
        var packet=dev.minemods.traumacraft.network.SyncBodyStatePacket.from(b,0);
        var buf=new net.minecraft.network.RegistryFriendlyByteBuf(io.netty.buffer.Unpooled.buffer(),p.registryAccess());
        try{
            dev.minemods.traumacraft.network.SyncBodyStatePacket.STREAM_CODEC.encode(buf,packet);
            var decoded=dev.minemods.traumacraft.network.SyncBodyStatePacket.STREAM_CODEC.decode(buf);
            require(decoded.clinical().unconscious()&&decoded.clinical().oxygen()==o.oxygen()&&decoded.clinical().shockLoad()==o.shockLoad()&&java.util.Arrays.equals(decoded.clinical().integrity(),packet.clinical().integrity()),"actual clinical packet roundtrip");
        }finally{buf.release();}
        b.physiologySecond(true,true,1);require(o.unconscious(),"cannot wake after one second");
        for(int i=0;i<30;i++)b.physiologySecond(true,true,1);
        require(!o.unconscious()&&PhysicalFunction.systemic(b)>0,"stable circulation wakes player");
        b.reset();
        var neutral=dev.minemods.traumacraft.integration.OrganSupport.factors(100,50,1,1);
        var stressed=dev.minemods.traumacraft.integration.OrganSupport.factors(20,15,.5f,.5f);
        require(neutral.circulation()==1&&neutral.repair()==1&&stressed.circulation()<1&&stressed.repair()<.5f,"provider factors bounded");
        require(dev.minemods.traumacraft.integration.OrganSupport.factors(Double.NaN,50,1,1).equals(dev.minemods.traumacraft.integration.OrganSupport.NEUTRAL),"invalid provider neutral fallback");
        b.physiologySecond(true,true,stressed.repair(),stressed.circulation());
        require(o.perfusion(1)<1&&o.oxygen()==1&&b.bloodMl()==5000,"provider affects function but does not invent blood loss");
        var clinical=TraumaApi.physiology(p).orElseThrow();
        require(clinical.circulationSupport()==stressed.circulation()&&clinical.shockLoad()==o.shockLoad()&&!clinical.unconscious(),"public clinical read only");
        b.reset();require(!o.unconscious()&&o.shockLoad()==0&&o.circulationSupport()==1,"reset clears systemic state");
    }
}
