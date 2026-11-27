package dev.minemods.thermal.client;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.minemods.thermal.ThermalMod;
import dev.minemods.thermal.content.*;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
@EventBusSubscriber(modid=ThermalMod.ID,bus=EventBusSubscriber.Bus.MOD,value=Dist.CLIENT)
public final class FiberRenderer implements BlockEntityRenderer<FiberWorkpieceEntity> {
    private static final ResourceLocation RAW=ResourceLocation.withDefaultNamespace("textures/block/moss_block.png");
    private static final ResourceLocation CLEAN=ResourceLocation.withDefaultNamespace("textures/block/hay_block_top.png");
    public FiberRenderer(BlockEntityRendererProvider.Context c){}
    @SubscribeEvent public static void register(EntityRenderersEvent.RegisterRenderers e){e.registerBlockEntityRenderer(ThermalContent.WORKPIECE_ENTITY.get(),FiberRenderer::new);}
    @Override public void render(FiberWorkpieceEntity e,float delta,PoseStack pose,MultiBufferSource buffers,int light,int overlay){
        for(int pass=0;pass<2;pass++){
            VertexConsumer b=buffers.getBuffer(RenderType.entityCutoutNoCull(pass==0?RAW:CLEAN));
            for(int cell=0;cell<64;cell++){
                boolean clean=FiberMask.clean(e.mask(),cell);if(clean!=(pass==1))continue;
                int x=cell%8,z=cell/8;float x0=x/8f,z0=z/8f,x1=(x+1)/8f,z1=(z+1)/8f;
                float y=1/16f+.0005f;
                vertex(b,pose,x0,y,z0,x0,z0,light,overlay);vertex(b,pose,x0,y,z1,x0,z1,light,overlay);
                vertex(b,pose,x1,y,z1,x1,z1,light,overlay);vertex(b,pose,x1,y,z0,x1,z0,light,overlay);
            }
        }
    }
    private static void vertex(VertexConsumer b,PoseStack p,float x,float y,float z,float u,float v,int light,int overlay){
        b.addVertex(p.last().pose(),x,y,z).setColor(255,255,255,255).setUv(u,v).setOverlay(overlay).setLight(light).setNormal(p.last(),0,1,0);
    }
}
