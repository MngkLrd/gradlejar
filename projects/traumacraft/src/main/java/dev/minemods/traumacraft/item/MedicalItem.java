package dev.minemods.traumacraft.item;

import dev.minemods.traumacraft.health.BodyPart;
import dev.minemods.traumacraft.health.BodyState;
import dev.minemods.traumacraft.health.MedicalAction;
import dev.minemods.traumacraft.medical.TreatmentManager;
import net.minecraft.server.level.ServerPlayer;
import dev.minemods.traumacraft.registry.ModAttachments;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** Server-side treatment logic is shared between RMB and body-panel clicks. */
public final class MedicalItem extends Item {
    public enum Type {
        BANDAGE, SPLINT, MEDKIT, RAG_DRESSING,
        STERILE_PATCH, COMPRESSION_WRAP, HEMOSTATIC_GEL, TOURNIQUET,
        FRACTURE_BRACE, BONE_MENDER, COMPACT_MEDKIT, FIELD_MEDKIT,
        TRAUMA_KIT, SURGICAL_KIT, RECOVERY_SERUM,
        ADRENALINE_INJECTOR, GUARDIAN_INJECTOR, FOCUS_INJECTOR, ENDURANCE_INJECTOR
    }
    private final Type type;
    public MedicalItem(Properties properties, Type type) {
        super(properties);
        this.type = type;
    }
    public Type medicalType() { return type; }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide()) return InteractionResultHolder.success(stack);
        if (!(player instanceof ServerPlayer server)) return InteractionResultHolder.fail(stack);
        BodyState state = player.getData(ModAttachments.BODY.get());
        BodyPart target = MedicalAction.autoTarget(state, type);
        if (target == null || !TreatmentManager.begin(server, this, target, false, hand)) {
            return InteractionResultHolder.fail(stack);
        }
        return InteractionResultHolder.success(stack);
    }
}
