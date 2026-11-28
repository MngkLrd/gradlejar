package dev.minemods.aquaculture;
import dev.minemods.aquaculture.api.AquaApi;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.CustomData;
import java.util.*;

public final class FishPortion extends Item {
 final Species species;
 final AquaApi.Preparation preparation;
 FishPortion(Species species,AquaApi.Preparation preparation){super(new Properties());this.species=species;this.preparation=preparation;}
 public Optional<AquaApi.FoodPortion> describe(ItemStack stack){
  if(stack.isEmpty()||stack.getItem()!=this||!stack.has(DataComponents.FOOD))return Optional.empty();
  var tag=stack.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag();
  if(!tag.contains("wl_portion_grams",Tag.TAG_INT))return Optional.empty();
  int grams=tag.getInt("wl_portion_grams"),limit=preparation==AquaApi.Preparation.RAW?100:80;
  if(grams<1||grams>limit)return Optional.empty();
  return Optional.of(new AquaApi.FoodPortion(species.id(),grams,preparation));
 }
 @Override public void appendHoverText(ItemStack stack,TooltipContext context,List<Component> lines,TooltipFlag flag){
  describe(stack).ifPresentOrElse(p->lines.add(Component.translatable("aquaculture.portion_mass",p.edibleMassGrams())),()->lines.add(Component.translatable("aquaculture.portion_unknown")));
 }
}
