package dev.minemods.traumacraft.item;

import dev.minemods.traumacraft.health.BodyPart;
import dev.minemods.traumacraft.health.BodyState;
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
    public MedicalItem(Properties properties, Type type) {
        super(properties);
        this.type = type;
    }
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide()) return InteractionResultHolder.success(stack);
        BodyState state = player.getData(ModAttachments.BODY.get());
        boolean changed = false;
        String result = "";
        switch (type) {
            case BANDAGE -> {
                BodyPart target = state.mostBleeding();
                if (target != null) {
                    state.stopBleeding(target);
                    changed = true;
                    result = "Bandaged " + target.label.toLowerCase();
                }
            }
            case SPLINT -> {
                BodyPart target = state.firstFracture();
                if (target != null) {
                    state.setFractured(target, false);
                    changed = true;
                    result = "Splinted " + target.label.toLowerCase();
                }
            }
            case MEDKIT -> {
                changed = state.healMostDamaged(25);
                result = "Restored 25 HP to the most damaged body part";
            }
        }
        if (!changed) {
            player.displayClientMessage(Component.literal("No applicable injury"), true);
            return InteractionResultHolder.fail(stack);
        }
        if (!player.getAbilities().instabuild) stack.shrink(1);
        player.displayClientMessage(Component.literal(result), true);
        return InteractionResultHolder.success(stack);
    }
}
