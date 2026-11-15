package dev.minemods.traumacraft.medical;

import dev.minemods.traumacraft.health.BodyPart;
import dev.minemods.traumacraft.health.BodyState;
import dev.minemods.traumacraft.health.MedicalAction;
import dev.minemods.traumacraft.item.MedicalItem;
import dev.minemods.traumacraft.network.ModNetwork;
import dev.minemods.traumacraft.registry.ModAttachments;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** One server-authoritative delayed medical action per player. */
public final class TreatmentManager {
    public record Pending(BodyPart part, Item item, MedicalItem.Type treatment, int startTick,
                          int duration, boolean cursor, InteractionHand hand, float startingHealth,
                          double x, double y, double z) {}
    private static final Map<UUID, Pending> ACTIVE = new HashMap<>();
    private TreatmentManager() {}

    public static int duration(MedicalItem.Type type) {
        return switch (type) {
            case RAG_DRESSING -> 20;
            case STERILE_PATCH -> 30;
            case BANDAGE, COMPRESSION_WRAP, HEMOSTATIC_GEL, TOURNIQUET -> 40;
            case SPLINT, FRACTURE_BRACE, BONE_MENDER -> 60;
            case MEDKIT, COMPACT_MEDKIT, FIELD_MEDKIT -> 80;
            case TRAUMA_KIT, RECOVERY_SERUM -> 100;
            case ANALGESIC_TABLETS -> 24;
            case SURGICAL_KIT -> 160;
            case ADRENALINE_INJECTOR, GUARDIAN_INJECTOR, FOCUS_INJECTOR, ENDURANCE_INJECTOR -> 20;
        };
    }

    public static boolean begin(ServerPlayer player, MedicalItem medicine, BodyPart part,
                                boolean cursor, InteractionHand hand) {
        if (!player.isAlive() || player.isSpectator()) return false;
        if (ACTIVE.containsKey(player.getUUID())) return false;
        if (cursor && player.containerMenu != player.inventoryMenu) return false;
        ItemStack stack = stack(player, cursor, hand);
        if (stack.isEmpty() || stack.getItem() != medicine) return false;
        BodyState body = player.getData(ModAttachments.BODY.get());
        if (!MedicalAction.canApply(body, medicine.medicalType(), part)) return false;
        Pending pending = new Pending(part, medicine, medicine.medicalType(), player.tickCount,
                duration(medicine.medicalType()), cursor, hand, player.getHealth(),
                player.getX(), player.getY(), player.getZ());
        ACTIVE.put(player.getUUID(), pending);
        ModNetwork.progress(player, pending.duration(), 0);
        return true;
    }

    private static ItemStack stack(ServerPlayer player, boolean cursor, InteractionHand hand) {
        return cursor ? player.containerMenu.getCarried() : player.getItemInHand(hand);
    }

    /** Cleanup on logout or respawn without sending packets to a closed connection. */
    public static void forget(ServerPlayer player) {
        ACTIVE.remove(player.getUUID());
    }

    public static void cancel(ServerPlayer player) {
        if (ACTIVE.remove(player.getUUID()) != null) ModNetwork.progress(player, 0, 0);
    }

    public static void tick(ServerPlayer player) {
        Pending p = ACTIVE.get(player.getUUID());
        if (p == null) return;
        if (!player.isAlive() || player.isSpectator() || player.isSprinting()
                || (p.cursor() && player.containerMenu != player.inventoryMenu)
                || (p.treatment() == MedicalItem.Type.SURGICAL_KIT &&
                    (Math.abs(player.getX() - p.x()) > 1.5 || Math.abs(player.getZ() - p.z()) > 1.5))
                || (!p.cursor() && !player.isUsingItem())) {
            // Real outside damage cancels treatment from TraumaEvents.onDamage.
            // Never cancel merely because VitalityBridge adjusted the displayed
            // hearts while the patient was bleeding.
            cancel(player);
            return;
        }
        ItemStack held = stack(player, p.cursor(), p.hand());
        if (held.isEmpty() || held.getItem() != p.item()) {
            cancel(player);
            return;
        }
        if (player.tickCount - p.startTick() < p.duration()) return;
        // Pop before applying to prevent reentrancy.
        ACTIVE.remove(player.getUUID());
        ModNetwork.progress(player, 0, 0);
        BodyState body = player.getData(ModAttachments.BODY.get());
        if (!MedicalAction.apply(player, body, p.treatment(), p.part())) return;
        if (!player.getAbilities().instabuild) {
            held.shrink(1);
            if (p.cursor()) {
                player.containerMenu.setCarried(held);
                player.containerMenu.broadcastChanges();
            }
        }
        ModNetwork.sync(player);
    }
}
