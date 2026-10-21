package dev.spidercentaur.client;

import dev.spidercentaur.entity.SpiderCentaurEntity;
import net.minecraft.client.model.SpiderModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

public final class SpiderCentaurRenderer
        extends MobRenderer<SpiderCentaurEntity, SpiderModel<SpiderCentaurEntity>> {

    private static final ResourceLocation SPIDER_TEXTURE =
            ResourceLocation.withDefaultNamespace("textures/entity/spider/spider.png");

    public SpiderCentaurRenderer(EntityRendererProvider.Context context) {
        super(context, createSpiderBody(context), 0.8F);
        this.addLayer(new SkeletonUpperLayer(this, context.bakeLayer(ModelLayers.SKELETON),
                context.getItemInHandRenderer()));
    }

    private static SpiderModel<SpiderCentaurEntity> createSpiderBody(EntityRendererProvider.Context context) {
        SpiderModel<SpiderCentaurEntity> model =
                new SpiderModel<>(context.bakeLayer(ModelLayers.SPIDER));
        // Replacing the spider head with a skeleton upper body is intentional.
        model.root().getChild("head").visible = false;
        return model;
    }

    @Override
    public ResourceLocation getTextureLocation(SpiderCentaurEntity entity) {
        return SPIDER_TEXTURE;
    }
}
