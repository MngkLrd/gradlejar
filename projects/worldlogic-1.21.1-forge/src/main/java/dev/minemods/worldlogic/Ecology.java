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
 private static final Map<Player,Long> HOT=new WeakHashMap<>();
 public static void crop(BlockEvent.CropGrowEvent.Pre e){if(!(e.getLevel() instanceof ServerLevel l)||!(e.getState().getBlock() instanceof CropBlock))return;BlockPos p=e.getPos();
  if(Rules.on("crop_crowding")){int count=0;for(BlockPos q:BlockPos.betweenClosed(p.offset(-2,0,-2),p.offset(2,0,2)))if(l.hasChunkAt(q)&&l.getBlockState(q).is(e.getState().getBlock()))count++;
   if(count>=13&&l.random.nextFloat()<.15){e.setResult(Event.Result.DENY);return;}}
  if(Rules.on("flood_stress")&&l.getFluidState(p.above()).is(net.minecraft.tags.FluidTags.WATER)&&l.random.nextFloat()<.5)e.setResult(Event.Result.DENY);
 }
 public static void hurt(LivingHurtEvent e){if(!Rules.on("herd_panic")||!(e.getEntity() instanceof Animal animal)||!(animal.level() instanceof ServerLevel l))return;
  var source=e.getSource().getEntity();Vec3 away=source==null?animal.position().add(1,0,0):source.position();int count=0;
  for(Animal a:l.getEntitiesOfClass(Animal.class,animal.getBoundingBox().inflate(8),a->a!=animal&&a.getType()==animal.getType())){if(++count>12)break;if(PANIC.getOrDefault(a,0L)>l.getGameTime())continue;PANIC.put(a,l.getGameTime()+100);Vec3 d=a.position().subtract(away).normalize().scale(5);a.getNavigation().moveTo(a.getX()+d.x,a.getY(),a.getZ()+d.z,1.3);}
 }
 public static void smelt(PlayerEvent.ItemSmeltedEvent e){if(e.getEntity().level() instanceof ServerLevel l&&e.getSmelting().has(DataComponents.FOOD))HOT.put(e.getEntity(),l.getGameTime()+1200);}
 public static void eat(LivingEntityUseItemEvent.Finish e){if(Rules.on("hot_meal")&&e.getEntity() instanceof Player p&&p.level() instanceof ServerLevel l&&e.getItem().has(DataComponents.FOOD)&&HOT.getOrDefault(p,0L)>l.getGameTime()){p.getFoodData().setSaturation(Math.min(p.getFoodData().getFoodLevel(),p.getFoodData().getSaturationLevel()+.5f));HOT.remove(p);}}
 public static void tick(TickEvent.PlayerTickEvent.Post e){if(!(e.player.level() instanceof ServerLevel l)||l.getGameTime()%40!=0||!Rules.on("scent"))return;Player p=e.player;int budget=12;
  for(ItemEntity item:l.getEntitiesOfClass(ItemEntity.class,p.getBoundingBox().inflate(12))){if(--budget<0)break;String scent=Material.item(item.getItem(),"raw_meat")?"meat":Material.item(item.getItem(),"fish")?"fish":"";if(scent.isEmpty())continue;
   int n=0;for(Mob m:l.getEntitiesOfClass(Mob.class,item.getBoundingBox().inflate(10),m->m.getTarget()==null&&(m instanceof Wolf&&scent.equals("meat")||m instanceof Fox||m instanceof Cat&&scent.equals("fish")))){if(++n>6)break;if(m instanceof TamableAnimal t&&t.isTame())continue;if(PANIC.getOrDefault(m,0L)>l.getGameTime())continue;m.getNavigation().moveTo(item,1.05);}
  }
 }
}
