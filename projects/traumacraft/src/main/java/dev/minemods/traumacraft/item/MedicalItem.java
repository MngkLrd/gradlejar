package dev.minemods.traumacraft.item;

import dev.minemods.traumacraft.health.BodyPart;
import dev.minemods.traumacraft.health.BodyState;
import dev.minemods.traumacraft.registry.ModAttachments;
import dev.minemods.traumacraft.network.MedicalNetworking;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public final class MedicalItem extends Item {
    public enum Type { BANDAGE, SPLINT, MEDKIT }
    public MedicalItem(Properties properties, Type type) { super(properties); }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide()) return InteractionResultHolder.success(stack);
        BodyState state = player.getData(ModAttachments.BODY.get());
        BodyPart part = MedicineLogic.defaultTarget(state, stack);
        if (part != null && MedicineLogic.treat(player, stack, part)) {
            if (player instanceof net.minecraft.server.level.ServerPlayer server) MedicalNetworking.sync(server);
            return InteractionResultHolder.success(stack);
        }
        return InteractionResultHolder.fail(stack);
    }
}
