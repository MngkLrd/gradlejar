package dev.creeperarmor.gameplay;

import dev.creeperarmor.registry.ModItems;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.level.ExplosionEvent;

public final class CreeperGameplayEvents {
    private static final float CRITICAL_HEALTH = 2.0F;
    private static final float ARMOR_BLAST_RADIUS = 2.5F;
    private static final EquipmentSlot[] CONSUMPTION_ORDER = {
            EquipmentSlot.FEET, EquipmentSlot.LEGS, EquipmentSlot.CHEST, EquipmentSlot.HEAD
    };

    @SubscribeEvent
    public void onExplosionDetonate(ExplosionEvent.Detonate event) {
        if (!(event.getLevel() instanceof ServerLevel)) return;
        // A full set prevents health damage and knockback, but its armor still wears.
        // Do this once per explosion because canceling damage suppresses vanilla wear.
        event.getAffectedEntities().removeIf(entity -> {
            if (!(entity instanceof ServerPlayer player)) return false;
            if (BlastProtection.isOwnBlastActive(player)) return true;
            if (!hasCompleteSet(player)) return false;
            for (EquipmentSlot slot : CONSUMPTION_ORDER) {
                ItemStack piece = player.getItemBySlot(slot);
                if (!piece.isEmpty()) {
                    piece.hurtAndBreak(4, player, slot);
                }
            }
            return true;
        });
    }

    @SubscribeEvent
    public void onIncomingDamage(LivingIncomingDamageEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (!event.getSource().is(DamageTypeTags.IS_EXPLOSION)) return;
        if (BlastProtection.isOwnBlastActive(player) || hasCompleteSet(player)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public void onDamageAboutToApply(LivingDamageEvent.Pre event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (!(player.level() instanceof ServerLevel serverLevel)) return;

        // Pre exposes the final health damage; armor and absorption were already processed.
        float healthDamage = Math.max(0.0F, event.getNewDamage());
        if (healthDamage <= 0.0F) return;
        if (player.getHealth() - healthDamage >= CRITICAL_HEALTH) return;

        EquipmentSlot toConsume = nextArmorPiece(player);
        if (toConsume == null) return;

        event.setNewDamage(0.0F);
        if (player.getHealth() < CRITICAL_HEALTH) {
            player.setHealth(CRITICAL_HEALTH);
        }
        player.setItemSlot(toConsume, ItemStack.EMPTY);
        serverLevel.playSound(null, player.blockPosition(), SoundEvents.ITEM_BREAK.value(),
                SoundSource.PLAYERS, 1.0F, 0.85F);

        BlastProtection.explodeSafely(serverLevel, player,
                new Vec3(player.getX(), player.getY() + player.getBbHeight() * 0.5, player.getZ()),
                ARMOR_BLAST_RADIUS);
    }

    @SubscribeEvent
    public void onCreeperDrops(LivingDropsEvent event) {
        if (!(event.getEntity() instanceof Creeper creeper)) return;
        if (!(creeper.level() instanceof ServerLevel level)) return;

        int amount = creeper.isPowered() ? 2 : 1;
        event.getDrops().add(new ItemEntity(level, creeper.getX(), creeper.getY() + 0.3D,
                creeper.getZ(), new ItemStack(ModItems.CREEPER_PLATE.get(), amount)));
        // Additional material for the new chestplate recipe; original plate drop stays intact.
        event.getDrops().add(new ItemEntity(level, creeper.getX(), creeper.getY() + 0.3D,
                creeper.getZ(), new ItemStack(ModItems.CREEPER_POWDER.get(), amount)));
    }

    public static boolean hasCompleteSet(ServerPlayer player) {
        return player.getItemBySlot(EquipmentSlot.HEAD).is(ModItems.CREEPER_HELMET.get())
                && player.getItemBySlot(EquipmentSlot.CHEST).is(ModItems.CREEPER_CHESTPLATE.get())
                && player.getItemBySlot(EquipmentSlot.LEGS).is(ModItems.CREEPER_LEGGINGS.get())
                && player.getItemBySlot(EquipmentSlot.FEET).is(ModItems.CREEPER_BOOTS.get());
    }

    private static EquipmentSlot nextArmorPiece(ServerPlayer player) {
        for (EquipmentSlot slot : CONSUMPTION_ORDER) {
            ItemStack stack = player.getItemBySlot(slot);
            if ((slot == EquipmentSlot.FEET && stack.is(ModItems.CREEPER_BOOTS.get()))
                    || (slot == EquipmentSlot.LEGS && stack.is(ModItems.CREEPER_LEGGINGS.get()))
                    || (slot == EquipmentSlot.CHEST && stack.is(ModItems.CREEPER_CHESTPLATE.get()))
                    || (slot == EquipmentSlot.HEAD && stack.is(ModItems.CREEPER_HELMET.get()))) {
                return slot;
            }
        }
        return null;
    }
}
