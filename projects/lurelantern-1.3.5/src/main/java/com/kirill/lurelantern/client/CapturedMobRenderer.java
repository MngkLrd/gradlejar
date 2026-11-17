package com.kirill.lurelantern.client;

import com.kirill.lurelantern.block.LureLanternBlock;
import com.kirill.lurelantern.blockentity.LureLanternBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.SpawnerRenderer;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import java.util.Map;
import java.util.WeakHashMap;

public final class CapturedMobRenderer implements BlockEntityRenderer<LureLanternBlockEntity> {
    private final EntityRenderDispatcher dispatcher;
    private final Map<LureLanternBlockEntity, CachedPreview> previews = new WeakHashMap<>();
    private record CachedPreview(int hash, Entity entity) { }

    public CapturedMobRenderer(BlockEntityRendererProvider.Context context) {
        dispatcher = context.getEntityRenderer();
    }

    @Override
    public void render(LureLanternBlockEntity lantern, float partialTick, PoseStack pose,
                       MultiBufferSource buffers, int packedLight, int packedOverlay) {
        Level level = lantern.getLevel();
        if (level == null || !LureLanternBlock.isAdvancedLantern(lantern.getBlockState())
                || !lantern.hasCapturedMob()) {
            previews.remove(lantern);
            return;
        }
        CompoundTag saved = lantern.getUpdateTag(level.registryAccess()).getCompound("CapturedMob");
        if (!saved.contains("id", 8)) return;
        int key = saved.toString().hashCode();
        CachedPreview existing = previews.get(lantern);
        Entity visual = existing != null && existing.hash() == key ? existing.entity() : null;
        if (visual == null) {
            try {
                visual = EntityType.loadEntityRecursive(saved.copy(), level, entity -> entity);
                if (visual == null) return;
                visual.setNoGravity(true);
                previews.put(lantern, new CachedPreview(key, visual));
            } catch (RuntimeException ex) {
                return;
            }
        }
        pose.pushPose();
        pose.translate(0.22D, 0.04D, 0.22D);
        pose.scale(0.56F, 0.56F, 0.56F);
        try {
            double spin = (level.getGameTime() + partialTick) * 2.0D;
            SpawnerRenderer.renderEntityInSpawner(partialTick, pose, buffers, packedLight,
                    visual, dispatcher, spin - 2.0D, spin);
        } catch (RuntimeException ignored) {
            // A broken custom renderer must never crash the entire client.
        } finally {
            pose.popPose();
        }
    }
}
