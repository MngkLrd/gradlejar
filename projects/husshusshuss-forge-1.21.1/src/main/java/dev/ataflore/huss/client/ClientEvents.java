package dev.ataflore.huss.client;

import dev.ataflore.huss.HussMod;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.npc.Villager;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.client.event.RenderNameTagEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

public final class ClientEvents {
    private static TrainingRenderer renderer;
    @Mod.EventBusSubscriber(modid=HussMod.ID,bus=Mod.EventBusSubscriber.Bus.MOD,value=Dist.CLIENT)
    public static final class ModBus {
        @SubscribeEvent public static void layers(EntityRenderersEvent.RegisterLayerDefinitions e) {
            e.registerLayerDefinition(TrainingRenderer.LAYER,TrainingModel::layer);
        }
        @SubscribeEvent public static void setup(EntityRenderersEvent.AddLayers e) {
            renderer=new TrainingRenderer(e.getContext());
        }
    }
    @Mod.EventBusSubscriber(modid=HussMod.ID,bus=Mod.EventBusSubscriber.Bus.FORGE,value=Dist.CLIENT)
    public static final class GameBus {
        @SubscribeEvent public static void render(RenderLivingEvent.Pre<?,?> e) {
            if(renderer==null || e.getRenderer()==renderer || !(e.getEntity() instanceof Villager v) || !v.hasEffect(HussMod.trainingEffect()) || v.isBaby()) return;
            e.setCanceled(true);
            renderer.render(v,Mth.rotLerp(e.getPartialTick(),v.yRotO,v.getYRot()),e.getPartialTick(),e.getPoseStack(),e.getMultiBufferSource(),e.getPackedLight());
        }
        @SubscribeEvent public static void name(RenderNameTagEvent e) {
            if(!(e.getEntity() instanceof Villager v) || v.getEffect(HussMod.trainingEffect())==null || v.hasCustomName()) return;
            int elapsed=dev.ataflore.huss.training.Training.DURATION+2-v.getEffect(HussMod.trainingEffect()).getDuration();
            int p=Math.floorMod(elapsed,36);
            if(p<2 || p>30) return;
            e.setContent(Component.literal(p<12?"HUSS!":p<22?"HUSS! HUSS!":"HUSS! HUSS! HUSS!").withStyle(ChatFormatting.GOLD,ChatFormatting.BOLD));
            e.setResult(Event.Result.ALLOW);
        }
    }
}
