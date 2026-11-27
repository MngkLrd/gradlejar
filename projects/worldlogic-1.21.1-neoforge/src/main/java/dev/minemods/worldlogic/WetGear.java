package dev.minemods.worldlogic;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.CustomData;
public final class WetGear {
 static boolean absorbent(ItemStack s){return !s.isEmpty()&&(Material.item(s,"absorbent_gear")||BuiltInRegistries.ITEM.getKey(s.getItem()).getPath().contains("leather")||s.is(Items.BOW));}
 static int wet(ItemStack s){return s.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag().getInt("worldlogic_wet");}
 static void tick(ServerLevel l,Player p,Body.State state){
  if(!Rules.on("wet_gear")&&!Rules.on("wet_bow"))return;
  boolean wet=p.isInWater()||l.isRainingAt(p.blockPosition());int dry=wet?0:AirAndFire.nearHeat(l,p.blockPosition(),2)?8:2;
  state.wet=Math.max(0,Math.min(100,state.wet+(wet?10:-dry)));
  for(int i=0;i<p.getInventory().getContainerSize();i++){ItemStack s=p.getInventory().getItem(i);if(!absorbent(s))continue;boolean exposed=s==p.getMainHandItem()||s==p.getOffhandItem();if(!exposed)for(ItemStack armor:p.getArmorSlots())if(armor==s){exposed=true;break;}
   int old=wet(s);int next=Math.max(0,Math.min(100,old+(wet&&exposed?10:-Math.max(1,dry))));if(next==old)continue;
   CustomData.update(DataComponents.CUSTOM_DATA,s,t->{if(next==0)t.remove("worldlogic_wet");else t.putInt("worldlogic_wet",next);});
  }
 }
}
