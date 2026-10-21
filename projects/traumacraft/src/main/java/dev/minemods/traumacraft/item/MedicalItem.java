package dev.minemods.traumacraft.item;

import dev.minemods.traumacraft.health.BodyPart;
import dev.minemods.traumacraft.health.BodyState;
import dev.minemods.traumacraft.health.MedicalAction;
import dev.minemods.traumacraft.registry.ModAttachments;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public final class MedicalItem extends Item {
    public enum Type { BANDAGE, SPLINT, MEDKIT }
    private final Type type;
    public MedicalItem(Properties properties, Type type) { super(properties); this.type = type; }
    public Type medicalType() { return type; }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide()) return InteractionResultHolder.success(stack);
        BodyState state = player.getData(ModAttachments.BODY.get());
        BodyPart target = switch (type) {
            case BANDAGE -> state.mostBleeding();
            case SPLINT -> state.firstFracture();
            case MEDKIT -> state.mostDamaged();
        };
        if (target == null || !MedicalAction.apply(state, type, target)) {
            player.displayClientMessage(Component.translatable("message.traumacraft.no_injury"), true);
            return InteractionResultHolder.fail(stack);
        }
        if (!player.getAbilities().instabuild) stack.shrink(1);
        player.displayClientMessage(Component.translatable("message.traumacraft.treated", target.label), true);
        return InteractionResultHolder.success(stack);
    }
}
