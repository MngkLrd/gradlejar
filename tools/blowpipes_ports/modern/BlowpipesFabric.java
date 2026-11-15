package dev.minemods.blowpipes;
import java.util.*;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.itemgroup.v1.CreativeModeTabEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.*;
import net.minecraft.resources.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.item.*;
public final class Blowpipes implements ModInitializer {
 public static final String ID="blowpipes";
 public static final String[] TYPES={"plain_dart","iron_dart","poison_dart","slowness_dart","fire_dart","sleep_dart"};
 private static final Map<String,Item> ALL=new LinkedHashMap<>(),FLIGHTS=new LinkedHashMap<>();
 private static EntityType<DartProjectile> dart;
 private static Item register(String name,int tier){
  ResourceKey<Item> key=ResourceKey.create(Registries.ITEM,Identifier.fromNamespaceAndPath(ID,name));
  Item.Properties props=new Item.Properties().setId(key);
  Item value=tier<0?new BlowpipeInfoItem(props,name):new BlowpipeItem(props.stacksTo(1),tier,name);
  Registry.register(BuiltInRegistries.ITEM,key,value);ALL.put(name,value);return value;
 }
 private static Item flight(String name){
  String id="flight_"+name;
  ResourceKey<Item> key=ResourceKey.create(Registries.ITEM,Identifier.fromNamespaceAndPath(ID,id));
  Item value=new Item(new Item.Properties().setId(key));
  Registry.register(BuiltInRegistries.ITEM,key,value);FLIGHTS.put(name,value);return value;
 }
 public static EntityType<DartProjectile> dartType(){return dart;}
 public static Item DEFAULT_DART(){return ALL.get("plain_dart");}
 public static Item flightItem(String name){return FLIGHTS.getOrDefault(name,FLIGHTS.get("plain_dart"));}
 @Override public void onInitialize(){
  register("bamboo_blowpipe",0);register("hunting_blowpipe",1);register("reinforced_blowpipe",2);
  for(String name:TYPES){register(name,-1);flight(name);}
  ResourceKey<EntityType<?>> key=ResourceKey.create(Registries.ENTITY_TYPE,Identifier.fromNamespaceAndPath(ID,"dart"));
  dart=Registry.register(BuiltInRegistries.ENTITY_TYPE,key,EntityType.Builder.<DartProjectile>of(DartProjectile::new,MobCategory.MISC)
      .sized(.16f,.16f).clientTrackingRange(6).updateInterval(2).build(key));
  CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.COMBAT).register(out->{for(Item item:ALL.values())out.accept(item);});
  ServerTickEvents.END_SERVER_TICK.register(SleepSystem::tick);
  ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity,source,damage)->{
   if(entity instanceof Mob mob)SleepSystem.wake(mob);return true;
  });
  ServerEntityEvents.ENTITY_UNLOAD.register((entity,level)->{if(entity instanceof Mob mob)SleepSystem.wake(mob);});
 }
}
