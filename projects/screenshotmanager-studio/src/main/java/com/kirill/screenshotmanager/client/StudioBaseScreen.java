package com.kirill.screenshotmanager.client;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Studio draws its own opaque background; never run vanilla menu blur over its canvas. */
abstract class StudioBaseScreen extends Screen {
    protected StudioBaseScreen(Component title){super(title);}
    @Override public final void renderBackground(GuiGraphics g,int mouseX,int mouseY,float partialTick){}
}
