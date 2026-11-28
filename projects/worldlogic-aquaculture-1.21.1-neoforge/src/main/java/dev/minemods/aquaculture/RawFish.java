package dev.minemods.aquaculture;
import java.util.List;
import java.util.Locale;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.CustomData;
public final class RawFish extends Item{
 public RawFish(Properties p){super(p);}
 @Override public net.minecraft.world.InteractionResultHolder<ItemStack> use(net.minecraft.world.level.Level level,net.minecraft.world.entity.player.Player player,net.minecraft.world.InteractionHand hand){
  ItemStack whole=player.getItemInHand(hand);
  if(hand!=net.minecraft.world.InteractionHand.MAIN_HAND||!player.isShiftKeyDown()||!player.getOffhandItem().is(Items.SHEARS))return super.use(level,player,hand);
  Species species=Species.ALL.stream().filter(s->whole.is(Aquaculture.RAW.get(s.id()).get())).findFirst().orElse(null);
  if(species==null)return net.minecraft.world.InteractionResultHolder.fail(whole);
  if(level.isClientSide)return net.minecraft.world.InteractionResultHolder.success(whole);
  var parts=FishPortions.split(whole,species);
  if(parts.isEmpty()){player.displayClientMessage(Component.translatable("aquaculture.processing.no_yield"),true);return net.minecraft.world.InteractionResultHolder.fail(whole);}
  int grams=FishPortions.edibleGrams(whole,species);
  whole.shrink(1);
  if(!player.getAbilities().instabuild)player.getOffhandItem().hurtAndBreak(1,player,net.minecraft.world.entity.EquipmentSlot.OFFHAND);
  for(ItemStack part:parts){player.getInventory().add(part);if(!part.isEmpty())player.drop(part,false);}
  player.displayClientMessage(Component.translatable("aquaculture.processing.yield",grams),true);
  return net.minecraft.world.InteractionResultHolder.consume(whole);
 }
 @Override public void appendHoverText(ItemStack stack,TooltipContext context,List<Component> lines,TooltipFlag flag){
  var t=stack.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag();
  Species s=Species.ALL.stream().filter(x->stack.is(Aquaculture.RAW.get(x.id()).get())).findFirst().orElse(null);
  if(s==null)return;var mass=FishMass.read(t,s);if(mass.isEmpty())return;
  lines.add(Component.translatable("aquaculture.mass",String.format(Locale.ROOT,"%.3f",mass.getAsDouble())));
  lines.add(Component.translatable("aquaculture.length",Math.round(t.getDouble("wl_fish_length_m")*100)));
  lines.add(Component.translatable("aquaculture.age",t.getInt("wl_fish_age_years")));
  lines.add(Component.translatable(t.getBoolean("wl_fish_female")?"aquaculture.female":"aquaculture.male"));
 }
}

