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
  if(intensity<=.005f)return;
  int w=gui.guiWidth(),h=gui.guiHeight();
  if(w<1||h<1)return;
  // Always obscure most of the world at maximum presence, including torches.
  ShaderInstance sh=active;
  if(sh==null || shaderFailed){
   fallback(gui,w,h,intensity);
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
   fallback(gui,w,h,intensity);
  }finally{
   RenderSystem.depthMask(true);
   RenderSystem.enableDepthTest();
   RenderSystem.disableBlend();
  }
 }

 private static void fallback(GuiGraphics g,int w,int h,float strength){
  float p=Mth.clamp(strength,0f,1f);
  // Standalone alpha compositor, no vanilla fog/potion fallback.
  int base=(int)(220*p*p);
  g.fill(0,0,w,h,(base<<24)|0x030305);
  // Nonuniform black rings deliberately crush edge visibility.
  int rings=12;
  for(int i=rings;i>0;i--){
   float f=i/(float)rings;
   int dx=(int)(w*.48f*f*p),dy=(int)(h*.49f*f*p);
   int alpha=Math.min(242,(int)(26*p*(1f-.4f*f)));
   int col=(alpha<<24)|0x010102;
   if(dx>0){g.fill(0,0,dx,h,col);g.fill(w-dx,0,w,h,col);}
   if(dy>0){g.fill(0,0,w,dy,col);g.fill(0,h-dy,w,h,col);}
  }
 }
 private SleeplessVoidShader(){}
}
