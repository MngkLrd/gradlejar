package dev.minemods.traumacraft.client;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.minemods.traumacraft.TraumaCraft;
import dev.minemods.traumacraft.content.*;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
@EventBusSubscriber(modid=TraumaCraft.LOADER_MOD_ID,bus=EventBusSubscriber.Bus.MOD,value=Dist.CLIENT)
public final class HerbRenderer implements BlockEntityRenderer<HerbWorkpieceEntity> {
    private static final ResourceLocation RAW=ResourceLocation.withDefaultNamespace("textures/block/moss_block.png");
    private static final ResourceLocation PASTE=ResourceLocation.withDefaultNamespace("textures/block/green_terracotta.png");
    public HerbRenderer(BlockEntityRendererProvider.Context c){}
    @SubscribeEvent public static void register(EntityRenderersEvent.RegisterRenderers e){e.registerBlockEntityRenderer(HerbContent.ENTITY.get(),HerbRenderer::new);}
    @Override public void render(HerbWorkpieceEntity e,float delta,PoseStack pose,MultiBufferSource buffers,int light,int overlay){
        if(e.emptySurface())return;
        // A thin dark herbal mass below a two-pixel layer of raw foliage.
        // Crushing removes the upper cell; the remaining material is visibly lower.
        VertexConsumer base=buffers.getBuffer(RenderType.entityCutoutNoCull(PASTE));
        quad(base,pose,0,0,1,1,1/16f+.0005f,light,overlay);
        VertexConsumer b=buffers.getBuffer(RenderType.entityCutoutNoCull(RAW));
        for(int cell=0;cell<16;cell++){
            if(HerbMask.done(e.mask(),cell))continue;
            int x=cell%4,z=cell/4;
            quad(b,pose,x/4f,z/4f,(x+1)/4f,(z+1)/4f,2/16f+.0005f,light,overlay);
        }
    }
    private static void quad(VertexConsumer b,PoseStack p,float x0,float z0,float x1,float z1,float y,int light,int overlay){
        vertex(b,p,x0,y,z0,x0,z0,light,overlay);vertex(b,p,x0,y,z1,x0,z1,light,overlay);
        vertex(b,p,x1,y,z1,x1,z1,light,overlay);vertex(b,p,x1,y,z0,x1,z0,light,overlay);
    }
    private static void vertex(VertexConsumer b,PoseStack p,float x,float y,float z,float u,float v,int light,int overlay){
        b.addVertex(p.last().pose(),x,y,z).setColor(255,255,255,255).setUv(u,v).setOverlay(overlay).setLight(light).setNormal(p.last(),0,1,0);
    }
}
