package dev.ataflore.huss.client;

import dev.ataflore.huss.HussMod;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.VillagerRenderer;
import net.minecraft.resources.ResourceLocation;

public final class TrainingRenderer extends VillagerRenderer {
    public static final ModelLayerLocation LAYER=new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath(HussMod.ID,"training_villager"),"main");
    public TrainingRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.model=new TrainingModel(context.bakeLayer(LAYER));
    }
}
