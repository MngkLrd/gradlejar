package com.kirill.pocketrealms.client;

import com.kirill.pocketrealms.PocketRealms;
import com.kirill.pocketrealms.entity.CorruptedZombie;
import net.minecraft.client.model.ZombieModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

public class CorruptedZombieRenderer extends MobRenderer<CorruptedZombie, ZombieModel<CorruptedZombie>> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
            PocketRealms.MOD_ID, "textures/entity/corrupted_zombie.png");
    public CorruptedZombieRenderer(EntityRendererProvider.Context context) {
        super(context, new HorrorZombieModel(context.bakeLayer(HorrorZombieModel.LAYER)), 0.5F);
    }
    @Override
    public ResourceLocation getTextureLocation(CorruptedZombie entity) {
        return TEXTURE;
    }
}
