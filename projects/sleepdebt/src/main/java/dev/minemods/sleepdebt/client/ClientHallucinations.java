package dev.minemods.sleepdebt.client;

import net.minecraft.client.Minecraft;
import dev.minemods.sleepdebt.entity.SleeplessEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.particles.ParticleTypes;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/** Purely client-side fatigue feedback; all silhouettes are non-interactive. */
public final class ClientHallucinations {
 private static float presence;
 private static int lastScan=-1;
 private static Object levelId;
 private static float closest=25;
 /**
  * Screen-space smoke vignette. Tied to REAL nearby Sleepless entities, not
  * only the numerical sleep debt. No shaders, postprocessors or mixins.
  */
 private static void renderApproach(GuiGraphics g,Minecraft mc){
  if(!ClientSettings.SLEEPLESS_AURA.get())return;
  if(levelId!=mc.level){levelId=mc.level;presence=0;lastScan=-1;}
  if(lastScan<0||mc.player.tickCount-lastScan>=5){
   lastScan=mc.player.tickCount;
   closest=25;
   for(SleeplessEntity mob:mc.level.getEntitiesOfClass(SleeplessEntity.class,
       mc.player.getBoundingBox().inflate(23))){
    if(mob.isAlive()){
     closest=Math.min(closest,(float)mc.player.distanceTo(mob));
    }
   }
  }
  float target=Math.max(0,Math.min(1,(19-closest)/18f));
  presence+=(target-presence)*.18f;
  if(presence<.015f)return;
  int w=g.guiWidth(),h=g.guiHeight();
  float beat=.80f+.20f*(float)Math.sin(mc.player.tickCount*.13);
  int density=Math.max(0,Math.min(220,(int)(presence*presence*218*beat)));
  int edge=Math.min(Math.min(w,h)/3,Math.max(8,(int)(24+presence*115)));
  for(int i=0;i<6;i++){
   int size=Math.max(1,edge*(6-i)/6);
   int alpha=Math.max(1,density*(6-i)/15);
   int color=(alpha<<24)|0x0E0B12;
   g.fill(0,0,size,h,color);g.fill(w-size,0,w,h,color);
   g.fill(0,0,w,size/2,color);g.fill(0,h-size/2,w,h,color);
  }
  if(presence>.58f){
   int shade=Math.min(105,(int)((presence-.58f)*220));
   int cloud=(shade<<24)|0x101016;
   int drift=(mc.player.tickCount/5)%24;
   int band=Math.max(6,(int)(presence*35));
   g.fill(0,h/4+drift,band,h/4+drift+5,cloud);
   g.fill(w-band,h*2/3-drift,w,h*2/3-drift+8,cloud);
   g.fill(w/7,0,w/7+21,Math.max(1,(int)(presence*13)),cloud);
   g.fill(w*4/5,h-Math.max(1,(int)(presence*16)),w*4/5+30,h,cloud);
  }
  if(presence>.74f){
   int shade=Math.min(86,(int)((presence-.74f)*330));
   g.fill(0,0,w,h,(shade<<24)|0x100E17);
  }
  // Short dark smoke streaks around the periphery; no random screen noise.
  if(presence>.38f){
   int alpha=Math.min(95,(int)(presence*65));
   int mist=(alpha<<24)|0x26232A;
   long t=mc.player.tickCount/5;
   int y1=(int)(h*.28)+(int)(t%17);
   int y2=(int)(h*.71)-(int)(t%13);
   g.fill(0,y1,(int)(presence*28),y1+3,mist);
   g.fill(w-(int)(presence*38),y2,w,y2+2,mist);
  }
 }

 public static void tick(ClientTickEvent.Post event){
  Minecraft mc=Minecraft.getInstance();
  if(!ClientSettings.HALLUCINATIONS.get()||mc.level==null||mc.player==null||mc.screen!=null)return;
  if(ClientState.debt<750||ClientState.maskTicks>0)return;
  if(mc.level.getMaxLocalRawBrightness(mc.player.blockPosition())>9)return;
  if(mc.player.tickCount%90!=0 || mc.level.random.nextInt(4)!=0)return;
  double angle=mc.player.getYRot()*Math.PI/180d;
  double side=mc.level.random.nextBoolean()?1:-1;
  double xx=mc.player.getX()+Math.cos(angle)*7+Math.cos(angle+Math.PI/2)*side*3;
  double zz=mc.player.getZ()+Math.sin(angle)*7+Math.sin(angle+Math.PI/2)*side*3;
  double yy=mc.player.getY()+mc.level.random.nextDouble();
  for(int i=0;i<4;i++)
   mc.level.addParticle(ParticleTypes.SOUL,xx+mc.level.random.nextGaussian()*.22,
    yy+i*.33,zz+mc.level.random.nextGaussian()*.22,0,.01,0);
 }
 public static void render(GuiGraphics g){
  Minecraft mc=Minecraft.getInstance();
  if(mc.player==null||mc.level==null||mc.options.hideGui||mc.screen!=null)return;
  renderApproach(g,mc);
  if(!ClientSettings.HALLUCINATIONS.get())return;
  if(ClientState.debt<620||ClientState.maskTicks>0||ClientSettings.MIRAGE_PLAYERS.get())return;
  long t=mc.level.getGameTime(),phase=t/190;
  long seed=(phase*37291L+mc.player.getUUID().getMostSignificantBits())&0x7fffffff;
  if(seed%7!=0||t%190>26)return;
  int w=g.guiWidth(),h=g.guiHeight();
  int x=(seed%2==0)?5:w-31;
  int y=(int)(h*.30)+(int)((seed/11)%60);
  int alpha=ClientState.stress>600?0x8C:0x54;
  int body=(alpha<<24)|0x292632,eyes=(alpha<<24)|0xBA8B9A;
  // Small angular human-like silhouette. Avoid flashy overlays or screen blur.
  g.fill(x+9,y+3,x+19,y+15,body);
  g.fill(x+11,y+1,x+17,y+4,body);
  g.fill(x+12,y+9,x+14,y+11,eyes);
  g.fill(x+17,y+9,x+19,y+11,eyes);
  g.fill(x+8,y+15,x+21,y+27,body);
  g.fill(x+5,y+17,x+8,y+29,body);
  g.fill(x+21,y+17,x+24,y+29,body);
  g.fill(x+9,y+27,x+13,y+36,body);
  g.fill(x+17,y+27,x+21,y+36,body);
  if(ClientState.debt>830) {
   g.fill(0,0,w,3,0x1A1B1621);
   g.fill(0,h-3,w,h,0x1A1B1621);
  }
 }
 private ClientHallucinations(){}
}


