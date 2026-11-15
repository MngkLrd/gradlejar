package dev.minemods.blowpipes;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import java.util.function.Consumer;
public class BlowpipeInfoItem extends Item {
 private final String name;
 public BlowpipeInfoItem(Properties properties,String name){super(properties);this.name=name;}
 @Override public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> builder, TooltipFlag flag){
  builder.accept(Component.translatable("tooltip.blowpipes."+name+".primary").withStyle(ChatFormatting.GRAY));
  builder.accept(Component.translatable("tooltip.blowpipes."+name+".secondary").withStyle(ChatFormatting.DARK_GRAY));
 }
}
