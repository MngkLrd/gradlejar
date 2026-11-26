package dev.minemods.worldlogic;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.animal.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.CampfireBlockEntity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.event.entity.living.*;
import net.minecraftforge.event.entity.player.PlayerEvent;
public final class Ecology {
 private static final Map<Mob,Long> PANIC=new WeakHashMap<>();
 static boolean panicked(Mob m,long now){return PANIC.getOrDefault(m,0L)>now;}
 static void panic(Mob m,long until){PANIC.merge(m,until,Math::max);}

 public static void crop(BlockEvent.CropGrowEvent.Pre e){if(!(e.getLevel() instanceof ServerLevel l)||!(e.getState().getBlock() instanceof CropBlock))return;BlockPos p=e.getPos();
  if(Rules.on("crop_crowding")){int count=0;for(BlockPos q:BlockPos.betweenClosed(p.offset(-2,0,-2),p.offset(2,0,2)))if(l.hasChunkAt(q)&&l.getBlockState(q).is(e.getState().getBlock()))count++;
   if(count>=13&&l.random.nextFloat()<Rules.chance("ecology",.15)){e.setResult(Event.Result.DENY);return;}}
  if(Rules.on("flood_stress")){int wet=0;for(net.minecraft.core.Direction d:net.minecraft.core.Direction.Plane.HORIZONTAL)if(l.getFluidState(p.relative(d)).is(net.minecraft.tags.FluidTags.WATER))wet++;var data=WorldState.get(l);long k=p.asLong(),now=l.getGameTime();if(wet>=2||l.getFluidState(p.above()).is(net.minecraft.tags.FluidTags.WATER)){if(data.flood.size()<32768||data.flood.containsKey(k)){data.flood.put(k,now+1200);data.setDirty();}}long until=data.flood.getOrDefault(k,0L);if(until>now&&l.random.nextFloat()<Rules.chance("ecology",.5)*Math.min(1,(until-now)/1200.0))e.setResult(Event.Result.DENY);else if(until>0&&until<=now){data.flood.remove(k);data.setDirty();}}
 }
 public static void hurt(LivingHurtEvent e){if(!Rules.on("herd_panic")||!(e.getEntity() instanceof Animal animal)||!(animal.level() instanceof ServerLevel l))return;
  var source=e.getSource().getEntity();Vec3 away=source==null?animal.position().add(1,0,0):source.position();
  panic(animal,l.getGameTime()+100);Set<Animal> seen=new HashSet<>();seen.add(animal);List<Animal> wave=List.of(animal);
  for(int depth=0;depth<3&&!wave.isEmpty()&&seen.size()<30;depth++){List<Animal> next=new ArrayList<>();for(Animal origin:wave){
   for(Animal a:l.getEntitiesOfClass(Animal.class,origin.getBoundingBox().inflate(6),a->a.getType()==animal.getType()&&a.isAlive())){
    if(seen.size()>=30)break;if(!seen.add(a)||PANIC.getOrDefault(a,0L)>l.getGameTime())continue;
    PANIC.put(a,l.getGameTime()+100-depth*20);Vec3 d=a.position().subtract(away).normalize().scale(5-depth);a.getNavigation().moveTo(a.getX()+d.x,a.getY(),a.getZ()+d.z,1.3-depth*.1);next.add(a);
   }
  }wave=next;}
 }
 public static void smelt(PlayerEvent.ItemSmeltedEvent e){/* Freshness is assigned at the cooking source, never reset by extraction. */}
 public static void eat(LivingEntityUseItemEvent.Finish e){if(Rules.on("hot_meal")&&e.getEntity() instanceof Player p&&p.level() instanceof ServerLevel l&&e.getItem().has(DataComponents.FOOD)&&e.getItem().getOrDefault(DataComponents.CUSTOM_DATA,net.minecraft.world.item.component.CustomData.EMPTY).copyTag().getLong("worldlogic_hot_until")>l.getServer().overworld().getGameTime()){p.getFoodData().setSaturation(Math.min(p.getFoodData().getFoodLevel(),p.getFoodData().getSaturationLevel()+.5f));}}
 public static void tick(TickEvent.PlayerTickEvent.Post e){if(!(e.player.level() instanceof ServerLevel l)||l.getGameTime()%40!=0||!Rules.on("scent"))return;Player p=e.player;int budget=12;
  for(ItemEntity item:l.getEntitiesOfClass(ItemEntity.class,p.getBoundingBox().inflate(12))){if(--budget<0)break;String scent=Material.item(item.getItem(),"raw_meat")?"meat":Material.item(item.getItem(),"fish")?"fish":"";if(scent.isEmpty()&&item.getItem().is(net.minecraft.world.item.Items.ROTTEN_FLESH)){int n=0;for(var z:l.getEntitiesOfClass(net.minecraft.world.entity.monster.Zombie.class,item.getBoundingBox().inflate(8),z->z.getTarget()==null)){if(n++>=4)break;z.getNavigation().moveTo(item,1.0);}continue;}if(scent.isEmpty())continue;
   int n=0;for(Mob m:l.getEntitiesOfClass(Mob.class,item.getBoundingBox().inflate(10),m->m.getTarget()==null&&(m instanceof Wolf&&scent.equals("meat")||m instanceof Fox||m instanceof Cat&&scent.equals("fish")))){if(++n>6)break;if(m instanceof TamableAnimal t&&t.isTame())continue;if(PANIC.getOrDefault(m,0L)>l.getGameTime())continue;PredatorEcology.remember(m,item.position());m.getNavigation().moveTo(item,1.05);}
  }
  for(int i=0;i<48;i++){BlockPos q=p.blockPosition().offset(l.random.nextInt(15)-7,l.random.nextInt(7)-3,l.random.nextInt(15)-7);if(!l.hasChunkAt(q))continue;
   var s=l.getBlockState(q);if(!s.hasProperty(net.minecraft.world.level.block.state.properties.BlockStateProperties.LIT)||!s.getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.LIT))continue;
   boolean meat=false;var be=l.getBlockEntity(q);
   if(be instanceof CampfireBlockEntity c){for(ItemStack stack:c.getItems())if(Material.item(stack,"raw_meat")||Material.item(stack,"fish")){meat=true;break;}}
   else if(be instanceof net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity furnace)meat=Material.item(furnace.getItem(0),"raw_meat")||Material.item(furnace.getItem(0),"fish");
   if(!meat)continue;int n=0;for(Mob m:l.getEntitiesOfClass(Mob.class,new net.minecraft.world.phys.AABB(q).inflate(14),m->(m instanceof Wolf||m instanceof Fox)&&m.getTarget()==null)){if(++n>6)break;if(m instanceof TamableAnimal t&&t.isTame())continue;PredatorEcology.remember(m,Vec3.atCenterOf(q));m.getNavigation().moveTo(q.getX()+.5,q.getY(),q.getZ()+.5,1.0);}
  }
 }
}
