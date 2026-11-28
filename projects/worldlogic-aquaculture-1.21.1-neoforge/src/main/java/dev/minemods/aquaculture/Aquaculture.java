package dev.minemods.aquaculture;
import com.mojang.brigadier.Command;
import dev.minemods.worldlogic.api.WorldLogicApi;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.animal.Cod;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.registries.DeferredRegister;
import java.util.*;
import java.util.function.Supplier;

@Mod(Aquaculture.ID)
public final class Aquaculture {
 public static final String ID="aquaculture";
 public static final DeferredRegister<EntityType<?>> ENTITIES=DeferredRegister.create(Registries.ENTITY_TYPE,ID);
 public static final DeferredRegister<Item> ITEMS=DeferredRegister.create(Registries.ITEM,ID);
 public static final Map<String,Supplier<EntityType<Cod>>> TYPES=new LinkedHashMap<>();
 public static final Map<String,Supplier<Item>> RAW=new LinkedHashMap<>();
 static {
  for(Species s:Species.ALL){
   TYPES.put(s.id(),ENTITIES.register(s.id(),()->EntityType.Builder.<Cod>of(Fish::new,MobCategory.WATER_AMBIENT)
    .sized((float)Math.max(.22,s.width()*1.7),(float)Math.max(.18,s.height())).clientTrackingRange(6).build(ID+":"+s.id())));
   RAW.put(s.id(),ITEMS.register("raw_"+s.id(),()->new RawFish(new Item.Properties().stacksTo(1))));
  }
 }
 public Aquaculture(IEventBus bus){
  ENTITIES.register(bus);ITEMS.register(bus);bus.addListener(this::attributes);
  NeoForge.EVENT_BUS.addListener(this::tick);NeoForge.EVENT_BUS.addListener(this::drops);NeoForge.EVENT_BUS.addListener(this::commands);
 }
 private void attributes(EntityAttributeCreationEvent e){
  for(Species s:Species.ALL)e.put(TYPES.get(s.id()).get(),Cod.createAttributes().build());
 }
 public static Species species(Entity entity){
  for(var entry:TYPES.entrySet())if(entity.getType()==entry.getValue().get())return Species.BY_ID.get(entry.getKey());
  return null;
 }
 public static CompoundTag traits(Fish fish,Species s){
  CompoundTag tag=fish.getPersistentData();
  if(!tag.contains("wl_fish_mass_kg")){
   var random=fish.getRandom();
   double f=Math.pow(random.nextDouble(),2.1);
   double mass=s.minKg()+(s.maxKg()-s.minKg())*f;
   double ratio=Math.cbrt(mass/s.maxKg());
   tag.putDouble("wl_fish_mass_kg",mass);
   tag.putDouble("wl_fish_length_m",Math.max(.05,s.adultLength()*ratio));
   tag.putInt("wl_fish_age_years",Math.max(1,(int)Math.round(1+8*f)));
   tag.putBoolean("wl_fish_female",random.nextBoolean());
   tag.putString("wl_fish_species",s.id());
   tag.putDouble("wl_fish_health",1);
  }
  return tag;
 }
 public static boolean water(ServerLevel level,BlockPos pos){
  return !level.isOutsideBuildHeight(pos)&&level.hasChunkAt(pos)&&level.getFluidState(pos).is(FluidTags.WATER);
 }
 public static int depth(ServerLevel level,BlockPos pos){
  if(!water(level,pos))return 0;
  int count=1;
  for(int i=1;i<=16&&water(level,pos.above(i));i++)count++;
  for(int i=1;i<=16&&water(level,pos.below(i));i++)count++;
  return count;
 }
 public record Habitat(WorldLogicApi.WaterKind kind,double temp,double salt,double risk,int depth){}
 public static Habitat habitat(ServerLevel level,BlockPos pos){
  if(!water(level,pos))return null;
  var profile=WorldLogicApi.waterProfile(level,pos);
  var env=WorldLogicApi.environment(level,pos);
  if(env.isEmpty())return null;
  // Air-derived estimate, not an authoritative water temperature.
  double temp=Math.max(-2,Math.min(36,env.get().ambientCelsiusEstimate()*.75+4));
  return new Habitat(profile.kind(),temp,profile.salinityGkg(),profile.contaminationRisk(),depth(level,pos));
 }
 public static Species choose(Habitat h,net.minecraft.util.RandomSource random){
  if(h==null)return null;
  int total=0;
  for(Species s:Species.ALL)total+=s.habitatWeight(h.kind,h.temp,h.salt,h.risk,h.depth);
  if(total==0)return null;
  int draw=random.nextInt(total);
  for(Species s:Species.ALL){
   draw-=s.habitatWeight(h.kind,h.temp,h.salt,h.risk,h.depth);
   if(draw<0)return s;
  }
  return null;
 }
 public static BlockPos findWaterColumn(ServerLevel level,int x,int z,int nearY){
  BlockPos probe=new BlockPos(x,nearY,z);
  if(!level.hasChunkAt(probe))return null;
  // First inspect the player's vertical band, then the actual water surface below vegetation/ice.
  int top=level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,x,z)-1;
  for(int start:new int[]{nearY,top}){
   for(int offset=0;offset<=10;offset++){
    BlockPos pos=new BlockPos(x,start-offset,z);
    if(Math.abs(pos.getY()-nearY)>48||level.isOutsideBuildHeight(pos))continue;
    if(water(level,pos)&&water(level,pos.below()))return pos;
   }
  }
  return null;
 }
 public static Fish spawn(ServerLevel level,BlockPos pos,Species s,Habitat h){
  if(s==null||h==null||!water(level,pos)||!s.suitable(h.kind,h.temp,h.salt,h.risk,h.depth))return null;
  var entity=TYPES.get(s.id()).get().create(level);
  if(!(entity instanceof Fish fish))return null;
  fish.moveTo(pos.getX()+.5,pos.getY()+.1,pos.getZ()+.5,level.random.nextFloat()*360,0);
  if(!level.noCollision(fish)||!FishWater.contact(fish))return null;
  traits(fish,s);
  return level.addFreshEntity(fish)?fish:null;
 }
 private void tick(ServerTickEvent.Post event){
  var server=event.getServer();
  if(server.getTickCount()%80!=0)return;
  for(ServerPlayer player:server.getPlayerList().getPlayers()){
   if(player.isSpectator())continue;
   ServerLevel level=player.serverLevel();
   if(!level.dimension().equals(Level.OVERWORLD))continue;
   BlockPos origin=player.blockPosition();
   var nearby=level.getEntitiesOfClass(Fish.class,new AABB(origin).inflate(48));
   if(nearby.size()>=30)continue;
   for(int a=0;a<8;a++){
    int dx=level.random.nextInt(65)-32,dz=level.random.nextInt(65)-32;
    if(dx*dx+dz*dz<64)continue;
    int x=origin.getX()+dx,z=origin.getZ()+dz;
    BlockPos surface=findWaterColumn(level,x,z,origin.getY());
    if(surface==null)continue;
    // Search distinct depths; one unsuitable or obstructed cell must not discard the entire column.
    for(int b=0;b<6;b++){
     BlockPos pos=surface.below(b);
     if(!water(level,pos)||Math.abs(pos.getY()-origin.getY())>48)break;
     Habitat h=habitat(level,pos);
     if(h==null)continue;
     Species s=choose(h,level.random);
     if(s==null)continue;
     long count=nearby.stream().filter(f->species(f)==s).count();
     if(count>=Math.min(12,s.maxGroup()))continue;
     if(spawn(level,pos,s,h)!=null){a=8;break;}
    }
   }
  }
 }
 public static ItemStack catchStack(Fish fish,Species s){
  var stack=new ItemStack(RAW.get(s.id()).get());
  stack.set(DataComponents.CUSTOM_DATA,CustomData.of(traits(fish,s).copy()));
  return stack;
 }
 private void drops(LivingDropsEvent e){
  if(!(e.getEntity() instanceof Fish fish))return;
  Species s=species(fish);if(s==null)return;
  e.getDrops().removeIf(drop->drop.getItem().is(Items.COD)||drop.getItem().is(Items.COOKED_COD));
  e.getDrops().add(new ItemEntity(fish.level(),fish.getX(),fish.getY(),fish.getZ(),catchStack(fish,s)));
 }
 private void commands(RegisterCommandsEvent e){
  e.getDispatcher().register(Commands.literal("aquaculture").requires(src->src.hasPermission(2))
   .then(Commands.literal("water_selftest").executes(c->{
    WaterChecks.run(c.getSource().getLevel());
    c.getSource().sendSuccess(()->Component.literal("AQUA_NATIVE_FISH_WATER_PASS"),false);
    return Command.SINGLE_SUCCESS;
   }))
   .then(Commands.literal("feeding_selftest").executes(c->{
    FeedingChecks.run(c.getSource().getLevel());
    c.getSource().sendSuccess(()->Component.literal("AQUACULTURE_FEEDING_36_PASS"),false);
    return Command.SINGLE_SUCCESS;
   }))
   .then(Commands.literal("selftest").executes(c->{
    int count=0;var level=c.getSource().getLevel();
    for(Species s:Species.ALL){
     var kind=s.region().startsWith("marine")?WorldLogicApi.WaterKind.SEA:s.region().equals("wetland")?WorldLogicApi.WaterKind.WETLAND:WorldLogicApi.WaterKind.LAKE;
     double temp=(s.minTemp()+s.maxTemp())/2,salt=(s.minSalt()+s.maxSalt())/2;
     if(!s.suitable(kind,temp,salt,.1,s.minDepth())||s.suitable(WorldLogicApi.WaterKind.NONE,temp,salt,.1,s.minDepth())||s.suitable(kind,temp,salt,.99,s.minDepth())||s.suitable(kind,temp,salt,.1,0))throw new IllegalStateException("habitat "+s.id());
     var entity=TYPES.get(s.id()).get().create(level);
     if(!(entity instanceof Fish f))throw new IllegalStateException("entity "+s.id());
     var stack=catchStack(f,s);
     if(stack.isEmpty()||!stack.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag().contains("wl_fish_mass_kg")||species(f)!=s)throw new IllegalStateException("traits "+s.id());
     f.discard();count++;
    }
    if(count!=36)throw new IllegalStateException("fish count "+count);
    c.getSource().sendSuccess(()->Component.literal("AQUACULTURE_36_SPECIES_TRAITS_HABITAT_PASS"),false);
    return Command.SINGLE_SUCCESS;
   })));
 }
}

