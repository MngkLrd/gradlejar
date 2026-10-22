package dev.minemods.sleepdebt.client;

import com.mojang.blaze3d.vertex.PoseStack;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.joml.Vector3f;
import com.mojang.math.Axis;

/**
 * Per-client world-space hallucinations. No fake network players or entity IDs:
 * only a baked vanilla Steve/Alex model rendered directly into the world scene.
 * No colliders, damage, hitboxes, persistent world state or multiplayer traffic.
 */
public final class MiragePlayers {
 private static final ResourceLocation STEVE =
   ResourceLocation.withDefaultNamespace("textures/entity/player/wide/steve.png");
 private static final ResourceLocation ALEX =
   ResourceLocation.withDefaultNamespace("textures/entity/player/slim/alex.png");
 private static final List<PhantomPlayer> MIRAGES = new ArrayList<>();
 private static PlayerModel<LivingEntity> wide;
 private static PlayerModel<LivingEntity> slim;
 private static Object lastLevel;
 private static long nextRoll;
 private static long lastSound;
 private record PhantomPlayer(Vec3 position,float facing,long born,int lifetime,boolean slim) {}

 public static void modelBake(EntityRenderersEvent.AddLayers e){
  wide=new PlayerModel<>(e.getEntityModels().bakeLayer(ModelLayers.PLAYER),false);
  slim=new PlayerModel<>(e.getEntityModels().bakeLayer(ModelLayers.PLAYER_SLIM),true);
 }

 public static void clientTick(ClientTickEvent.Post ignored){
  Minecraft mc=Minecraft.getInstance();
  if(lastLevel!=mc.level){
   MIRAGES.clear();lastLevel=mc.level;nextRoll=0;lastSound=0;
  }
  if(mc.level==null||mc.player==null||mc.player.isSpectator()||mc.screen!=null
    ||!ClientSettings.HALLUCINATIONS.get()||!ClientSettings.MIRAGE_PLAYERS.get()
    ||ClientState.maskTicks>0||ClientState.debt<650){
   MIRAGES.clear();return;
  }
  long time=mc.level.getGameTime();
  Vec3 eye=mc.player.getEyePosition();
  Vector3f sight=mc.player.getLookAngle().toVector3f().normalize();
  Iterator<PhantomPlayer> iterator=MIRAGES.iterator();
  while(iterator.hasNext()){
   PhantomPlayer p=iterator.next();
   Vec3 to=p.position().add(0,1.5,0).subtract(eye);
   double length=to.length();
   double dot=length<.01?1:to.x*sight.x()/length+to.y*sight.y()/length+to.z*sight.z()/length;
   if(time-p.born()>p.lifetime() || length<7 || (time-p.born()>24 && dot>.9985 && length<27)){
    iterator.remove();
   }
  }
  if(time<nextRoll || !MIRAGES.isEmpty())return;
  nextRoll=time+160+mc.level.random.nextInt(180);
  if(mc.level.isDay()&&ClientState.stress<800)return;
  if(mc.level.random.nextInt(100)>Math.min(90,18+(ClientState.debt-650)/5+ClientState.stress/20))return;
  int count=2+mc.level.random.nextInt(3+(ClientState.debt>=900?2:0));
  count=Math.min(ClientSettings.MIRAGE_MAX.get(),count);
  Vec3 look=mc.player.getLookAngle();
  double first=Math.atan2(look.z,look.x);
  for(int i=0;i<count;i++){
   double angle=first+(i-(count-1)*.5)*.39+mc.level.random.nextGaussian()*.12;
   double range=19+mc.level.random.nextDouble()*14;
   int x=Mth.floor(mc.player.getX()+Math.cos(angle)*range);
   int z=Mth.floor(mc.player.getZ()+Math.sin(angle)*range);
   // Only use loaded chunks; never generate or demand chunks for a visual effect.
   BlockPos check=new BlockPos(x,mc.player.getBlockY(),z);
   if(!mc.level.hasChunkAt(check))continue;
   int y=mc.level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,x,z);
   BlockPos floor=new BlockPos(x,y,z);
   if(!mc.level.getBlockState(floor).isAir()
      ||!mc.level.getBlockState(floor.above()).isAir())continue;
   if(Math.abs(y-mc.player.getY())>9)continue;
   double dx=mc.player.getX()-x,dz=mc.player.getZ()-z;
   float yaw=(float)(Math.atan2(dz,dx)*180/Math.PI)-90f;
   MIRAGES.add(new PhantomPlayer(new Vec3(x+.5,y,z+.5),yaw,time,
     65+mc.level.random.nextInt(100),mc.level.random.nextBoolean()));
  }
  if(!MIRAGES.isEmpty()&&time-lastSound>150 && mc.level.random.nextInt(3)==0){
   lastSound=time;
   PhantomPlayer sound=MIRAGES.getFirst();
   mc.level.playLocalSound(sound.position.x,sound.position.y,sound.position.z,
      SoundEvents.GRASS_STEP,SoundSource.AMBIENT,.25f,.7f+mc.level.random.nextFloat()*.3f,false);
  }
 }

 public static void renderWorld(RenderLevelStageEvent e){
  if(e.getStage()!=RenderLevelStageEvent.Stage.AFTER_PARTICLES || MIRAGES.isEmpty()
      ||wide==null||slim==null)return;
  Minecraft mc=Minecraft.getInstance();
  if(mc.level==null||mc.player==null||mc.screen!=null)return;
  PoseStack pose=e.getPoseStack();
  MultiBufferSource.BufferSource buffers=mc.renderBuffers().bufferSource();
  Vec3 camera=e.getCamera().getPosition();
  long t=mc.level.getGameTime();
  for(PhantomPlayer ghost:MIRAGES){
   if(!mc.level.hasChunkAt(BlockPos.containing(ghost.position())))continue;
   float age=(t-ghost.born())/(float)ghost.lifetime();
   float fade=Math.min(1f,Math.min(age*6f,(1-age)*5f));
   if(fade<=0f)continue;
   Vec3 pos=ghost.position().subtract(camera);
   pose.pushPose();
   pose.translate(pos.x,pos.y,pos.z);
   pose.mulPose(Axis.YP.rotationDegrees(180-ghost.facing()));
   pose.scale(-1f,-1f,1f);
   pose.translate(0,-1.501,0);
   PlayerModel<LivingEntity> m=ghost.slim()?slim:wide;
   m.setAllVisible(true);
   m.hat.visible=true;
   m.rightArm.xRot=-.08f;
   m.leftArm.xRot=.06f;
   int alpha=Math.min(255,Math.max(1,(int)((ClientState.stress>=700?.78:.66)*fade*255)));
   int tint=(alpha<<24)|0xD8D8DF;
   int light=net.minecraft.client.renderer.LightTexture.pack(
     mc.level.getBrightness(net.minecraft.world.level.LightLayer.BLOCK,BlockPos.containing(ghost.position())),
     mc.level.getBrightness(net.minecraft.world.level.LightLayer.SKY,BlockPos.containing(ghost.position())));
   m.renderToBuffer(pose,buffers.getBuffer(RenderType.entityTranslucent(ghost.slim()?ALEX:STEVE)),
     light,OverlayTexture.NO_OVERLAY,tint);
   pose.popPose();
  }
  buffers.endBatch();
 }
 private MiragePlayers(){}
}

