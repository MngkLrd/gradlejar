package dev.norsegear.item;

import dev.norsegear.NorseGear;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ShieldItem;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.ItemAttributeModifiers;

/** Armor bonus is granted by vanilla's item attribute system only in OFFHAND. */
public final class BossedShieldItem extends ShieldItem {
    public BossedShieldItem(Properties properties) {
        super(properties);
    }

    public static ItemAttributeModifiers offhandArmor() {
        return ItemAttributeModifiers.builder().add(
            Attributes.ARMOR,
            new AttributeModifier(
                ResourceLocation.fromNamespaceAndPath(NorseGear.MODID, "bossed_shield_offhand_armor"),
                2.0D, AttributeModifier.Operation.ADD_VALUE),
            EquipmentSlotGroup.OFFHAND
        ).build();
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context,
            List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        tooltip.add(Component.translatable("tooltip.norsegear.bossed_shield"));
    }
}
