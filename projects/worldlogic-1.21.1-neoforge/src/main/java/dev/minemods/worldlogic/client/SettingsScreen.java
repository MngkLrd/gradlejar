package dev.minemods.worldlogic.client;
import dev.minemods.worldlogic.Rules;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import java.util.*;
public final class SettingsScreen extends Screen {
 private final Screen parent;private int page;
 public SettingsScreen(Screen parent){super(Component.literal("World Logic: Realistic"));this.parent=parent;}
 private void button(int x,int y,int w,String title,Runnable action){addRenderableWidget(Button.builder(Component.literal(title),b->action.run()).bounds(x,y,w,20).build());}
 private boolean remote(){return minecraft!=null&&minecraft.level!=null&&minecraft.getSingleplayerServer()==null;}
 private int rows(){return Math.max(1,(height-90)/13);}
 @Override protected void init(){
  int w=Math.min(width-20,420),left=(width-w)/2,half=(w-6)/2;
  if(!remote()){
   int max=(Rules.keys().size()-1)/rows();page=Math.min(page,max);
   button(left,height-52,half,"< Page "+(page+1)+"/"+(max+1),()->{if(page>0){page--;rebuildWidgets();}});
   button(left+half+6,height-52,half,"Next >",()->{if(page<max){page++;rebuildWidgets();}});
  }
  button(left,height-28,w,"Back",this::onClose);
 }
 @Override public void render(GuiGraphics g,int mouseX,int mouseY,float partial){
  super.render(g,mouseX,mouseY,partial);g.drawCenteredString(font,title,width/2,9,0xffffff);
  if(remote()){g.drawCenteredString(font,"Rules are controlled by the server.",width/2,32,0xffffff);return;}
  g.drawCenteredString(font,"One realism mode; addon ownership respected.",width/2,23,0xbfbfbf);
  var keys=Rules.keys();int left=(width-Math.min(width-20,420))/2;
  for(int i=0;i<rows()&&page*rows()+i<keys.size();i++){
   String k=keys.get(page*rows()+i);boolean enabled=Rules.on(k);
   String label=k.replace('_',' ')+": "+(enabled?"ON":"OFF / delegated");
   g.drawString(font,label,left,39+i*13,enabled?0xc0e0c0:0xc0c0c0);
  }
 }
 @Override public void onClose(){if(minecraft!=null)minecraft.setScreen(parent);}
}
