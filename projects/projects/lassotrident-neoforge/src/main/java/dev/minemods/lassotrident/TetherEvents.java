package dev.minemods.lassotrident;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.ThrownTrident;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.AnvilUpdateEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.entity.ProjectileImpactEvent;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

@EventBusSubscriber(modid = LassoTrident.MOD_ID)
public final class TetherEvents {
    private static final int MAX_TICKS = 30 * 20;
    private static final double MAX_DISTANCE = 48.0;
    private static final Map<UUID, Anchor> ANCHORS = new HashMap<>();
    private TetherEvents() {}

    private static final class Anchor {
        final int entityId;
        final BlockPos wall;
        final Vec3 position;
        final Vec3 safeTarget;
        final long startTick;
        double ropeLength;
        boolean pulling;
        boolean pressedBefore;

        Anchor(int entityId, BlockPos wall, Vec3 position, Direction face, long startTick) {
            this.entityId = entityId;
            this.wall = wall.immutable();
            this.position = position;
            this.safeTarget = position.add(face.getStepX() * 0.9,
                    face.getStepY() * 0.9, face.getStepZ() * 0.9);
            this.startTick = startTick;
        }
    }

    @SubscribeEvent
    public static void anvil(AnvilUpdateEvent event) {
        if (!event.getLeft().is(Items.TRIDENT)
                || !event.getRight().is(Items.LEAD)
                || TetherData.hasLead(event.getLeft())) return;
        ItemStack result = event.getLeft().copy();
        TetherData.attachLead(result);
        event.setOutput(result);
        event.setCost(2);
        event.setMaterialCost(1);
    }

    @SubscribeEvent
    public static void tooltip(ItemTooltipEvent event) {
        if (event.getItemStack().is(Items.TRIDENT) && TetherData.hasLead(event.getItemStack())) {
            event.getToolTip().add(Component.translatable("lassotrident.tooltip.ready"));
        }
    }

    @SubscribeEvent
    public static void impact(ProjectileImpactEvent event) {
        if (!(event.getProjectile() instanceof ThrownTrident trident)
                || !(event.getRayTraceResult() instanceof BlockHitResult blockHit)
                || !(trident.getOwner() instanceof ServerPlayer player)
                || !(trident.level() instanceof ServerLevel level)
                || !TetherData.hasLead(trident.getPickupItemStackOrigin())) return;

        if (player.position().distanceToSqr(blockHit.getLocation()) > MAX_DISTANCE * MAX_DISTANCE) return;
        release(player, false);
        Vec3 struck = blockHit.getLocation();
        trident.setOwner(null); // Stops Loyalty's auto-return while the trident is mounted.
        trident.setNoGravity(true);
        trident.pickup = net.minecraft.world.entity.projectile.AbstractArrow.Pickup.DISALLOWED;
        Anchor anchor = new Anchor(trident.getId(), blockHit.getBlockPos(), struck,
                blockHit.getDirection(), level.getGameTime());
        anchor.ropeLength = Math.max(2.0, player.getEyePosition().distanceTo(anchor.safeTarget));
        ANCHORS.put(player.getUUID(), anchor);
        TetherNetwork.sendSync(player, trident.getId());
        player.displayClientMessage(Component.translatable("lassotrident.message.attached"), true);
        level.playSound(null, trident.blockPosition(), SoundEvents.CHAIN_PLACE, SoundSource.PLAYERS, 0.7F, 1.2F);
    }

    public static void onControl(ServerPlayer player, boolean down) {
        Anchor anchor = ANCHORS.get(player.getUUID());
        if (anchor == null) return;
        if (!down) {
            anchor.pulling = false;
        } else if (anchor.pressedBefore) {
            release(player, false);
        } else {
            anchor.pressedBefore = true;
            anchor.pulling = true;
        }
    }

    @SubscribeEvent
    public static void playerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        Anchor anchor = ANCHORS.get(player.getUUID());
        if (anchor == null) return;
        ServerLevel level = (ServerLevel) player.level();
        Entity entity = level.getEntity(anchor.entityId);
        if (!(entity instanceof ThrownTrident trident)
                || trident.isRemoved()
                || !player.isAlive()
                || level.getGameTime() - anchor.startTick >= MAX_TICKS
                || !level.hasChunkAt(anchor.wall)
                || level.getBlockState(anchor.wall).isAir()
                || player.position().distanceToSqr(anchor.position) > MAX_DISTANCE * MAX_DISTANCE) {
            release(player, true);
            return;
        }

        // Keep the projectile in the struck face even with Loyalty, gravity, or water.
        trident.setNoGravity(true);
        trident.setDeltaMovement(Vec3.ZERO);
        trident.setPos(anchor.position.x, anchor.position.y, anchor.position.z);


        if (player.isPassenger() || player.getAbilities().flying) return;
        Vec3 eyeToAnchor = anchor.safeTarget.subtract(player.getEyePosition());
        double distance = eyeToAnchor.length();
        if (distance < 1.0e-4) return;
        Vec3 inward = eyeToAnchor.scale(1.0 / distance);
        Vec3 velocity = player.getDeltaMovement();
        boolean changed = false;

        if (anchor.pulling) {
            if (distance <= 1.25) {
                // Reaching the wall does not detach the trident.
                anchor.pulling = false;
            } else {
                // Winch in the rope gradually rather than teleporting or fixing velocity.
                anchor.ropeLength = Math.max(1.5, anchor.ropeLength - 0.34);
                double speed = net.minecraft.util.Mth.clamp(distance * 0.085, 0.18, 0.48);
                Vec3 desired = inward.scale(speed);
                velocity = velocity.scale(0.70).add(desired.scale(0.30));
                changed = true;
            }
        }

        // When the rope reaches full extension, it becomes a soft radial constraint.
        // Tangential velocity remains, so the player can swing rather than freeze.
        double extension = distance - anchor.ropeLength;
        if (extension > 0.0) {
            double outwardSpeed = -velocity.dot(inward);
            if (outwardSpeed > 0.0) velocity = velocity.add(inward.scale(outwardSpeed * 0.92));
            velocity = velocity.add(inward.scale(Math.min(0.16, extension * 0.19)));
            changed = true;
        }

        if (changed) {
            player.setDeltaMovement(velocity);
            player.hurtMarked = true;
            player.fallDistance = 0.0F;
        }
    }

    @SubscribeEvent
    public static void logout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) release(player, true);
    }

    private static void release(ServerPlayer player, boolean expired) {
        Anchor anchor = ANCHORS.remove(player.getUUID());
        if (anchor == null) return;
        ServerLevel level = (ServerLevel) player.level();
        Entity entity = level.getEntity(anchor.entityId);
        if (entity instanceof ThrownTrident trident && !trident.isRemoved()) {
            ItemStack stack = trident.getPickupItemStackOrigin().copy();
            if (!stack.isEmpty()) trident.spawnAtLocation(stack, 0.1F);
            trident.discard();
        }
        TetherNetwork.sendSync(player, -1);
        player.displayClientMessage(Component.translatable(
                expired ? "lassotrident.message.expired" : "lassotrident.message.released"), true);
        level.playSound(null, BlockPos.containing(anchor.position), SoundEvents.LEASH_KNOT_BREAK,
                SoundSource.PLAYERS, 0.8F, 1.0F);
    }
}
