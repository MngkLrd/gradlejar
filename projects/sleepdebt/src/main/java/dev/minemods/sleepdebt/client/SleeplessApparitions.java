package dev.minemods.sleepdebt.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.minemods.sleepdebt.SleepDebtMod;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

/**
 * Non-hostile visions of the actual Sleepless, not fake server entities.
 * They appear behind and to the sides at 19-30 blocks for 5-10 seconds.
 * The player must stare at them for a fraction of a second before they fade.
 * No hitbox, target, loot, networking or persistent data.
 */
public final class SleeplessApparitions {
 private static final ResourceLocation SKIN=ResourceLocation.fromNamespaceAndPath(
  SleepDebtMod.MOD_ID,"textures/entity/sleepless.png");
 private static final List<Apparition> VISIONS=new ArrayList<>();
 private static SleeplessModel model;
 private static Object lastLevel,lastPlayer;
 private static long nextTry,lastFootstep;
 private static final class Apparition{
  final Vec3 at;final float facing;final long born;final int lifetime;
  int stareTicks;
  Apparition(Vec3 at,float facing,long born,int lifetime){
   this.at=at;this.facing=facing;this.born=born;this.lifetime=lifetime;
  }
 }
 public static void bake(EntityRenderersEvent.AddLayers e){
  model=new SleeplessModel(e.getEntityModels().bakeLayer(SleeplessModel.LAYER));
 }
 public static void tick(ClientTickEvent.Post event){
  Minecraft mc=Minecraft.getInstance();
  if(lastLevel!=mc.level||lastPlayer!=mc.player){
   VISIONS.clear();lastLevel=mc.level;lastPlayer=mc.player;
   nextTry=0;lastFootstep=0;
  }
  if(mc.level==null||mc.player==null||mc.player.isSpectator()||
     mc.player.isSleeping()||mc.screen!=null||
     !ClientSettings.HALLUCINATIONS.get()||
     !ClientSettings.SLEEPLESS_VISIONS.get()||
     ClientState.maskTicks>0||
     ClientState.debt<ClientSettings.SLEEPLESS_VISION_DEBT.get()){
   VISIONS.clear();return;
  }
  long tick=mc.player.tickCount;
  Vec3 eye=mc.player.getEyePosition();
  Iterator<Apparition> it=VISIONS.iterator();
  while(it.hasNext()){
   Apparition ghost=it.next();
   Vec3 to=ghost.at.add(0,1.45,0).subtract(eye);
   double dist=to.length();
   double gaze=dist>0.01?mc.player.getLookAngle().dot(to.scale(1/dist)):0;
   if(tick-ghost.born>40&&dist<38&&gaze>.985)ghost.stareTicks++;
   else ghost.stareTicks=Math.max(0,ghost.stareTicks-1);
   if(tick-ghost.born>ghost.lifetime||dist<5||ghost.stareTicks>=11)it.remove();
  }
  if(!VISIONS.isEmpty()){
   for(Apparition ghost:VISIONS){
    if((tick-ghost.born)%7==0){
     mc.level.addParticle(ParticleTypes.LARGE_SMOKE,
       ghost.at.x+(mc.level.random.nextDouble()-.5)*1.2,
       ghost.at.y+mc.level.random.nextDouble()*1.7,
       ghost.at.z+(mc.level.random.nextDouble()-.5)*1.2,0,.008,0);
    }
    if(tick-lastFootstep>42&&mc.level.random.nextInt(4)==0){
     lastFootstep=tick;
     mc.level.playLocalSound(ghost.at.x,ghost.at.y,ghost.at.z,
       SoundEvents.SOUL_SAND_STEP,SoundSource.AMBIENT,.56f,.72f,false);
    }
   }
   return;
  }
  if(tick<nextTry)return;
  int debt=ClientState.debt,stress=ClientState.stress;
  nextTry=tick+(debt>=900?135:debt>=750?210:315)+mc.level.random.nextInt(110);
  if(mc.level.isDay()&&stress<800)return;
  int chance=Math.min(90,35+(debt-550)/10+stress/25);
  if(mc.level.random.nextInt(100)>=chance)return;
  Vec3 look=mc.player.getLookAngle();
  for(int attempt=0;attempt<10;attempt++){
   // Mostly behind; occasionally flank to the player's left or right.
   double offset=(attempt<7?Math.PI:Math.PI*.55) +
     (mc.level.random.nextDouble()-.5)*1.55*(mc.level.random.nextBoolean()?1:-1);
   double angle=Math.atan2(look.z,look.x)+offset;
   int radius=19+mc.level.random.nextInt(12);
   int x=Mth.floor(mc.player.getX()+Math.cos(angle)*radius);
   int z=Mth.floor(mc.player.getZ()+Math.sin(angle)*radius);
   BlockPos check=new BlockPos(x,mc.player.getBlockY(),z);
   if(!mc.level.hasChunkAt(check))continue;
   int y=mc.level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,x,z);
   BlockPos spawn=new BlockPos(x,y,z);
   if(!mc.level.getBlockState(spawn).isAir()||
      !mc.level.getBlockState(spawn.above()).isAir()||
      Math.abs(y-mc.player.getY())>7)continue;
   double dx=mc.player.getX()-x,dz=mc.player.getZ()-z;
   float yaw=(float)(Math.atan2(dz,dx)*180/Math.PI)-90f;
   VISIONS.add(new Apparition(new Vec3(x+.5,y,z+.5),yaw,tick,
     120+mc.level.random.nextInt(85)));
   break;
  }
 }
 public static void render(RenderLevelStageEvent event){
  if(event.getStage()!=RenderLevelStageEvent.Stage.AFTER_PARTICLES||
     VISIONS.isEmpty()||model==null)return;
  Minecraft mc=Minecraft.getInstance();
  if(mc.level==null||mc.player==null||mc.screen!=null)return;
  PoseStack pose=event.getPoseStack();
  Vec3 camera=event.getCamera().getPosition();
  MultiBufferSource.BufferSource buffers=mc.renderBuffers().bufferSource();
  long tick=mc.player.tickCount;
  for(Apparition g:VISIONS){
   if(!mc.level.hasChunkAt(BlockPos.containing(g.at)))continue;
   float age=(tick-g.born)/(float)g.lifetime;
   float fade=Math.max(0,Math.min(1,Math.min(age*6,(1-age)*6)));
   if(g.stareTicks>0)fade*=Math.max(0,1f-g.stareTicks/11f);
   if(fade<=.01)continue;
   Vec3 at=g.at.subtract(camera);
   pose.pushPose();
   pose.translate(at.x,at.y,at.z);
   pose.mulPose(Axis.YP.rotationDegrees(180-g.facing));
   pose.scale(-1f,-1f,1f);
   pose.translate(0,-1.501,0);
   model.setAllVisible(true);
   int alpha=Mth.clamp((int)(fade*195),1,225);
   int color=(alpha<<24)|0xB5B7BC;
   model.renderToBuffer(pose,buffers.getBuffer(RenderType.entityTranslucent(SKIN)),
       net.minecraft.client.renderer.LightTexture.pack(11,11),
       OverlayTexture.NO_OVERLAY,color);
   pose.popPose();
  }
  buffers.endBatch();
 }
 private SleeplessApparitions(){}
}