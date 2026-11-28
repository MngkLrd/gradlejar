package dev.minemods.worldlogic.client;
import dev.minemods.worldlogic.GuideBook;
import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
final class GuideIndexScreen extends Screen{
 private final GuideScreen parent;private final List<GuideBook.Chapter> chapters;private final boolean ru;private EditBox search;private int offset;
 GuideIndexScreen(GuideScreen parent,List<GuideBook.Chapter> chapters,boolean ru){super(Component.literal(ru?"Указатель статей":"Article index"));this.parent=parent;this.chapters=chapters;this.ru=ru;}
 private int rows(){return Math.max(1,(height-90)/23);}
 private List<GuideBook.Entry> matches(){return GuideBook.search(chapters,search==null?"":search.getValue(),ru);}
 @Override protected void init(){
  String old=search==null?"":search.getValue();search=new EditBox(font,12,26,Math.max(30,width-24),20,Component.literal(ru?"Поиск":"Search"));search.setMaxLength(100);search.setValue(old);search.setResponder(v->offset=0);addRenderableWidget(search);
  addRenderableWidget(Button.builder(Component.literal("<"),b->offset=Math.max(0,offset-rows())).bounds(12,height-26,32,20).build());
  addRenderableWidget(Button.builder(Component.literal(">"),b->offset=Math.min(Math.max(0,matches().size()-rows()),offset+rows())).bounds(48,height-26,32,20).build());
  addRenderableWidget(Button.builder(Component.translatable("gui.back"),b->onClose()).bounds(width-88,height-26,76,20).build());
 }
 @Override public boolean isPauseScreen(){return false;}
 @Override public void onClose(){Minecraft.getInstance().setScreen(parent);}
 @Override public boolean mouseClicked(double x,double y,int button){
  if(button==0&&x>=12&&x<width-12&&y>=54&&y<54+rows()*23){
   var found=matches();int i=offset+(int)(y-54)/23;if(i<found.size()){var e=found.get(i);parent.select(e.chapter(),e.page());return true;}}
  return super.mouseClicked(x,y,button);
 }
 @Override public boolean mouseScrolled(double x,double y,double dx,double dy){offset=Math.max(0,Math.min(Math.max(0,matches().size()-rows()),offset+(dy<0?1:-1)));return true;}
 @Override public void render(GuiGraphics g,int mx,int my,float dt){
  g.fill(0,0,width,height,0xff46362b);g.drawCenteredString(font,title,width/2,10,0xfff0dfc0);var found=matches();
  offset=Math.min(offset,Math.max(0,found.size()-rows()));
  for(int i=0;i<rows()&&offset+i<found.size();i++){var e=found.get(offset+i);var c=chapters.get(e.chapter());var p=c.pages().get(e.page());int y=54+i*23;boolean hover=mx>=12&&mx<width-12&&my>=y&&my<y+22;g.fill(12,y,width-12,y+22,hover?0xffead9ad:0xffd4bd94);g.drawString(font,font.plainSubstrByWidth(p.title().get(ru),width-34),17,y+2,0xff392c21,false);g.drawString(font,font.plainSubstrByWidth(c.title().get(ru),width-34),17,y+12,0xff76604a,false);}
  g.drawCenteredString(font,Component.literal(Integer.toString(found.size())),width/2,height-20,0xfff0dfc0);super.render(g,mx,my,dt);
 }
}
