package dev.minemods.halohaven.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.minemods.halohaven.HaloHaven;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.entity.LivingEntity;

/**
 * Fully three-dimensional, animated luminous nimbus.  It is emitted from the
 * entity renderer, never into the world mesh or from the dedicated server.
 * Two-sided fan of textured ring segments with fixed fullbright lighting.
 */
public final class CelestialHalo {
    private CelestialHalo(){}

    public static void draw(LivingEntity living,float partial,PoseStack pose,
                            MultiBufferSource buffer,float radius,float height,
                            int red,int green,int blue) {
        float time=living.tickCount+partial;
        float y=height+0.065f*(float)Math.sin(time*0.11f);
        float rotate=time*0.018f;
        float inner=radius*0.72f;
        int alpha=190;
        VertexConsumer vertex=buffer.getBuffer(RenderType.entityTranslucent(
            HaloHaven.id("textures/entity/halo_ring.png")));
        pose.pushPose();
        for(int i=0;i<32;i++){
            double a0=(Math.PI*2.0*i/32.0)+rotate;
            double a1=(Math.PI*2.0*(i+1)/32.0)+rotate;
            float x0=(float)Math.cos(a0),z0=(float)Math.sin(a0);
            float x1=(float)Math.cos(a1),z1=(float)Math.sin(a1);
            emit(vertex,pose,x0*radius,y,z0*radius,0,0,red,green,blue,alpha);
            emit(vertex,pose,x0*inner,y,z0*inner,0,1,red,green,blue,alpha);
            emit(vertex,pose,x1*inner,y,z1*inner,1,1,red,green,blue,alpha);
            emit(vertex,pose,x1*radius,y,z1*radius,1,0,red,green,blue,alpha);
            // underside, so the nimbus remains visible from low camera angles
            emit(vertex,pose,x1*radius,y,z1*radius,0,0,red,green,blue,alpha);
            emit(vertex,pose,x1*inner,y,z1*inner,0,1,red,green,blue,alpha);
            emit(vertex,pose,x0*inner,y,z0*inner,1,1,red,green,blue,alpha);
            emit(vertex,pose,x0*radius,y,z0*radius,1,0,red,green,blue,alpha);
        }
        pose.popPose();
    }

    private static void emit(VertexConsumer out,PoseStack pose,float x,float y,float z,
                             float u,float v,int r,int g,int b,int alpha){
        out.addVertex(pose.last().pose(),x,y,z)
            .setColor(r,g,b,alpha)
            .setUv(u,v)
            .setOverlay(OverlayTexture.NO_OVERLAY)
            .setLight(0x00F000F0)
            .setNormal(pose.last(),0,1,0);
    }
}
