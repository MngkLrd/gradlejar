package dev.minemods.aquaculture;
import java.util.List;
import java.util.Locale;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.CustomData;
public final class RawFish extends Item{
 public RawFish(Properties p){super(p);}
 @Override public void appendHoverText(ItemStack stack,TooltipContext context,List<Component> lines,TooltipFlag flag){
  var t=stack.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag();
  if(!t.contains("wl_fish_mass_kg"))return;
  lines.add(Component.translatable("aquaculture.mass",String.format(Locale.ROOT,"%.2f",t.getDouble("wl_fish_mass_kg"))));
  lines.add(Component.translatable("aquaculture.length",Math.round(t.getDouble("wl_fish_length_m")*100)));
  lines.add(Component.translatable("aquaculture.age",t.getInt("wl_fish_age_years")));
  lines.add(Component.translatable(t.getBoolean("wl_fish_female")?"aquaculture.female":"aquaculture.male"));
 }
}

