package dev.minemods.thermal.server;
import dev.minemods.worldlogic.api.WorldLogicApi;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
final class ThermalChecks {
    private ThermalChecks() {}
    private static void require(boolean value,String detail){if(!value)throw new IllegalStateException("Thermal QA: "+detail);}
    static void verify(ServerLevel l,String name) {
        var p=FakePlayerFactory.getMinecraft(l);
        p.moveTo(8.5,100,8.5,0,0);
        var n=p.getPersistentData(); n.remove(ThermalEvents.KEY);
        var old=p.getItemBySlot(EquipmentSlot.CHEST).copy();
        p.setItemSlot(EquipmentSlot.CHEST,ItemStack.EMPTY);
        try {
            var s=EnvironmentSampler.sample(p,1).orElseThrow();
            var e=s.exposure();
            switch(name) {
                case "climate" -> {
                    var env=WorldLogicApi.environment(l,p.blockPosition()).orElseThrow();
                    require(Math.abs(e.airC()-env.ambientCelsiusEstimate())<1e-8,"air must be authoritative API value");
                    require(s.sheltered()==env.covered(),"covered must match API");
                    require(WorldLogicApi.environment(l,new BlockPos(1000000,100,1000000)).isEmpty(),"unloaded position rejected");
                }
                case "dry" -> {
                    require(e.radiantC()==0,"no local heat without source");
                    require(s.skinWetness()<1&&s.skinWetness()>0,"skin dries gradually");
                }
                case "heat" -> {
                    require(e.radiantC()>1,"campfire gives local heat");
                    require(s.skinWetness()<.98,"radiant source accelerates drying");
                }
                case "blocked" -> require(e.radiantC()==0,"wall blocks campfire radiation");
                case "armor" -> {
                    require(e.insulation()==0,"empty armor has zero insulation");
                    p.setItemSlot(EquipmentSlot.CHEST,new ItemStack(Items.LEATHER_CHESTPLATE));
                    double leather=EnvironmentSampler.sample(p,0).orElseThrow().exposure().insulation();
                    p.setItemSlot(EquipmentSlot.CHEST,new ItemStack(Items.IRON_CHESTPLATE));
                    double iron=EnvironmentSampler.sample(p,0).orElseThrow().exposure().insulation();
                    require(leather>iron&&iron>0,"registered gear tags differentiate insulation");
                }
                case "effects" -> {
                    require(WorldLogicApi.fatigue(p).enabled(),"core fatigue active");
                    WorldLogicApi.recoverExertion(p,100);
                    CompoundTag tag=new CompoundTag();tag.putDouble("body",34);n.put(ThermalEvents.KEY,tag);
                    ThermalEvents.update(p);
                    var after=n.getCompound(ThermalEvents.KEY);
                    require(after.getString("stage").equals("HYPOTHERMIA"),"cold stage persisted by real update");
                    require(after.getDouble("body")!=34,"body evolves");
                    require(WorldLogicApi.fatigue(p).fatiguePercent()>0,"thermal update adds core fatigue");
                    require(p.getAttribute(Attributes.MOVEMENT_SPEED).getModifier(ThermalEvents.SPEED)!=null,"cold speed penalty");
                    tag=new CompoundTag();tag.putDouble("body",40);n.put(ThermalEvents.KEY,tag);
                    ThermalEvents.update(p);
                    require(n.getCompound(ThermalEvents.KEY).getString("stage").equals("HYPERTHERMIA"),"hot stage persisted");
                    tag=new CompoundTag();tag.putDouble("body",37);n.put(ThermalEvents.KEY,tag);
                    ThermalEvents.update(p);
                    require(n.getCompound(ThermalEvents.KEY).getString("stage").equals("COMFORTABLE"),"neutral stage");
                    require(p.getAttribute(Attributes.MOVEMENT_SPEED).getModifier(ThermalEvents.SPEED)==null,"neutral clears speed penalty");
                }
                case "conditions" -> {
                    var q=dev.minemods.thermal.ThermalCondition.Progress.initial();
                    for(int i=0;i<29;i++)q=dev.minemods.thermal.ThermalCondition.advance(q,dev.minemods.thermal.ThermalModel.Stage.HYPERTHERMIA,1,false);
                    require(q.kind()==dev.minemods.thermal.ThermalCondition.Kind.NONE,"no immediate illness");
                    q=dev.minemods.thermal.ThermalCondition.advance(q,dev.minemods.thermal.ThermalModel.Stage.HYPERTHERMIA,1,false);
                    require(q.kind()==dev.minemods.thermal.ThermalCondition.Kind.SUNSTROKE&&dev.minemods.thermal.ThermalCondition.pulse(q),"sustained solar exposure causes sunstroke");
                    for(var kind:dev.minemods.thermal.ThermalCondition.Kind.values()){
                        if(kind==dev.minemods.thermal.ThermalCondition.Kind.NONE)continue;
                        var source=ThermalInjury.source(p,kind);
                        require(source.is(net.minecraft.tags.DamageTypeTags.BYPASSES_ARMOR),"physiological damage bypasses already-accounted armor");
                        require(!source.is(net.minecraft.tags.DamageTypeTags.IS_FIRE)&&source.getEntity()==null,"nonphysical damage routed through existing Trauma handler");
                    }
                    var tag=new CompoundTag();tag.putDouble("body",34);tag.putInt("coldSeconds",29);n.put(ThermalEvents.KEY,tag);
                    ThermalEvents.update(p);
                    var status=dev.minemods.thermal.api.ThermalApi.status(p).orElseThrow();
                    require(status.condition()==dev.minemods.thermal.ThermalCondition.Kind.HYPOTHERMIA,"condition persisted by live update and API");
                    require(p.hasEffect(net.minecraft.world.effect.MobEffects.WEAKNESS),"physiological debuff applied");
                    tag=new CompoundTag();tag.putDouble("body",37);n.put(ThermalEvents.KEY,tag);
                    ThermalEvents.update(p);
                    require(dev.minemods.thermal.api.ThermalApi.status(p).orElseThrow().condition()==dev.minemods.thermal.ThermalCondition.Kind.NONE,"comfort clears illness");
                    p.removeEffect(net.minecraft.world.effect.MobEffects.WEAKNESS);
                }
                default -> throw new IllegalArgumentException(name);
            }
        } finally {
            ThermalEvents.penalty(p,0);
            WorldLogicApi.recoverExertion(p,100);
            n.remove(ThermalEvents.KEY);
            p.setItemSlot(EquipmentSlot.CHEST,old);
        }
    }
}
