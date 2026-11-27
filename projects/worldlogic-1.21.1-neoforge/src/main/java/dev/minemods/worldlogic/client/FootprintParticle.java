package dev.minemods.worldlogic.client;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;
import net.minecraft.core.particles.SimpleParticleType;
public final class FootprintParticle extends TextureSheetParticle {
 private final float yaw;
 private FootprintParticle(ClientLevel level,double x,double y,double z,double yaw,SpriteSet sprites){
  super(level,x,y,z);this.yaw=(float)yaw;this.xd=0;this.yd=0;this.zd=0;this.gravity=0;this.hasPhysics=false;this.lifetime=200;this.quadSize=.145f;this.alpha=.8f;pickSprite(sprites);
 }
 @Override public FacingCameraMode getFacingCameraMode(){return (q,c,t)->q.identity().rotateX(-(float)Math.PI/2).rotateZ(yaw);}
 @Override public ParticleRenderType getRenderType(){return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;}
 @Override public void tick(){if(age++>=lifetime){remove();return;}alpha=.8f*Math.min(1,(lifetime-age)/60f);}
 public static ParticleProvider<SimpleParticleType> provider(SpriteSet sprites){return (type,level,x,y,z,vx,vy,vz)->new FootprintParticle(level,x,y,z,vx,sprites);}
}
