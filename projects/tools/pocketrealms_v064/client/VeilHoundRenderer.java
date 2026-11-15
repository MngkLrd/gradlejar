package com.kirill.pocketrealms.client;

import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.WolfRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.animal.Wolf;

/** Preserves the animated vanilla wolf mesh but draws alpha-blended shadow fur. */
public final class VeilHoundRenderer extends WolfRenderer {
    private static final ResourceLocation SKIN=ResourceLocation.fromNamespaceAndPath(
            "pocketrealms","textures/entity/veil_hound.png");
    public VeilHoundRenderer(EntityRendererProvider.Context context){super(context);}
    @Override public ResourceLocation getTextureLocation(Wolf wolf){return SKIN;}
    @Override protected RenderType getRenderType(Wolf wolf,boolean visible,boolean translucent,boolean glowing){
        return RenderType.entityTranslucent(SKIN);
    }
}
