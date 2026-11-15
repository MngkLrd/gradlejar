package dev.minemods.blowpipes;
import java.util.*;
import net.minecraft.core.registries.*;
import net.minecraft.resources.*;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.*;
import net.minecraft.world.item.*;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.registries.*;
import net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
@Mod(Blowpipes.ID)
public final class Blowpipes {
 public static final String ID="blowpipes";
 public static final String[] TYPES={"plain_dart","iron_dart","poison_dart","slowness_dart","fire_dart","sleep_dart"};
 private static final DeferredRegister.Items ITEMS=DeferredRegister.createItems(ID);
 private static final DeferredRegister<EntityType<?>> ENTITIES=DeferredRegister.create(BuiltInRegistries.ENTITY_TYPE,ID);
 private static final DeferredRegister<CreativeModeTab> TABS=DeferredRegister.create(Registries.CREATIVE_MODE_TAB,ID);
 private static final Map<String,DeferredItem<Item>> ALL=new LinkedHashMap<>();
 private static final Map<String,DeferredItem<Item>> FLIGHTS=new LinkedHashMap<>();
 private static DeferredItem<Item> register(String name,int tier) {
  DeferredItem<Item> item=ITEMS.registerItem(name,p->tier<0?new BlowpipeInfoItem(p,name):new BlowpipeItem(p.stacksTo(1),tier,name));
  ALL.put(name,item);return item;
 }
 private static DeferredItem<Item> flight(String name){
  DeferredItem<Item> item=ITEMS.registerItem("flight_"+name,Item::new);
  FLIGHTS.put(name,item);return item;
 }
 static {
  register("bamboo_blowpipe",0);register("hunting_blowpipe",1);register("reinforced_blowpipe",2);
  for(String name:TYPES){register(name,-1);flight(name);}
 }
 private static final DeferredHolder<EntityType<?>,EntityType<DartProjectile>> DART=ENTITIES.register("dart",()->
   EntityType.Builder.<DartProjectile>of(DartProjectile::new,MobCategory.MISC).sized(.16f,.16f).clientTrackingRange(6).updateInterval(2)
      .build(ResourceKey.create(Registries.ENTITY_TYPE,Identifier.fromNamespaceAndPath(ID,"dart"))));
 private static final DeferredHolder<CreativeModeTab,CreativeModeTab> TAB=TABS.register("blowpipes",()->
    CreativeModeTab.builder().title(Component.translatable("itemGroup.blowpipes.main"))
     .icon(()->new ItemStack(ALL.get("hunting_blowpipe").get()))
     .displayItems((params,out)->{for(var e:ALL.values())out.accept(e.get());}).build());
 public static EntityType<DartProjectile> dartType(){return DART.get();}
 public static Item DEFAULT_DART(){return ALL.get("plain_dart").get();}
 public static Item flightItem(String name){
  var value=FLIGHTS.get(name);return value==null?FLIGHTS.get("plain_dart").get():value.get();
 }
 public Blowpipes(IEventBus bus){
  ITEMS.register(bus);ENTITIES.register(bus);TABS.register(bus);
  NeoForge.EVENT_BUS.addListener(Blowpipes::tick);
  NeoForge.EVENT_BUS.addListener(Blowpipes::incoming);
  NeoForge.EVENT_BUS.addListener(Blowpipes::leaving);
 }
 private static void tick(EntityTickEvent.Post event) {
  if(event.getEntity() instanceof Mob mob && !mob.level().isClientSide())SleepSystem.check(mob);
 }
 private static void incoming(LivingIncomingDamageEvent event) {
  if(event.getEntity() instanceof Mob mob && !mob.level().isClientSide())SleepSystem.wake(mob);
 }
 private static void leaving(EntityLeaveLevelEvent event) {
  if(event.getEntity() instanceof Mob mob && !event.getLevel().isClientSide())SleepSystem.wake(mob);
 }
}
