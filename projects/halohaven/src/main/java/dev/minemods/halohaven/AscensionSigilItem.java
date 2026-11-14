package dev.minemods.halohaven;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;

public final class AscensionSigilItem extends Item {
    public AscensionSigilItem(Properties properties) { super(properties); }

    @Override
    public InteractionResult onItemUseFirst(ItemStack stack, UseOnContext context) {
        Player player = context.getPlayer();
        if (player == null || !context.getLevel().getBlockState(context.getClickedPos()).is(Blocks.BELL)) {
            return InteractionResult.PASS;
        }
        if (context.getLevel().isClientSide()) return InteractionResult.SUCCESS;
        if (!(player instanceof ServerPlayer sp)) return InteractionResult.PASS;
        BlockPos bell = context.getClickedPos();
        if (!ritualReady((ServerLevel) context.getLevel(), bell)) {
            sp.displayClientMessage(Component.translatable("message.halohaven.altar"), true);
            return InteractionResult.SUCCESS;
        }
        ServerLevel destination = sp.getServer().getLevel(HaloHaven.HEAVEN);
        if (destination == null) {
            sp.displayClientMessage(Component.literal("Halo Haven dimension is missing. Check the datapack."), false);
            return InteractionResult.FAIL;
        }
        sp.getPersistentData().putDouble("halohaven_return_x", sp.getX());
        sp.getPersistentData().putDouble("halohaven_return_y", sp.getY());
        sp.getPersistentData().putDouble("halohaven_return_z", sp.getZ());
        ((ServerLevel) context.getLevel()).sendParticles(ParticleTypes.END_ROD,
            bell.getX()+0.5, bell.getY()+0.8, bell.getZ()+0.5, 110, 1.2, 2, 1.2, 0.06);
        sp.level().playSound(null, bell, SoundEvents.BEACON_ACTIVATE, SoundSource.BLOCKS, 1.2f, 1.4f);
        HavenVillages.ensure(destination, 0, 0);
        sp.teleportTo(destination, 0.5, HavenVillages.WALK_Y + 1.0, 5.5, sp.getYRot(), sp.getXRot());
        sp.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 400, 0, false, true));
        sp.getCooldowns().addCooldown(this, 50);
        sp.displayClientMessage(Component.translatable("message.halohaven.welcome"), false);
        return InteractionResult.SUCCESS;
    }

    private static boolean ritualReady(ServerLevel world, BlockPos bell) {
        if (bell.getY() < 170 || !world.canSeeSky(bell.above())) return false;
        if (!world.getBlockState(bell.below()).is(Blocks.QUARTZ_BLOCK)) return false;
        for (int dx : new int[]{-1,1}) for (int dz : new int[]{-1,1}) {
            if (!world.getBlockState(bell.offset(dx, -1, dz)).is(Blocks.AMETHYST_BLOCK)) return false;
        }
        return true;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.dimension().equals(HaloHaven.HEAVEN)) return InteractionResultHolder.pass(stack);
        if (level.isClientSide()) return InteractionResultHolder.success(stack);
        if (!(player instanceof ServerPlayer sp)) return InteractionResultHolder.pass(stack);
        ServerLevel overworld = sp.getServer().overworld();
        double x = sp.getPersistentData().contains("halohaven_return_x")
            ? sp.getPersistentData().getDouble("halohaven_return_x") : overworld.getSharedSpawnPos().getX()+0.5;
        double y = sp.getPersistentData().contains("halohaven_return_y")
            ? sp.getPersistentData().getDouble("halohaven_return_y") : overworld.getSharedSpawnPos().getY()+1;
        double z = sp.getPersistentData().contains("halohaven_return_z")
            ? sp.getPersistentData().getDouble("halohaven_return_z") : overworld.getSharedSpawnPos().getZ()+0.5;
        sp.teleportTo(overworld, x, y, z, sp.getYRot(), sp.getXRot());
        sp.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 120, 0, false, true));
        sp.getCooldowns().addCooldown(this, 50);
        overworld.playSound(null, sp.blockPosition(), SoundEvents.BEACON_DEACTIVATE, SoundSource.PLAYERS, 0.8f, 1.2f);
        return InteractionResultHolder.success(stack);
    }
}
