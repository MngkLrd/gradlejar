package dev.minemods.leashwrap.client;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.texture.OverlayTexture;

/** Renders two textured rope turns and a knot on the outside of a full cube. */
public final class RopeGeometry {
    private RopeGeometry() {}
    public static void draw(PoseStack.Pose pose, VertexConsumer out, int light) {
        band(pose,out,light,-0.125f,-0.070f);
        band(pose,out,light, 0.030f, 0.085f);
        strip(pose,out,light,-0.11f,-0.18f,-0.533f,0.11f,0.15f,-0.533f,0.046f);
        strip(pose,out,light,0.11f,-0.18f,-0.537f,-0.11f,0.15f,-0.537f,0.046f);
    }
    private static void band(PoseStack.Pose p, VertexConsumer b, int l, float bottom, float top) {
        float r=0.516f;
        quad(p,b,l,-r,bottom,-r,r,bottom,-r,r,top,-r,-r,top,-r,0,0,-1);
        quad(p,b,l,r,bottom,r,-r,bottom,r,-r,top,r,r,top,r,0,0,1);
        quad(p,b,l,-r,bottom,r,-r,bottom,-r,-r,top,-r,-r,top,r,-1,0,0);
        quad(p,b,l,r,bottom,-r,r,bottom,r,r,top,r,r,top,-r,1,0,0);
    }
    private static void strip(PoseStack.Pose p, VertexConsumer b, int l,
                              float ax,float ay,float az,float bx,float by,float bz,float width) {
        float dx=bx-ax,dy=by-ay;
        float factor=width/((float)Math.sqrt(dx*dx+dy*dy))*0.5f;
        float ox=-dy*factor,oy=dx*factor;
        quad(p,b,l,ax-ox,ay-oy,az,bx-ox,by-oy,bz,bx+ox,by+oy,bz,ax+ox,ay+oy,az,0,0,-1);
    }
    private static void quad(PoseStack.Pose p,VertexConsumer b,int l,
                             float ax,float ay,float az,float bx,float by,float bz,
                             float cx,float cy,float cz,float dx,float dy,float dz,
                             float nx,float ny,float nz) {
        point(p,b,l,ax,ay,az,0,1,nx,ny,nz);
        point(p,b,l,bx,by,bz,1,1,nx,ny,nz);
        point(p,b,l,cx,cy,cz,1,0,nx,ny,nz);
        point(p,b,l,dx,dy,dz,0,0,nx,ny,nz);
    }
    private static void point(PoseStack.Pose p,VertexConsumer b,int l,
                             float x,float y,float z,float u,float v,
                             float nx,float ny,float nz) {
        b.addVertex(p,x,y,z).setColor(255,255,255,255).setUv(u,v)
                .setOverlay(OverlayTexture.NO_OVERLAY).setLight(l).setNormal(p,nx,ny,nz);
    }
}
