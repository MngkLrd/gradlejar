package dev.minemods.openmotion.client;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.minemods.openmotion.OpenMotion;
import dev.minemods.openmotion.block.HorizontalMachineBlock;
import dev.minemods.openmotion.blockentity.FanRotorBlockEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;
import com.mojang.math.Axis;
/** Animated client-side rotor, without server-side blockstate updates. */
public final class FanRotorRenderer implements BlockEntityRenderer<FanRotorBlockEntity> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(OpenMotion.MOD_ID,"textures/block/v15_air_blade.png");
    public FanRotorRenderer(BlockEntityRendererProvider.Context ctx) {}
    @Override
    public void render(FanRotorBlockEntity be, float partialTick, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        if (be.getLevel() == null) return;
        BlockState state = be.getBlockState();
        boolean powered = state.getValue(HorizontalMachineBlock.POWERED);
        float angle = powered ? ((be.getLevel().getGameTime() + partialTick) * 18f) % 360f : 0f;
        int yaw = switch(state.getValue(HorizontalMachineBlock.FACING)) {
            case SOUTH -> 180; case EAST -> 90; case WEST -> 270; default -> 0;
        };
        pose.pushPose();
        pose.translate(.5,.5,.5);
        pose.mulPose(Axis.YP.rotationDegrees(yaw));
        pose.translate(0,0,-.20);
        pose.mulPose(Axis.ZP.rotationDegrees(angle));
        VertexConsumer out = buffers.getBuffer(RenderType.entityCutoutNoCull(TEXTURE));
        for (int i=0; i<4; i++) {
            pose.pushPose(); pose.mulPose(Axis.ZP.rotationDegrees(i*90f));
            blade(out,pose.last(),light,overlay);pose.popPose();
        }
        rect(out,pose.last(),-.045f,-.045f,.043f,.045f,.045f,177,197,190,light,overlay);
        rect(out,pose.last(),-.035f,-.035f,.047f,.035f,.035f,89,120,126,light,overlay);
        pose.popPose();
    }
    private static void blade(VertexConsumer out,PoseStack.Pose p,int light,int overlay) {
        quad(out,p,-.037f,.020f,-.014f,.033f,.045f,-.014f,.099f,.127f,-.014f,-.018f,.151f,-.014f,113,190,182,light,overlay,-1);
        quad(out,p,-.018f,.151f,-.014f,.099f,.127f,-.014f,.065f,.238f,-.014f,-.091f,.198f,-.014f,69,159,152,light,overlay,-1);
        quad(out,p,-.018f,.151f,.009f,.099f,.127f,.009f,.033f,.045f,.009f,-.037f,.020f,.009f,61,118,124,light,overlay,1);
        quad(out,p,-.091f,.198f,.009f,.065f,.238f,.009f,.099f,.127f,.009f,-.018f,.151f,.009f,49,108,116,light,overlay,1);
    }
    private static void rect(VertexConsumer out,PoseStack.Pose p,float x0,float y0,float z,float x1,float y1,int r,int g,int b,int light,int overlay) {
        quad(out,p,x0,y0,z,x1,y0,z,x1,y1,z,x0,y1,z,r,g,b,light,overlay,-1);
    }
    private static void quad(VertexConsumer out,PoseStack.Pose p,
        float x0,float y0,float z0,float x1,float y1,float z1,float x2,float y2,float z2,float x3,float y3,float z3,
        int r,int g,int b,int light,int overlay,int nz) {
        pt(out,p,x0,y0,z0,0,0,r,g,b,light,overlay,nz);
        pt(out,p,x1,y1,z1,1,0,r,g,b,light,overlay,nz);
        pt(out,p,x2,y2,z2,1,1,r,g,b,light,overlay,nz);
        pt(out,p,x3,y3,z3,0,1,r,g,b,light,overlay,nz);
    }
    private static void pt(VertexConsumer out,PoseStack.Pose p,float x,float y,float z,float u,float v,
        int r,int g,int b,int light,int overlay,int nz) {
        out.addVertex(p,x,y,z).setColor(r,g,b,255).setUv(u,v).setOverlay(overlay).setLight(light).setNormal(p,0,0,nz);
    }
}
