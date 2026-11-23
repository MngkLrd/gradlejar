package com.kirill.simplesorter.client;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;
@Mod.EventBusSubscriber(modid="simplesorter",bus=Mod.EventBusSubscriber.Bus.MOD,value=Dist.CLIENT)
public final class ClientKeyMappings {
 public static final String CATEGORY="key.categories.simplesorter";
 public static final KeyMapping OPEN_SETTINGS=new KeyMapping("key.simplesorter.open_settings",InputConstants.Type.KEYSYM,GLFW.GLFW_KEY_G,CATEGORY);
 public static final KeyMapping QUICK_SORT=new KeyMapping("key.simplesorter.quick_sort",InputConstants.Type.KEYSYM,GLFW.GLFW_KEY_H,CATEGORY);
 private ClientKeyMappings(){}
 @SubscribeEvent public static void register(RegisterKeyMappingsEvent e){e.register(OPEN_SETTINGS);e.register(QUICK_SORT);}
}
