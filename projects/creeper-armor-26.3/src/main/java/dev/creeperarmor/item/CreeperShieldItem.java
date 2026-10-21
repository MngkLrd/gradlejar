package dev.creeperarmor.item;

import dev.creeperarmor.gameplay.BlastProtection;
import dev.creeperarmor.registry.ModItems;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ShieldItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public final class CreeperShieldItem extends ShieldItem {
    public static final int EXPLOSION_DURABILITY_COST = 50;
    private static final int ARMING_TICKS = 12;
    private static final float SHIELD_BLAST_RADIUS = 1.8F;
    private static final double BLAST_DISTANCE = 2.4D;

    public CreeperShieldItem(Properties properties) {
        super(properties);
    }

    @Override
    public void onUseTick(Level level, LivingEntity user, ItemStack stack, int remainingUseDuration) {
        if (!(level instanceof ServerLevel serverLevel) || !(user instanceof ServerPlayer player)) return;
        int heldTicks = stack.getUseDuration(player) - remainingUseDuration;
        if (heldTicks != ARMING_TICKS) return;

        InteractionHand hand = player.getUsedItemHand();
        EquipmentSlot equipmentSlot = hand == InteractionHand.OFF_HAND ? EquipmentSlot.OFFHAND : EquipmentSlot.MAINHAND;
        if (stack.getMaxDamage() - stack.getDamageValue() < EXPLOSION_DURABILITY_COST) return;

        Vec3 facing = player.getLookAngle();
        Vec3 center = player.getEyePosition().add(facing.scale(BLAST_DISTANCE));
        BlastProtection.explodeSafely(serverLevel, player, center, SHIELD_BLAST_RADIUS);
        stack.hurtAndBreak(EXPLOSION_DURABILITY_COST, player, equipmentSlot);
    }

}
