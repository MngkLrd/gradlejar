package dev.minemods.worldlogic;
import java.util.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.tick.*;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
public final class Condition {
 private static final Map<Player,Double> BURDEN=new WeakHashMap<>();
 static double stackLoad(ItemStack s){if(!Material.item(s,"dense_cargo"))return 0;double weight=Math.max(1.5,Material.weight(s));return weight*s.getCount()/Math.max(1,s.getMaxStackSize());}
 static double burden(Player p){return Rules.on("cargo_burden")?BURDEN.getOrDefault(p,0.0):0;}
 static double weakness(Player p){return Rules.on("weak_condition")?weakness(p.getHealth()/Math.max(1,p.getMaxHealth()),p.getFoodData().getFoodLevel()):0;}
 static double weakness(double health,int food){return Math.min(.5,Math.max(0,.5-health)*.6+Math.max(0,10-food)*.025);}
 static double penalty(Player p){return Math.max(Math.min(.18,burden(p)*.035),weakness(p)*.30);}
 static double workFactor(Player p){return 1+Math.min(.5,burden(p)*.09)+weakness(p)*.4;}
 public static void tick(PlayerTickEvent.Post e){
  Player p=e.getEntity();if(!Survival.active(p)||p.tickCount%20!=0||!Rules.on("cargo_burden"))return;
  double load=0;for(int i=0;i<p.getInventory().getContainerSize();i++)load+=stackLoad(p.getInventory().getItem(i));
  BURDEN.put(p,Math.max(0,load-4));
 }
 public static void hurt(LivingIncomingDamageEvent e){if(!e.isCanceled()&&e.getSource().getDirectEntity() instanceof Player p&&Survival.active(p)&&Rules.on("weak_condition"))e.setAmount(e.getAmount()*(float)(1-weakness(p)*.4));}
 public static void logout(PlayerEvent.PlayerLoggedOutEvent e){BURDEN.remove(e.getEntity());}
}
