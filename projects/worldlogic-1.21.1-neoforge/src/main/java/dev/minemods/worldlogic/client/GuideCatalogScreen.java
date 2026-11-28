package dev.minemods.worldlogic.client;
import dev.minemods.worldlogic.GuideCatalog;
import dev.minemods.worldlogic.api.client.WorldLogicTabs;
import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
final class GuideCatalogScreen extends ClearScreen implements GuideJeiLookup.Hovered{
 private final Screen parent;private final boolean ru;private List<ItemStack> all=List.of(),filtered=List.of();private EditBox search;private int page;
 GuideCatalogScreen(Screen parent,boolean ru){super(Component.literal(ru?"Предметы и блоки":"Items and blocks"));this.parent=parent;this.ru=ru;}
 private int usable(){return WorldLogicTabs.contentWidth(width);}
 private int columns(){return Math.max(1,(usable()-24)/24);}
 private int rows(){return Math.max(1,(height-84)/24);}
 private int capacity(){return columns()*rows();}
 private void filter(String q){String s=q.toLowerCase(Locale.ROOT).strip();filtered=all.stream().filter(st->(st.getHoverName().getString()+" "+BuiltInRegistries.ITEM.getKey(st.getItem())).toLowerCase(Locale.ROOT).contains(s)).toList();page=0;}
 @Override protected void init(){
  var mc=Minecraft.getInstance();all=mc.level==null?List.of():GuideCatalog.items(mc.level.enabledFeatures());
  String old=search==null?"":search.getValue();search=new EditBox(font,12,24,Math.max(30,usable()-24),20,Component.literal(ru?"Поиск":"Search"));search.setValue(old);search.setResponder(this::filter);filter(old);addRenderableWidget(search);
  addRenderableWidget(Button.builder(Component.literal("<"),b->page=Math.max(0,page-1)).bounds(12,height-26,32,20).build());
  addRenderableWidget(Button.builder(Component.literal(">"),b->page=Math.min(Math.max(0,(filtered.size()-1)/capacity()),page+1)).bounds(48,height-26,32,20).build());
  addRenderableWidget(Button.builder(Component.translatable("gui.back"),b->onClose()).bounds(usable()-88,height-26,76,20).build());
  TabRail.install(this,this::addRenderableWidget);
 }
 @Override public ItemStack hovered(double x,double y){
  int cx=(int)Math.floor((x-12)/24),cy=(int)Math.floor((y-52)/24);
  if(cx<0||cx>=columns()||cy<0||cy>=rows())return ItemStack.EMPTY;
  int i=page*capacity()+cy*columns()+cx;return i<filtered.size()?filtered.get(i):ItemStack.EMPTY;
 }
 @Override public boolean keyPressed(int key,int scan,int mods){if(!search.isFocused()&&GuideJeiLookup.key(this,key,scan))return true;return super.keyPressed(key,scan,mods);}
 @Override public boolean mouseClicked(double x,double y,int button){
  var st=hovered(x,y);if(!st.isEmpty()&&(button==0||button==1)){
   if(!GuideJeiLookup.show(st,button==1))Minecraft.getInstance().setScreen(new GuideRecipesScreen(this,ru,st,button==1));return true;
  }return super.mouseClicked(x,y,button);
 }
 @Override public boolean mouseScrolled(double x,double y,double dx,double dy){if(dy==0)return false;page=Math.max(0,Math.min(Math.max(0,(filtered.size()-1)/capacity()),page+(dy<0?1:-1)));return true;}
 @Override public boolean isPauseScreen(){return false;}
 @Override public void onClose(){Minecraft.getInstance().setScreen(parent);}
 @Override public void render(GuiGraphics g,int mx,int my,float dt){
  VanillaPanels.backdrop(g,width,height);VanillaPanels.panel(g,6,6,usable()-6,height-6);
  g.drawCenteredString(font,title,usable()/2,10,VanillaPanels.TEXT);
  for(int k=0;k<capacity()&&page*capacity()+k<filtered.size();k++){
   int x=12+k%columns()*24,y=52+k/columns()*24;var st=filtered.get(page*capacity()+k);
   g.fill(x,y,x+22,y+22,0xff8b8b8b);g.renderOutline(x,y,22,22,0xff555555);g.renderItem(st,x+3,y+3);
  }
  g.drawCenteredString(font,filtered.size()+" · "+(page+1)+"/"+Math.max(1,(filtered.size()+capacity()-1)/capacity()),usable()/2,height-20,VanillaPanels.TEXT);
  super.render(g,mx,my,dt);var st=hovered(mx,my);if(!st.isEmpty())g.renderTooltip(font,st,mx,my);
 }
}
