package dev.minemods.thermal.content;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.*;
import java.util.List;
public final class ThermalInfoItem extends Item {
    public ThermalInfoItem(Properties properties){super(properties);}
    @Override public void appendHoverText(ItemStack stack,TooltipContext context,List<Component> lines,TooltipFlag flag){
        super.appendHoverText(stack,context,lines,flag);
        lines.add(Component.translatable(getDescriptionId()+".info").withStyle(ChatFormatting.GRAY));
    }
}
