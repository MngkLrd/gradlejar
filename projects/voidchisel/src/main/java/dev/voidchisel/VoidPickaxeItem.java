package dev.voidchisel;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/** Actual pickaxe behavior for ordinary blocks; always-visible five-use durability bar. */
public final class VoidPickaxeItem extends PickaxeItem {
    public VoidPickaxeItem(Tier tier, Properties properties) {
        super(tier, properties);
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return true;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context,
                                List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        tooltip.add(Component.translatable("tooltip.voidchisel.description")
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.voidchisel.charges",
                        Math.max(0, stack.getMaxDamage() - stack.getDamageValue()))
                .withStyle(ChatFormatting.LIGHT_PURPLE));
    }
}
