package dev.minemods.redstonevision.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.model.data.ModelData;

@EventBusSubscriber(modid="redstonevision",value=Dist.CLIENT)
public final class VisionWorldRenderer {
    private VisionWorldRenderer() {}
    @SubscribeEvent public static void render(RenderLevelStageEvent event) {
        if (event.getStage()!=RenderLevelStageEvent.Stage.AFTER_PARTICLES || !VisionState.active()) return;
        Minecraft mc=Minecraft.getInstance();
        if (mc.level==null || CircuitScanner.hits().isEmpty()) return;
        Vec3 camera=event.getCamera().getPosition();
        PoseStack pose=new PoseStack();
        pose.translate(-camera.x,-camera.y,-camera.z);
        MultiBufferSource.BufferSource source=mc.renderBuffers().bufferSource();
        for (CircuitScanner.Hit hit:CircuitScanner.hits()) {
            BlockPos p=hit.pos();
            BlockState state=hit.state();
            pose.pushPose();pose.translate(p.getX(),p.getY(),p.getZ());
            mc.getBlockRenderer().renderSingleBlock(state,pose,source,LightTexture.FULL_BRIGHT,
                OverlayTexture.NO_OVERLAY,ModelData.EMPTY,VisionRenderTypes.CIRCUITS);
            pose.popPose();
        }
        source.endBatch(VisionRenderTypes.CIRCUITS);
        VertexConsumer lines=source.getBuffer(VisionRenderTypes.OUTLINES);
        for (CircuitScanner.Hit hit:CircuitScanner.hits()) {
            BlockPos p=hit.pos();
            int power=CircuitScanner.signal(hit.state());
            float r=power>0?1F:0.25F;
            float g=power>0?0.10F+0.20F*power/15F:0.75F;
            float b=power>0?0.10F:1F;
            LevelRenderer.renderLineBox(pose,lines,
                p.getX()+0.02,p.getY()+0.02,p.getZ()+0.02,
                p.getX()+0.98,p.getY()+0.98,p.getZ()+0.98,
                r,g,b,0.94F);
        }
        source.endBatch(VisionRenderTypes.OUTLINES);
    }
}
