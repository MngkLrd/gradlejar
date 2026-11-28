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
        EnvironmentSampler.invalidateLocal(l);RoomHeat.invalidate(l);
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
                    require(s.skinWetness()<1-.015*dev.minemods.thermal.WeatherReading.dryingFactor(s.weather().humidity()),"radiant source accelerates drying at same humidity");
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
                case "weather" -> {
                    var a=WorldLogicApi.atmosphere(l,p.blockPosition()).orElseThrow();
                    var w=s.weather();
                    require(w.atmosphere()&&Math.abs(w.humidity()-a.relativeHumidityPercent())<1e-8,"authoritative relative humidity");
                    require(Math.abs(w.windSpeed()-a.outdoorWindSpeedMps())<1e-8&&w.precipitation()==a.precipitation().ordinal(),"authoritative wind and precipitation");
                    require(Math.abs(e.wind()-dev.minemods.thermal.WeatherReading.normalizedWind(w.windSpeed(),s.sheltered()))<1e-8,"wind used in exposure");
                }
                case "history" -> {
                    var before=n.getList(HistoryStore.KEY,net.minecraft.nbt.Tag.TAG_COMPOUND).copy();
                    try{
                        n.remove(HistoryStore.KEY);
                        ThermalEvents.update(p);
                        var points=HistoryStore.read(n);
                        require(points.size()==1,"real update records a route sample");
                        var point=points.getFirst();
                        require(point.x()==p.getBlockX()&&point.y()==p.getBlockY()&&point.z()==p.getBlockZ(),"actual coordinates");
                        require(point.dimension().equals(l.dimension().location().toString())&&point.biome().equals(s.weather().biome()),"actual location");
                        require(Math.abs(point.air()-s.exposure().airC())<.01,"actual ambient reading");
                        ThermalEvents.update(p);
                        require(HistoryStore.read(n).size()==1,"no duplicate within sample interval");
                        CompoundTag saved=n.copy();
                        require(HistoryStore.read(saved).equals(points),"NBT round trip");
                    }finally{n.put(HistoryStore.KEY,before);}
                }
                case "comfort" -> {
                    var c=dev.minemods.thermal.ClimateComfort.class;
                    var h=EnvironmentSampler.localComfort(l,p,0,40,false);
                    require(h.heat()==0,"blocked fixture must not heat");
                    var pos=new BlockPos(10,100,8);l.setBlockAndUpdate(pos,net.minecraft.world.level.block.Blocks.CAMPFIRE.defaultBlockState().setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.LIT,true));
                    for(int y=100;y<=102;y++)for(int z=7;z<=9;z++)l.setBlockAndUpdate(new BlockPos(9,y,z),net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());
                    h=EnvironmentSampler.localComfort(l,p,0,40,false);
                    require(h.heat()>=18,"campfire at two blocks supplies usable heat at zero degrees");
                    var model=new dev.minemods.thermal.ThermalModel.Exposure(0,h.heat(),.3,0,0,0,false,0,0);
                    var state=dev.minemods.thermal.ThermalModel.tick(dev.minemods.thermal.ThermalModel.initial(),model,1);
                    require(state.targetC()>=36.6,"campfire prevents dangerous cold");
                    l.setBlockAndUpdate(pos,net.minecraft.world.level.block.Blocks.LANTERN.defaultBlockState());
                    require(EnvironmentSampler.localComfort(l,p,0,40,false).heat()>0,"lantern gives some heat");
                    l.setBlockAndUpdate(pos,net.minecraft.world.level.block.Blocks.FURNACE.defaultBlockState().setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.LIT,true));
                    require(EnvironmentSampler.localComfort(l,p,0,40,false).heat()>10,"lit furnace produces heat");
                    l.setBlockAndUpdate(pos,net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());
                }
                case "fixtures" -> {
                    var pos=new BlockPos(10,100,8);
                    var hearth=dev.minemods.thermal.content.ThermalContent.HEARTH.get();
                    var cooler=dev.minemods.thermal.content.ThermalContent.COOLER.get();
                    l.setBlockAndUpdate(pos,hearth.defaultBlockState());
                    require(EnvironmentSampler.localComfort(l,p,0,40,true).heat()==0,"unfueled hearth cold");
                    require(hearth.activate(l,pos,l.getBlockState(pos)),"hearth activation");
                    require(!hearth.activate(l,pos,l.getBlockState(pos)),"no duplicate activation");
                    require(EnvironmentSampler.localComfort(l,p,0,40,true).heat()>=18,"fueled hearth warms");
                    l.setBlockAndUpdate(pos,cooler.defaultBlockState());
                    require(EnvironmentSampler.localComfort(l,p,40,20,true).cooling()==0,"dry cooler does not cool");
                    require(cooler.activate(l,pos,l.getBlockState(pos)),"cooler activation");
                    require(EnvironmentSampler.localComfort(l,p,40,20,true).cooling()>EnvironmentSampler.localComfort(l,p,40,90,true).cooling(),"humidity limits cooling");
                    require(EnvironmentSampler.localComfort(l,p,0,20,true).cooling()==0,"no cooling in cold");
                    l.setBlockAndUpdate(pos,net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());
                }
                case "clothing" -> {
                    var pos=new BlockPos(10,100,8);
                    l.setBlockAndUpdate(pos,net.minecraft.world.level.block.Blocks.SHORT_GRASS.defaultBlockState());
                    require(dev.minemods.thermal.content.FiberHarvest.harvest(l,pos,p),"bare-hand fiber harvest removes tagged plant");
                    require(l.getBlockState(pos).isAir(),"harvest consumes plant");
                    require(!dev.minemods.thermal.content.FiberHarvest.harvest(l,pos,p),"no duplicate harvest");
                    require(!dev.minemods.thermal.content.FiberHarvest.canHarvest(net.minecraft.world.level.block.Blocks.STONE.defaultBlockState()),"stone is not fiber");
                    p.setItemSlot(EquipmentSlot.CHEST,new ItemStack(dev.minemods.thermal.content.ThermalContent.GRASS_COAT.get()));
                    require(EnvironmentSampler.sample(p,0).orElseThrow().exposure().insulation()>=.24,"grass coat protects");
                    p.setItemSlot(EquipmentSlot.CHEST,new ItemStack(dev.minemods.thermal.content.ThermalContent.LIGHT_TUNIC.get()));
                    require(EnvironmentSampler.sample(p,0).orElseThrow().exposure().ventilation()>.4,"light tunic ventilates");
                }
                case "fiber_work" -> {
                    dev.minemods.thermal.content.FiberWorkChecks.verify(l,p);
                }
                case "creative_tab" -> {
                    var tab=dev.minemods.thermal.content.ThermalContent.TAB.get();
                    var key=net.minecraft.core.registries.BuiltInRegistries.CREATIVE_MODE_TAB.getKey(tab);
                    require(key!=null&&key.toString().equals("worldlogic_thermal:temperature"),"dedicated creative tab registered");
                    var items=dev.minemods.thermal.content.ThermalContent.creativeItems();
                    var registry=net.minecraft.core.registries.BuiltInRegistries.ITEM;
                    var expected=registry.keySet().stream().filter(k->k.getNamespace().equals("worldlogic_thermal")).collect(java.util.stream.Collectors.toSet());
                    var listed=items.stream().map(registry::getKey).collect(java.util.stream.Collectors.toSet());
                    require(items.size()==listed.size()&&listed.equals(expected)&&items.size()==51,"all fifty-one registered items listed exactly once");
                }
                case "portable" -> ExpansionChecks.portable(l,p);
                case "advanced_clothes" -> ExpansionChecks.clothes(l,p);
                case "devices" -> ExpansionChecks.devices(l,p);
                case "textile" -> ExpansionChecks.textile(l,p);
                case "adaptation" -> ClimateEvolutionChecks.adaptation(l,p);
                case "frostbite" -> ClimateEvolutionChecks.frostbite(l,p);
                case "nutrition" -> ClimateEvolutionChecks.nutrition(l,p);
                case "room_inertia" -> ClimateEvolutionChecks.room(l,p);
                case "room_ventilation","room_humidity","room_persistence","room_cache","wall_insulation" -> RoomChecks.verify(l,p,name);
                case "village_climate" -> ClimateEvolutionChecks.settlement(l,p);
                case "integration" -> IntegrationChecks.verify(l,p);
                case "water_exposure","room_response","hud_signals" -> StabilityChecks.verify(l,p,name);
                case "physical_materials" -> PhysicalChecks.verify(l,p);
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
