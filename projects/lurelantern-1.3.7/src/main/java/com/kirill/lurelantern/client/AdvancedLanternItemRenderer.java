package com.kirill.lurelantern.client;

import com.kirill.lurelantern.block.LureLanternBlock;
import com.kirill.lurelantern.registry.ModBlocks;
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
import net.minecraft.world.level.block.state.BlockState;

/** Draws a translucent lantern with the actual captured mob, even while held. */
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
        Minecraft minecraft = Minecraft.getInstance();
        Level level = minecraft.level;
        boolean active = CapturedMobData.isLureActive(stack);
        BlockState state = ModBlocks.ADVANCED_LURE_LANTERN.get().defaultBlockState()
                .setValue(LureLanternBlock.ACTIVE, active);
        // The camera/perspective transform from the item model is already applied.
        // Render the captive first; draw the translucent glass in front of it.
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
                                minecraft.getTimer().getGameTimeDeltaPartialTick(true),
                                false, pose, buffers, minecraft.getEntityRenderDispatcher(), hash);
                    } catch (RuntimeException ignored) {
                        // Modded entity previews must not crash the client.
                    }
                }
            } else {
                cachedMob = null;
                cachedHash = Integer.MIN_VALUE;
            }
        }
        minecraft.getBlockRenderer().renderSingleBlock(state, pose, buffers, light, overlay);
    }
}
