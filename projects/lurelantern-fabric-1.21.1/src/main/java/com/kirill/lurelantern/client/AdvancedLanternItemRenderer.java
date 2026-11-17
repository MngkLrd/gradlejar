package com.kirill.lurelantern.client;

import com.kirill.lurelantern.util.CapturedMobData;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** Small held lantern, with a transparent 3D window and actual captive preview. */
public final class AdvancedLanternItemRenderer extends BlockEntityWithoutLevelRenderer {
    private int cachedHash = Integer.MIN_VALUE;
    private Level cachedLevel;
    private Entity cachedMob;

    public AdvancedLanternItemRenderer() {
        super(Minecraft.getInstance().getBlockEntityRenderDispatcher(),
                Minecraft.getInstance().getEntityModels());
    }

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext context, PoseStack pose,
                             MultiBufferSource buffers, int light, int overlay) {
        Minecraft mc = Minecraft.getInstance();
        Level level = mc.level;
        boolean active = CapturedMobData.isLureActive(stack);
        boolean held = context == ItemDisplayContext.FIRST_PERSON_RIGHT_HAND
                || context == ItemDisplayContext.FIRST_PERSON_LEFT_HAND;
        pose.pushPose();
        try {
            if (held) {
                // builtin/entity JSON already has the hand transform. Shrink about its center.
                pose.translate(0.5D, 0.5D, 0.5D);
                pose.scale(0.44F, 0.44F, 0.44F);
                pose.translate(-0.5D, -0.5D, -0.5D);
            }
            AdvancedLanternHandMesh.renderFrame(active, pose, buffers, light, overlay);
            if (level != null) {
                CompoundTag tag = CapturedMobData.getCapturedMobTag(stack).orElse(null);
                if (tag != null && tag.contains("id", 8)) {
                    int hash = tag.hashCode();
                    if (cachedMob == null || hash != cachedHash || cachedLevel != level) {
                        cachedHash = hash;
                        cachedLevel = level;
                        cachedMob = null;
                        try {
                            cachedMob = EntityType.loadEntityRecursive(tag.copy(), level, entity -> entity);
                            if (cachedMob != null) cachedMob.setNoGravity(true);
                        } catch (RuntimeException ignored) {
                            cachedMob = null;
                        }
                    }
                    if (cachedMob != null) {
                        try {
                            CapturedMobRenderer.renderMiniature(cachedMob, level,
                                    mc.getTimer().getGameTimeDeltaPartialTick(true), false,
                                    pose, buffers, mc.getEntityRenderDispatcher(), hash);
                        } catch (RuntimeException ignored) {
                            // A modded entity preview must never crash the player.
                        }
                    }
                } else {
                    cachedMob = null;
                    cachedHash = Integer.MIN_VALUE;
                }
            }
            AdvancedLanternHandMesh.renderGlass(active, pose, buffers, light, overlay);
        } finally {
            pose.popPose();
        }
    }
}
