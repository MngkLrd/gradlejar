package dev.minemods.worldlogic.client;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
/** These screens draw their own complete background before drawing content.
 * Screen.render calls renderBackground before widgets; allowing the vanilla blur
 * there would blur everything we have already drawn. Keep this override final. */
abstract class ClearScreen extends Screen{
 protected ClearScreen(Component title){super(title);}
 @Override public final void renderBackground(GuiGraphics g,int mx,int my,float delta){}
}
