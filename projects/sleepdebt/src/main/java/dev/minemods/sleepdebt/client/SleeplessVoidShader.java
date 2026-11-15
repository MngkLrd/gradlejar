package dev.minemods.sleepdebt.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import java.io.IOException;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.client.DeltaTracker;
import net.neoforged.neoforge.client.event.RegisterShadersEvent;
import org.joml.Matrix4f;
import dev.minemods.sleepdebt.SleepDebtMod;

/**
 * Independent fragment-shader compositor. It renders OVER the finished world,
 * UNDER the HUD. Nothing is injected into vanilla fog or potion render paths.
 *
 * The effect is designed to be uncomfortable: at full intensity it retains
 * only a small distorted opening at screen center, NOT a navigable fog radius.
 *
 * Render state is restored even if the shader throws. A fallback dark overlay
 * keeps the horror mechanic functional on graphics drivers rejecting GLSL.
 */
public final class SleeplessVoidShader {
 private static volatile ShaderInstance active;
 private static final ResourceLocation ID=
    ResourceLocation.fromNamespaceAndPath(SleepDebtMod.MOD_ID,"sleepless_void");
 private static boolean shaderFailed;

 public static void register(RegisterShadersEvent event) {
  try {
   event.registerShader(new ShaderInstance(event.getResourceProvider(),ID,
     DefaultVertexFormat.POSITION_TEX), shader -> {
       active=shader;
       shaderFailed=false;
     });
  }catch(IOException exception){
   // A missing or rejected program must not crash world loading.
   // The separate alpha-compositor fallback remains available.
   active=null;
   shaderFailed=true;
   System.err.println("[Sleep Debt] Custom Void shader unavailable: "+exception);
  }
 }

 public static void render(GuiGraphics gui,DeltaTracker frame){
  Minecraft mc=Minecraft.getInstance();
  if(mc.level==null||mc.player==null||mc.player.isSpectator()||
     mc.player.isSleeping()||mc.screen!=null)return;
  float intensity=SleeplessDread.density();
  int w=gui.guiWidth(),h=gui.guiHeight();
  if(w<1||h<1)return;
  if(intensity<=.005f){
   diagnostics(gui,mc,intensity);
   return;
  }
  // CRITICAL: This non-vanilla alpha compositor ALWAYS runs, independent of
  // whether a GPU shader was registered, "successfully" linked, or silently
  // rendered zero pixels. Alpha 15 used the fallback only on shader errors.
  paintBase(gui,w,h,intensity,mc.player.tickCount, SleeplessDread.shock());
  ShaderInstance sh=active;
  if(sh==null || shaderFailed || !ClientSettings.VOID_GPU.get()){
   diagnostics(gui,mc,intensity);
   return;
  }
  try {
   gui.flush();
   if(sh.getUniform("Intensity")!=null)
      sh.getUniform("Intensity").set(Mth.clamp(intensity,0f,1f));
   if(sh.getUniform("Shock")!=null)
      sh.getUniform("Shock").set(SleeplessDread.shock());
   if(sh.getUniform("Time")!=null)
      sh.getUniform("Time").set((mc.player.tickCount+frame.getGameTimeDeltaPartialTick(true))*.05f);
   if(sh.getUniform("Resolution")!=null)
      sh.getUniform("Resolution").set((float)w,(float)h);
   RenderSystem.enableBlend();
   RenderSystem.defaultBlendFunc();
   RenderSystem.disableDepthTest();
   RenderSystem.depthMask(false);
   RenderSystem.setShader(() -> sh);
   RenderSystem.setShaderColor(1f,1f,1f,1f);
   Matrix4f matrix=gui.pose().last().pose();
   BufferBuilder b=Tesselator.getInstance().begin(VertexFormat.Mode.QUADS,
     DefaultVertexFormat.POSITION_TEX);
   b.addVertex(matrix,0,0,0).setUv(0f,0f);
   b.addVertex(matrix,0,h,0).setUv(0f,1f);
   b.addVertex(matrix,w,h,0).setUv(1f,1f);
   b.addVertex(matrix,w,0,0).setUv(1f,0f);
   BufferUploader.drawWithShader(b.buildOrThrow());
  }catch(RuntimeException err){
   shaderFailed=true;
   System.err.println("[Sleep Debt] Void GPU enhancement disabled; CPU compositor is still active: "+err);
  }finally{
   RenderSystem.depthMask(true);
   RenderSystem.enableDepthTest();
   RenderSystem.disableBlend();
  }
  diagnostics(gui,mc,intensity);
 }


 /**
  * Deterministic layered blackness. Uses only GuiGraphics alpha-composited
  * rectangles over the already rendered scene: no vanilla fog, effects,
  * scene lights, framebuffer capture or shaderpack dependency.
  *
  * 1) Always present black substrate, also in opt-in no-GPU mode.
  * 2) Organic billowing masses (low-frequency continuous waves).
  * 3) Peripheral collapse, irregular bands and a hit-triggered blackout.
  */
 private static void paintBase(GuiGraphics g,int w,int h,float intensity,
                              int tick,float strike) {
  float p=Mth.clamp(intensity,0f,1f);
  float t=tick*.055f;
  float wave=(float)(Math.sin(t*1.6)+.6*Math.sin(t*2.31+1.7));
  float pressure=Mth.clamp(p+.023f*wave*p,0f,1f);
  // The previous 220*p*p overlay was conditional and too weak at 10-20m.
  // At point-blank proximity the actual screen approaches 98% black.
  int alpha=Mth.clamp((int)(249*Math.pow(pressure,1.1)),0,249);
  g.fill(0,0,w,h,(alpha<<24)|0x010102);

  // Smother the horizon from every edge, but do not use Minecraft's fog.
  for(int i=9;i>=1;i--){
   float layer=i/9f;
   float jitter=(float)(.93+.07*Math.sin(t*.74+i*.81));
   int x=Math.max(0,(int)(w*.48*pressure*layer*jitter));
   int y=Math.max(0,(int)(h*.47*pressure*layer*jitter));
   int edgeAlpha=Mth.clamp((int)(18*pressure*(1.3-.3*layer)),0,31);
   int ink=(edgeAlpha<<24)|0x000000;
   if(x>0){g.fill(0,0,x,h,ink);g.fill(w-x,0,w,h,ink);}
   if(y>0){g.fill(0,0,w,y,ink);g.fill(0,h-y,w,h,ink);}
  }

  // Horizontal overlapping billows: coherent changing shape, never pixel noise.
  int rows=12,columns=16;
  int dx=(w+columns-1)/columns,dy=(h+rows-1)/rows;
  for(int j=0;j<rows;j++){
   float fy=(j+.5f)/rows;
   for(int i=0;i<columns;i++){
    float fx=(i+.5f)/columns;
    float edgex=Math.abs(fx-.5f)*2,edgey=Math.abs(fy-.5f)*2;
    float peripheral=.20f+.80f*Math.max(edgex,edgey);
    double motion=Math.sin(fx*9.8+fy*3.1+t*.42)
      +.65*Math.cos(fy*10.4-fx*5.8-t*.55);
    double folds=Math.sin(fx*15.7+Math.sin(fy*8.7+t*.34)*1.6-t*.71);
    float opacity=(float)(Math.max(0,motion*.37+folds*.3+.23)
       *peripheral*pressure*36);
    int a=Mth.clamp((int)opacity,0,58);
    if(a<2)continue;
    g.fill(i*dx,j*dy,Math.min(w,(i+1)*dx+1),
           Math.min(h,(j+1)*dy+1),(a<<24)|0x000000);
   }
  }

  if(strike>.01f){
   int a=Mth.clamp((int)(246*strike*pressure),0,247);
   g.fill(0,0,w,h,(a<<24)|0x000000);
  }
 }

 private static void diagnostics(GuiGraphics g,Minecraft mc,float intensity){
  if(!ClientSettings.VOID_DEBUG.get() || mc.player==null)return;
  String str=String.format(java.util.Locale.ROOT,
    "SleepDebt VOID: range %.1fm | pressure %.2f | GPU %s | enabled %s",
    SleeplessDread.nearestDistance(),intensity,
    ClientSettings.VOID_GPU.get() && active!=null && !shaderFailed,
    ClientSettings.VOID_ENABLED.get());
  g.drawString(mc.font,str,4,g.guiHeight()-14,0xFFFFFFFF,true);
 }
 private SleeplessVoidShader(){}
}
