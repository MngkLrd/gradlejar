package dev.minemods.sleepdebt.client;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.minemods.sleepdebt.SleepDebtMod;
import dev.minemods.sleepdebt.entity.SleeplessEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.resources.ResourceLocation;

/**
 * Taller and anatomically wrong: a silhouette that occupies a different
 * visual vocabulary from standard Minecraft bipeds.
 */
public final class SleeplessRenderer extends HumanoidMobRenderer<SleeplessEntity,SleeplessModel>{
 private static final ResourceLocation SKIN=ResourceLocation.fromNamespaceAndPath(
  SleepDebtMod.MOD_ID,"textures/entity/sleepless.png");
 public SleeplessRenderer(EntityRendererProvider.Context ctx){
  super(ctx,new SleeplessModel(ctx.bakeLayer(SleeplessModel.LAYER)),.53f);
 }
 @Override protected void scale(SleeplessEntity mob,PoseStack pose,float partialTick){
  pose.scale(1.09f,1.32f,1.09f);
 }
 @Override public ResourceLocation getTextureLocation(SleeplessEntity mob){return SKIN;}
}
