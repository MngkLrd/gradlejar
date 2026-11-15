package com.kirill.pocketrealms.client;

import com.kirill.pocketrealms.PocketRealms;
import com.kirill.pocketrealms.entity.CorruptedSkeleton;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.SkeletonRenderer;
import net.minecraft.resources.ResourceLocation;

/** SkeletonRenderer includes vanilla held item, armor and bow aiming layers. */
public class CorruptedSkeletonRenderer extends SkeletonRenderer<CorruptedSkeleton> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
            PocketRealms.MOD_ID, "textures/entity/corrupted_skeleton.png");
    public CorruptedSkeletonRenderer(EntityRendererProvider.Context ctx) { super(ctx); this.model = new HorrorSkeletonModel(ctx.bakeLayer(HorrorSkeletonModel.LAYER)); }
    @Override
    public ResourceLocation getTextureLocation(CorruptedSkeleton entity) { return TEXTURE; }
}
