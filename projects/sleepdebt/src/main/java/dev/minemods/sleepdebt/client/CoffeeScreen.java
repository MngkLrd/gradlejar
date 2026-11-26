package dev.minemods.sleepdebt.client;

import dev.minemods.sleepdebt.SleepDebtMod;
import dev.minemods.sleepdebt.menu.CoffeeMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/**
 * Pixel-perfect native-scaled interface. Every slot in the 176x166 PNG is
 * aligned 1:1 with CoffeeMenu; all text uses the Minecraft font instance.
 */
public final class CoffeeScreen extends AbstractContainerScreen<CoffeeMenu> {
 private static final ResourceLocation BACKGROUND=ResourceLocation.fromNamespaceAndPath(
  SleepDebtMod.MOD_ID,"textures/gui/coffee_maker.png");
 public CoffeeScreen(CoffeeMenu menu,Inventory inventory,Component title){
  super(menu,inventory,title);
  imageWidth=176;imageHeight=166;
  titleLabelX=8;titleLabelY=7;inventoryLabelY=72;
 }
 @Override protected void renderBg(GuiGraphics gui,float partial,int mouseX,int mouseY){
  gui.blit(BACKGROUND,leftPos,topPos,0f,0f,imageWidth,imageHeight,imageWidth,imageHeight);
  int progress=Math.min(22,Math.max(0,menu.progress()*22/menu.total()));
  if(progress>0){gui.fill(leftPos+109,topPos+48,leftPos+109+progress,topPos+54,0xFFB7834F);gui.fill(leftPos+109,topPos+48,leftPos+109+progress,topPos+49,0xFFE6BE83);}
 }
 @Override protected void renderLabels(GuiGraphics g,int mouseX,int mouseY){
  g.drawString(font,Component.translatable("container.sleepdebt.coffee_machine"),8,7,0xFFF1E1C4,false);
  slotLabel(g,"base",36);slotLabel(g,"ingredient",63);slotLabel(g,"fuel",90);slotLabel(g,"output",144);
  g.drawString(font,playerInventoryTitle,8,72,0xFF40372D,false);
 }
 private void slotLabel(GuiGraphics g,String key,int center){
  Component text=Component.translatable("gui.sleepdebt.brew."+key);
  g.drawString(font,text,center-font.width(text)/2,29,0xFFE9E2D5,false);
 }
 @Override public void render(GuiGraphics gui,int mouseX,int mouseY,float partial){
  renderBackground(gui,mouseX,mouseY,partial);
  super.render(gui,mouseX,mouseY,partial);
  renderTooltip(gui,mouseX,mouseY);
 }
}
