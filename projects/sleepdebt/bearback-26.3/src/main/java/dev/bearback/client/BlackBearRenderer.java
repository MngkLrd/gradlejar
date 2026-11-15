package dev.bearback.client;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.PolarBearRenderState;
import net.minecraft.resources.Identifier;

public final class BlackBearRenderer extends BearPolarRenderer {
    public BlackBearRenderer(EntityRendererProvider.Context context){super(context);}
    @Override public Identifier getTextureLocation(PolarBearRenderState state){
        return BlackBearTexture.texture();
    }
}
