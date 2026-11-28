package dev.minemods.aquaculture.client;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.minemods.aquaculture.*;
import net.minecraft.client.renderer.entity.CodRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.world.entity.animal.Cod;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

@EventBusSubscriber(modid=Aquaculture.MOD_ID,bus=EventBusSubscriber.Bus.MOD,value=Dist.CLIENT)
public final class ClientRegistration {
 @SubscribeEvent public static void register(EntityRenderersEvent.RegisterRenderers e){
  for(var s:Species.ALL)e.registerEntityRenderer(Aquaculture.TYPES.get(s.id()).get(),ctx->new RegionalFishRenderer(ctx,s));
 }
 private static final class RegionalFishRenderer extends CodRenderer{
  private final Species s;
  RegionalFishRenderer(EntityRendererProvider.Context ctx,Species species){super(ctx);this.s=species;}
  @Override protected void scale(Cod fish,PoseStack pose,float partial){
   pose.scale((float)(s.width()/.19),(float)(s.height()/.28),(float)(s.length()/.45));
  }
 }
}

