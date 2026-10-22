package com.kirill.pocketrealms.client;

import com.kirill.pocketrealms.entity.VeilGhost;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.SkeletonRenderer;
import net.minecraft.resources.ResourceLocation;

/** Ethereal humanoid silhouette with native 64x64 UV and alpha blending. */
public final class VeilGhostRenderer extends SkeletonRenderer<VeilGhost> {
    private static final ResourceLocation SKIN=ResourceLocation.fromNamespaceAndPath(
            "pocketrealms","textures/entity/veil_ghost.png");
    public VeilGhostRenderer(EntityRendererProvider.Context context){super(context);}
    @Override public ResourceLocation getTextureLocation(VeilGhost ghost){return SKIN;}
    @Override protected RenderType getRenderType(VeilGhost ghost,boolean visible,boolean translucent,boolean glowing){
        return RenderType.entityTranslucent(SKIN);
    }
}
