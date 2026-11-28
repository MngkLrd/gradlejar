package dev.minemods.worldlogic.client;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.fml.ModList;
/** Optional Patchouli integration through its exact public 1.21.1-93 API.
 * No Patchouli classes are linked when the mod is absent. */
final class GuideOpener{
 private static final ResourceLocation BOOK=ResourceLocation.fromNamespaceAndPath("worldlogic","field_guide");
 static void open(Screen parent){openPatchouli(parent);}
 static void openPatchouli(Screen parent){
  if(ModList.get().isLoaded("patchouli")){
   try{
    Class<?> api=Class.forName("vazkii.patchouli.api.PatchouliAPI");
    Object instance=api.getMethod("get").invoke(null);
    Class<?> contract=Class.forName("vazkii.patchouli.api.PatchouliAPI$IPatchouliAPI");
    contract.getMethod("openBookGUI",ResourceLocation.class).invoke(instance,BOOK);
    return;
   }catch(ReflectiveOperationException|LinkageError ex){org.slf4j.LoggerFactory.getLogger("WorldLogic").error("Patchouli guide open failed",ex);}
  }
  Minecraft.getInstance().setScreen(new GuideScreen(parent));
 }
}
