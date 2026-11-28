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
        b.reset();for(int i=0;i<3;i++)b.impact(BodyPart.CHEST,80,WoundKind.PUNCTURE);
        float severe=o.internalLossPerSecond();require(b.stabilizeInternal(BodyPart.CHEST)&&o.internalLossPerSecond()<severe&&o.internalLossPerSecond()>0,"advanced support reduces severe leak without magic closure");
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
    }
}
