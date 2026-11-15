package dev.bearback.client;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.PolarBearRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.animal.PolarBear;

public final class BlackBearRenderer extends PolarBearRenderer {
    public BlackBearRenderer(EntityRendererProvider.Context context){super(context);}
    @Override public ResourceLocation getTextureLocation(PolarBear bear){
        return BlackBearTexture.texture();
    }
}
