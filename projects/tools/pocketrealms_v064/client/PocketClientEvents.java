package com.kirill.pocketrealms.client;

import com.kirill.pocketrealms.PocketRealms;
import com.kirill.pocketrealms.registry.ModEntities;
import com.kirill.pocketrealms.registry.ModBlockEntities;
import com.kirill.pocketrealms.world.ModDimensions;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;

@EventBusSubscriber(modid = PocketRealms.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class PocketClientEvents {
    private PocketClientEvents() {}
    private static final ResourceLocation VIGNETTE = ResourceLocation.fromNamespaceAndPath(
            PocketRealms.MOD_ID, "textures/gui/pocket_vignette.png");

    @SubscribeEvent
    public static void onRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.CORRUPTED_ZOMBIE.get(), CorruptedZombieRenderer::new);
        event.registerEntityRenderer(ModEntities.CORRUPTED_SKELETON.get(), CorruptedSkeletonRenderer::new);
        event.registerEntityRenderer(ModEntities.WALL_GRASPER.get(), WallGrasperRenderer::new);
        event.registerEntityRenderer(ModEntities.VOID_PURSUER.get(), VoidPursuerRenderer::new);
        event.registerEntityRenderer(ModEntities.VEIL_HOUND.get(), VeilHoundRenderer::new);
        event.registerEntityRenderer(ModEntities.VEIL_GHOST.get(), VeilGhostRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.DOOR_PORTAL.get(),PocketDoorPortalRenderer::new);
    }

    @SubscribeEvent
    public static void onLayers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(GraspingHandsModel.LAYER, GraspingHandsModel::createBodyLayer);
        event.registerLayerDefinition(VoidPursuerModel.LAYER, VoidPursuerModel::createBodyLayer);
        event.registerLayerDefinition(HorrorZombieModel.LAYER, HorrorZombieModel::createBodyLayer);
        event.registerLayerDefinition(HorrorSkeletonModel.LAYER, HorrorSkeletonModel::createBodyLayer);
    }

    @SubscribeEvent
    public static void onGuiLayers(RegisterGuiLayersEvent event) {
        event.registerBelowAll(ResourceLocation.fromNamespaceAndPath(PocketRealms.MOD_ID, "void_pulse"),
                (gui, delta) -> {
                    Minecraft mc = Minecraft.getInstance();
                    if (mc.player == null || mc.level == null || !mc.level.dimension().equals(ModDimensions.POCKET_DIMENSION)
                            || mc.options.hideGui) return;
                    int w = gui.guiWidth();
                    int h = gui.guiHeight();
                    double t=System.nanoTime()/1_000_000_000.0D;
                    float pulse=(float)(0.50D+0.125D*Math.sin(t*1.3D)+0.035D*Math.sin(t*3.1D));
                    RenderSystem.enableBlend();
                    RenderSystem.defaultBlendFunc();
                    RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, pulse);
                    gui.blit(VIGNETTE, 0, 0, w, h, 0, 0, 64, 64, 64, 64);
                    RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
                    RenderSystem.disableBlend();
                });
    }
}
