package dev.minemods.redstonevision.item;

import dev.minemods.redstonevision.client.ClientGogglesExtensions;
import dev.minemods.redstonevision.menu.GogglesChargeMenu;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import org.jetbrains.annotations.Nullable;

public final class EngineerGogglesItem extends ArmorItem {
    public EngineerGogglesItem(Holder<ArmorMaterial> material, Properties properties) {
        super(material, ArmorItem.Type.HELMET, properties);
    }
    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        ClientGogglesExtensions.initialize(consumer);
    }
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!player.isShiftKeyDown()) return super.use(level, player, hand);
        if (!level.isClientSide && player instanceof ServerPlayer server) {
            server.openMenu(new MenuProvider() {
                @Override public Component getDisplayName() {
                    return Component.translatable("screen.redstonevision.title");
                }
                @Nullable @Override public AbstractContainerMenu createMenu(int id, Inventory inventory, Player viewer) {
                    return new GogglesChargeMenu(id, inventory, hand);
                }
            });
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }
    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext ctx, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, ctx, tooltip, flag);
        int seconds = (GogglesPower.ticks(stack) + 19) / 20;
        tooltip.add(Component.translatable("tooltip.redstonevision.fuel", seconds, GogglesPower.MAX_TICKS / 20));
        tooltip.add(Component.translatable("tooltip.redstonevision.open"));
        tooltip.add(Component.translatable("tooltip.redstonevision.worn"));
    }
    @Override public boolean isBarVisible(ItemStack stack) { return true; }
    @Override public int getBarWidth(ItemStack stack) {
        return Math.round(13F * GogglesPower.ticks(stack) / GogglesPower.MAX_TICKS);
    }
    @Override public int getBarColor(ItemStack stack) {
        return GogglesPower.ticks(stack) == 0 ? 0xE34A4A : 0x66CC88;
    }
}
