package dev.minemods.sleepdebt.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.particles.ParticleTypes;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/** Purely client-side fatigue feedback; all silhouettes are non-interactive. */
public final class ClientHallucinations {
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
  if(!ClientSettings.HALLUCINATIONS.get()||mc.player==null||mc.level==null||mc.options.hideGui||mc.screen!=null)return;
  if(ClientState.debt<620||ClientState.maskTicks>0)return;
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

