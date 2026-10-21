package dev.minemods.traumacraft.item;

import dev.minemods.traumacraft.health.BodyPart;
import dev.minemods.traumacraft.health.BodyState;
import dev.minemods.traumacraft.registry.ModAttachments;
import dev.minemods.traumacraft.registry.ModItems;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public final class MedicineLogic {
    private MedicineLogic() {}
    public static boolean medical(ItemStack stack) {
        return stack.is(ModItems.BANDAGE.get()) || stack.is(ModItems.SPLINT.get()) || stack.is(ModItems.MEDKIT.get());
    }

    public static BodyPart defaultTarget(BodyState state, ItemStack stack) {
        if (stack.is(ModItems.BANDAGE.get())) return state.mostBleeding();
        if (stack.is(ModItems.SPLINT.get())) return state.firstFracture();
        if (stack.is(ModItems.MEDKIT.get())) {
            BodyPart worst = null;
            float ratio = 1f;
            for (BodyPart part : BodyPart.values()) {
                if (state.percent(part) < ratio) {
                    worst = part;
                    ratio = state.percent(part);
                }
            }
            return worst;
        }
        return null;
    }

    public static boolean treat(Player player, ItemStack stack, BodyPart part) {
        if (stack.isEmpty()) return false;
        BodyState state = player.getData(ModAttachments.BODY.get());
        String message;
        if (stack.is(ModItems.BANDAGE.get()) && state.bleeding(part) > 0) {
            state.stopBleeding(part);
            message = "Bleeding stopped: " + part.label;
        } else if (stack.is(ModItems.SPLINT.get()) && state.fractured(part)) {
            state.setFractured(part, false);
            message = "Fracture treated: " + part.label;
        } else if (stack.is(ModItems.MEDKIT.get()) && state.heal(part, 25)) {
            message = "Restored 25 local HP: " + part.label;
        } else {
            player.displayClientMessage(Component.literal("Medicine cannot treat this body part"), true);
            return false;
        }
        if (!player.getAbilities().instabuild) stack.shrink(1);
        player.displayClientMessage(Component.literal(message), true);
        return true;
    }
}
