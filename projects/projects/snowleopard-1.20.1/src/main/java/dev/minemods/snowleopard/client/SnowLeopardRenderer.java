package dev.minemods.snowleopard.client;

import dev.minemods.snowleopard.SnowLeopardMod;
import dev.minemods.snowleopard.entity.SnowLeopardEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

public final class SnowLeopardRenderer extends MobRenderer<SnowLeopardEntity, SnowLeopardModel<SnowLeopardEntity>> {
    private static final ResourceLocation TEXTURE =
        new ResourceLocation(SnowLeopardMod.MOD_ID, "textures/entity/snow_leopard.png");

    public SnowLeopardRenderer(EntityRendererProvider.Context context) {
        super(context, new SnowLeopardModel<>(context.bakeLayer(SnowLeopardModel.LAYER)), 0.55f);
    }

    @Override
    public ResourceLocation getTextureLocation(SnowLeopardEntity entity) {
        return TEXTURE;
    }
}
