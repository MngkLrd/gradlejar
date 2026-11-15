package dev.minemods.blowpipes;

import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

/**
 * Sleep dart is crowd-control, not a vanilla Slowness dart.
 * Mob AI is paused on the logical server, without affecting client rendering.
 * Original NoAI value is restored on wake, after expiry and after world reload.
 */
@EventBusSubscriber(modid = Blowpipes.ID)
public final class BlowpipeEvents {
    private static final String SLEEP_UNTIL = "blowpipes:sleep_until";
    private static final String PREVIOUS_NO_AI = "blowpipes:previous_no_ai";
    private static final String[] PIPE_NAMES = {
            "bamboo_blowpipe", "hunting_blowpipe", "reinforced_blowpipe"
    };
    private static final String[] DART_NAMES = {
            "plain_dart", "iron_dart", "poison_dart",
            "slowness_dart", "fire_dart", "sleep_dart"
    };

    private BlowpipeEvents() { }

    public static boolean putToSleep(Mob mob, int durationTicks) {
        if (mob.level().isClientSide() || !mob.isAlive() || mob.getMaxHealth() >= 100f
                || mob.getType() == EntityType.ELDER_GUARDIAN) return false;

        CompoundTag state = mob.getPersistentData();
        // Don't alter AI that another mod or a command disabled already.
        if (!state.contains(SLEEP_UNTIL, Tag.TAG_LONG)) {
            if (mob.isNoAi()) return false;
            state.putBoolean(PREVIOUS_NO_AI, mob.isNoAi());
        }
        long end = mob.level().getGameTime() + Math.max(1, durationTicks);
        state.putLong(SLEEP_UNTIL, end);
        mob.setTarget(null);
        mob.getNavigation().stop();
        mob.setNoAi(true);
        return true;
    }

    private static void wake(Mob mob, CompoundTag state) {
        if (!state.contains(SLEEP_UNTIL, Tag.TAG_LONG)) return;
        boolean originallyDisabled = state.getBoolean(PREVIOUS_NO_AI);
        state.remove(SLEEP_UNTIL);
        state.remove(PREVIOUS_NO_AI);
        mob.setNoAi(originallyDisabled);
    }

    @SubscribeEvent
    public static void onMobTick(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof Mob mob) || mob.level().isClientSide()) return;
        CompoundTag state = mob.getPersistentData();
        if (!state.contains(SLEEP_UNTIL, Tag.TAG_LONG)) return;

        if (mob.level().getGameTime() >= state.getLong(SLEEP_UNTIL)) {
            wake(mob, state);
            return;
        }
        if (!mob.isNoAi()) mob.setNoAi(true);
        if (mob.level() instanceof ServerLevel server && mob.tickCount % 15 == 0) {
            server.sendParticles(ParticleTypes.WITCH, mob.getX(), mob.getY() + mob.getBbHeight() + 0.2,
                    mob.getZ(), 2, 0.14, 0.08, 0.14, 0.0);
        }
    }

    @SubscribeEvent
    public static void onDamaged(LivingIncomingDamageEvent event) {
        if (event.getEntity() instanceof Mob mob && !mob.level().isClientSide()) {
            // The sleep projectile applies sleep AFTER its own hit. A later hit wakes it.
            wake(mob, mob.getPersistentData());
        }
    }

    @SubscribeEvent
    public static void onTooltip(ItemTooltipEvent event) {
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(event.getItemStack().getItem());
        if (!Blowpipes.ID.equals(id.getNamespace())) return;
        String name = id.getPath();
        boolean ours = false;
        for (String value : PIPE_NAMES) if (name.equals(value)) ours = true;
        for (String value : DART_NAMES) if (name.equals(value)) ours = true;
        if (!ours) return;

        String key = "tooltip.blowpipes." + name;
        event.getToolTip().add(Component.translatable(key + ".primary").withStyle(ChatFormatting.GRAY));
        event.getToolTip().add(Component.translatable(key + ".secondary").withStyle(ChatFormatting.DARK_GRAY));
    }
}
