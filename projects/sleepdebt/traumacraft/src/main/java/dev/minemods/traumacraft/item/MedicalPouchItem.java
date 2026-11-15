package dev.minemods.traumacraft.item;

import dev.minemods.traumacraft.menu.MedicalPouchMenu;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** Four actual synchronized slots backed by the pouch ItemStack's vanilla container data. */
public final class MedicalPouchItem extends Item {
    public MedicalPouchItem(Properties properties) { super(properties); }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack=player.getItemInHand(hand);
        if (!level.isClientSide() && player instanceof ServerPlayer server) {
            server.openMenu(new SimpleMenuProvider(
                (id, inventory, owner)->new MedicalPouchMenu(id,inventory,hand),
                Component.translatable("container.traumacraft.medical_pouch")));
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }
}
