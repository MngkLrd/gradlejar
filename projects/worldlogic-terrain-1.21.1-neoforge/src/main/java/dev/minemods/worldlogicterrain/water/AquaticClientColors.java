package dev.minemods.worldlogicterrain.water;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;

@EventBusSubscriber(modid="worldlogicterrain",bus=EventBusSubscriber.Bus.MOD,value=Dist.CLIENT)
public final class AquaticClientColors {
    private AquaticClientColors(){}
    @SubscribeEvent public static void blockColors(RegisterColorHandlersEvent.Block event){
        var lily=BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("worldlogicterrain","water_lily"));
        event.register((state,level,pos,tint)->event.getBlockColors().getColor(Blocks.LILY_PAD.defaultBlockState(),level,pos,tint),lily);
    }
}
