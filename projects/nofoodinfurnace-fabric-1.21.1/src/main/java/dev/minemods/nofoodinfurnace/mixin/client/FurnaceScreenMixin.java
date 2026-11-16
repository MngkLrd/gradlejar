package dev.minemods.nofoodinfurnace.mixin.client;
import dev.minemods.nofoodinfurnace.FoodRules;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractFurnaceScreen;
import net.minecraft.client.gui.screens.inventory.FurnaceScreen;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(AbstractFurnaceScreen.class)
public abstract class FurnaceScreenMixin {
 private static final ResourceLocation nofoodinfurnace$CROSS=ResourceLocation.withDefaultNamespace("container/anvil/error");
 @Inject(method="renderBg",at=@At("TAIL"))
 private void nofoodinfurnace$render(GuiGraphics graphics,float delta,int mouseX,int mouseY,CallbackInfo ci){
  if(!((Object)this instanceof FurnaceScreen screen))return;
  var level=Minecraft.getInstance().level;
  if(level==null||!FoodRules.forbidden(level,screen.getMenu().getSlot(0).getItem()))return;
  graphics.blitSprite(nofoodinfurnace$CROSS,screen.getGuiLeft()+77,screen.getGuiTop()+33,28,21);
 }
}
