package com.kirill.lurelantern.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;

/** Uses the same Blockbench cubes and UV atlas as the placed Advanced Lantern.
 * Held glass uses the item translucent pipeline so the world and captive remain visible.
 */
public final class AdvancedLanternHandMesh {
    private static final ResourceLocation METAL = tex("advanced_lure_lantern");
    private static final ResourceLocation METAL_ACTIVE = tex("advanced_lure_lantern_active");
    private static final ResourceLocation GLASS = tex("advanced_lure_lantern_glass");
    private static final ResourceLocation GLASS_ACTIVE = tex("advanced_lure_lantern_glass_active");
    private static final Cube[] FRAME = {
        new Cube(4f,0f,4f,12f,2f,12f,new float[][]{{0f,9f,8f,11f},{0f,9f,8f,11f},{0f,9f,8f,11f},{0f,9f,8f,11f},{0f,9f,8f,11f},{0f,9f,8f,11f}}),
        new Cube(4f,8f,4f,12f,10f,12f,new float[][]{{0f,0f,8f,2f},{0f,0f,8f,2f},{0f,0f,8f,2f},{0f,0f,8f,2f},{0f,0f,8f,2f},{0f,0f,8f,2f}}),
        new Cube(4f,2f,4f,5f,8f,5f,new float[][]{{12f,1f,13f,8f},{12f,1f,13f,8f},{12f,1f,13f,8f},{12f,1f,13f,8f},{12f,1f,13f,8f},{12f,1f,13f,8f}}),
        new Cube(4f,2f,11f,5f,8f,12f,new float[][]{{12f,1f,13f,8f},{12f,1f,13f,8f},{12f,1f,13f,8f},{12f,1f,13f,8f},{12f,1f,13f,8f},{12f,1f,13f,8f}}),
        new Cube(11f,2f,4f,12f,8f,5f,new float[][]{{12f,1f,13f,8f},{12f,1f,13f,8f},{12f,1f,13f,8f},{12f,1f,13f,8f},{12f,1f,13f,8f},{12f,1f,13f,8f}}),
        new Cube(11f,2f,11f,12f,8f,12f,new float[][]{{12f,1f,13f,8f},{12f,1f,13f,8f},{12f,1f,13f,8f},{12f,1f,13f,8f},{12f,1f,13f,8f},{12f,1f,13f,8f}}),
        new Cube(6f,10f,6f,10f,11f,10f,new float[][]{{0f,10f,4f,11f},{0f,10f,4f,11f},{0f,10f,4f,11f},{0f,10f,4f,11f},{0f,10f,4f,11f},{0f,10f,4f,11f}}),
        new Cube(6.5f,11f,7f,7.5f,13f,9f,new float[][]{{10f,1f,11f,4f},{10f,1f,11f,4f},{10f,1f,11f,4f},{10f,1f,11f,4f},{10f,1f,11f,4f},{10f,1f,11f,4f}}),
        new Cube(8.5f,11f,7f,9.5f,13f,9f,new float[][]{{10f,1f,11f,4f},{10f,1f,11f,4f},{10f,1f,11f,4f},{10f,1f,11f,4f},{10f,1f,11f,4f},{10f,1f,11f,4f}}),
        new Cube(6.5f,13f,7f,9.5f,14f,9f,new float[][]{{0f,10f,4f,11f},{0f,10f,4f,11f},{0f,10f,4f,11f},{0f,10f,4f,11f},{0f,10f,4f,11f},{0f,10f,4f,11f}})
    };
    private static final Cube[] WINDOW = {
        new Cube(5f,2f,5f,11f,8f,11f,new float[][]{{5f,3f,11f,9f},{5f,3f,11f,9f},{5f,3f,11f,9f},{5f,3f,11f,9f},{5f,3f,11f,9f},{5f,3f,11f,9f}})
    };
    private record Cube(float x0,float y0,float z0,float x1,float y1,float z1,float[][] uv) {}
    private AdvancedLanternHandMesh() {}
    private static ResourceLocation tex(String name) {
        return ResourceLocation.fromNamespaceAndPath("lurelantern","textures/block/"+name+".png");
    }
    public static void renderFrame(boolean active, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        draw(FRAME,buffers.getBuffer(RenderType.entitySolid(active ? METAL_ACTIVE : METAL)),pose.last().pose(),light,overlay,false);
    }
    public static void renderGlass(boolean active, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        draw(WINDOW,buffers.getBuffer(RenderType.itemEntityTranslucentCull(active ? GLASS_ACTIVE : GLASS)),pose.last().pose(),light,overlay,true);
    }
    private static void draw(Cube[] cubes, VertexConsumer out, Matrix4f mat, int light, int overlay, boolean glassOnlySides) {
        for(Cube c:cubes) {
            float x0=c.x0()/16f,y0=c.y0()/16f,z0=c.z0()/16f,x1=c.x1()/16f,y1=c.y1()/16f,z1=c.z1()/16f;
            for(int side=0;side<(glassOnlySides ? 4 : 6);side++) {
                float[] uv=c.uv()[side];
                float u0=uv[0]/16f,v0=uv[1]/16f,u1=uv[2]/16f,v1=uv[3]/16f;
                switch(side) {
                    case 0 -> quad(out,mat,x0,y0,z0,x0,y1,z0,x1,y1,z0,x1,y0,z0,u0,v0,u1,v1,0,0,-1,light,overlay);
                    case 1 -> quad(out,mat,x1,y0,z1,x1,y1,z1,x0,y1,z1,x0,y0,z1,u0,v0,u1,v1,0,0,1,light,overlay);
                    case 2 -> quad(out,mat,x1,y0,z0,x1,y1,z0,x1,y1,z1,x1,y0,z1,u0,v0,u1,v1,1,0,0,light,overlay);
                    case 3 -> quad(out,mat,x0,y0,z1,x0,y1,z1,x0,y1,z0,x0,y0,z0,u0,v0,u1,v1,-1,0,0,light,overlay);
                    case 4 -> quad(out,mat,x0,y1,z0,x0,y1,z1,x1,y1,z1,x1,y1,z0,u0,v0,u1,v1,0,1,0,light,overlay);
                    default -> quad(out,mat,x0,y0,z1,x0,y0,z0,x1,y0,z0,x1,y0,z1,u0,v0,u1,v1,0,-1,0,light,overlay);
                }
            }
        }
    }
    private static void quad(VertexConsumer out, Matrix4f mat,
            float x0,float y0,float z0,float x1,float y1,float z1,
            float x2,float y2,float z2,float x3,float y3,float z3,
            float u0,float v0,float u1,float v1,
            float nx,float ny,float nz,int light,int overlay) {
        vertex(out,mat,x0,y0,z0,u0,v1,nx,ny,nz,light,overlay);
        vertex(out,mat,x1,y1,z1,u0,v0,nx,ny,nz,light,overlay);
        vertex(out,mat,x2,y2,z2,u1,v0,nx,ny,nz,light,overlay);
        vertex(out,mat,x3,y3,z3,u1,v1,nx,ny,nz,light,overlay);
    }
    private static void vertex(VertexConsumer out, Matrix4f mat, float x,float y,float z,
            float u,float v,float nx,float ny,float nz,int light,int overlay) {
        out.addVertex(mat,x,y,z).setColor(255,255,255,255).setUv(u,v)
                .setOverlay(overlay).setLight(light).setNormal(nx,ny,nz);
    }
}
