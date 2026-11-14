package dev.minemods.halohaven.client;

import dev.minemods.halohaven.HaloEntities;
import dev.minemods.halohaven.HaloHaven;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.VillagerRenderer;
import net.minecraft.client.renderer.entity.RabbitRenderer;
import net.minecraft.client.renderer.entity.AllayRenderer;
import net.minecraft.client.renderer.entity.IronGolemRenderer;
import net.minecraft.client.renderer.entity.VexRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.animal.Rabbit;
import net.minecraft.world.entity.animal.allay.Allay;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.monster.Vex;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

@EventBusSubscriber(modid = HaloHaven.ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class HaloClient {
    private HaloClient() {}
    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(HaloEntities.SKYFOLK.get(), SkyfolkRenderer::new);
        event.registerEntityRenderer(HaloEntities.CLOUDHARE.get(), CloudhareRenderer::new);
        event.registerEntityRenderer(HaloEntities.LUMEN.get(), LumenRenderer::new);
        event.registerEntityRenderer(HaloEntities.DAWN_GUARDIAN.get(), GuardianRenderer::new);
        event.registerEntityRenderer(HaloEntities.DUSK_WRAITH.get(), WraithRenderer::new);
        event.registerEntityRenderer(HaloEntities.ECLIPSE_REVENANT.get(), EclipseRenderer::new);
    }

    public static final class SkyfolkRenderer extends VillagerRenderer {
        public SkyfolkRenderer(EntityRendererProvider.Context c) {
            super(c);
            // The clothes are painted directly into the custom villager skin,
            // preventing vanilla profession textures from covering them.
            this.layers.clear();
        }
        @Override public ResourceLocation getTextureLocation(Villager v) {
            return HaloHaven.id("textures/entity/skyfolk.png");
        }
        @Override
        public void render(Villager e,float yaw,float partial,PoseStack pose,MultiBufferSource buffer,int light){
            super.render(e,yaw,partial,pose,buffer,light);
            CelestialHalo.draw(e,partial,pose,buffer,0.44f,2.20f,247,207,135);
        }
    }
    public static final class CloudhareRenderer extends RabbitRenderer {
        public CloudhareRenderer(EntityRendererProvider.Context c) { super(c); }
        @Override public ResourceLocation getTextureLocation(Rabbit e) {
            return HaloHaven.id("textures/entity/cloudhare.png");
        }
    }
    public static final class LumenRenderer extends AllayRenderer {
        public LumenRenderer(EntityRendererProvider.Context c) { super(c); }
        @Override public ResourceLocation getTextureLocation(Allay e) {
            return HaloHaven.id("textures/entity/lumen.png");
        }
        @Override
        public void render(Allay e,float yaw,float partial,PoseStack pose,MultiBufferSource buffer,int light){
            super.render(e,yaw,partial,pose,buffer,light);
            CelestialHalo.draw(e,partial,pose,buffer,0.25f,0.9f,135,245,223);
        }
    }
    public static final class GuardianRenderer extends IronGolemRenderer {
        public GuardianRenderer(EntityRendererProvider.Context c) { super(c); }
        @Override public ResourceLocation getTextureLocation(IronGolem e) {
            return HaloHaven.id("textures/entity/dawn_guardian.png");
        }
        @Override
        public void render(IronGolem e,float yaw,float partial,PoseStack pose,MultiBufferSource buffer,int light){
            super.render(e,yaw,partial,pose,buffer,light);
            CelestialHalo.draw(e,partial,pose,buffer,0.75f,3.06f,255,228,173);
        }
    }
    public static final class EclipseRenderer extends VexRenderer {
        public EclipseRenderer(EntityRendererProvider.Context ctx){super(ctx);}
        @Override public ResourceLocation getTextureLocation(Vex e){
            return HaloHaven.id("textures/entity/eclipse_revenant.png");
        }
        @Override
        public void render(Vex e,float yaw,float partial,PoseStack pose,MultiBufferSource buffer,int light){
            pose.pushPose();
            pose.scale(1.8f,1.8f,1.8f);
            super.render(e,yaw,partial,pose,buffer,light);
            CelestialHalo.draw(e,partial,pose,buffer,0.46f,1.02f,227,113,218);
            pose.popPose();
        }
    }
    public static final class WraithRenderer extends VexRenderer {
        public WraithRenderer(EntityRendererProvider.Context c) { super(c); }
        @Override public ResourceLocation getTextureLocation(Vex e) {
            return HaloHaven.id("textures/entity/dusk_wraith.png");
        }
        @Override
        public void render(Vex e,float yaw,float partial,PoseStack pose,MultiBufferSource buffer,int light){
            super.render(e,yaw,partial,pose,buffer,light);
            CelestialHalo.draw(e,partial,pose,buffer,0.34f,1.0f,141,119,213);
        }
    }
}
