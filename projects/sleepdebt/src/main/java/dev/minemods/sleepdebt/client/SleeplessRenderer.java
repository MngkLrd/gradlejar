package dev.minemods.sleepdebt.client;
import dev.minemods.sleepdebt.SleepDebtMod;
import dev.minemods.sleepdebt.entity.SleeplessEntity;
import net.minecraft.client.model.ZombieModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.resources.ResourceLocation;
public final class SleeplessRenderer extends HumanoidMobRenderer<SleeplessEntity,ZombieModel<SleeplessEntity>> {
 private static final ResourceLocation TEXTURE=ResourceLocation.fromNamespaceAndPath(SleepDebtMod.MOD_ID,"textures/entity/sleepless.png");
 public SleeplessRenderer(EntityRendererProvider.Context ctx){super(ctx,new ZombieModel<>(ctx.bakeLayer(ModelLayers.ZOMBIE)),.5f);}
 @Override public ResourceLocation getTextureLocation(SleeplessEntity e){return TEXTURE;}
}

