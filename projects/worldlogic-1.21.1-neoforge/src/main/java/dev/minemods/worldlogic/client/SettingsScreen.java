package dev.minemods.worldlogic.client;
import dev.minemods.worldlogic.Rules;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import java.util.*;
public final class SettingsScreen extends Screen {
 private final Screen parent;private Map<String,String> draft=Rules.snapshot();private int page=0;private String status="";
 public SettingsScreen(Screen parent){super(Component.literal("World Logic"));this.parent=parent;}
 private void button(int x,int y,int w,String title,Runnable action){addRenderableWidget(Button.builder(Component.literal(title),b->action.run()).bounds(x,y,w,20).build());}
 private boolean remote(){return minecraft!=null&&minecraft.level!=null&&minecraft.getSingleplayerServer()==null;}
 @Override protected void init(){
  int w=Math.min(width-20,420),left=(width-w)/2,half=(w-6)/2;
  if(remote()){button(left,height-28,w,"Back",this::onClose);return;}
  for(int i=0;i<3;i++){String name=List.of("light","realistic","hardcore").get(i);int x=left+i*(w/3);button(x,28,w/3-3,name,()->{draft=Rules.preset(name);rebuildWidgets();});}
  List<String> keys=new ArrayList<>(Rules.keys());for(String k:Rules.tuningKeys())keys.add("strength."+k);
  int rows=Math.max(1,(height-112)/24),per=rows*2,max=(keys.size()-1)/per;page=Math.min(page,max);
  for(int i=0;i<per&&page*per+i<keys.size();i++){String k=keys.get(page*per+i);String v=draft.getOrDefault(k,"false");boolean number=k.startsWith("strength.");int x=left+(i%2)*(half+6),y=55+(i/2)*24;
   String label=k.replace("strength.","").replace('_',' ')+": "+(number?v:Boolean.parseBoolean(v)?"ON":"OFF");
   button(x,y,half,label,()->{if(number){double d=Double.parseDouble(draft.getOrDefault(k,"1"));draft.put(k,Double.toString(d>=2?.25:Math.min(2,Math.round((d+.25)*100)/100.0)));}else draft.put(k,Boolean.toString(!Boolean.parseBoolean(v)));draft.put("preset","custom");rebuildWidgets();});
  }
  button(left,height-52,half,"< Page "+(page+1)+"/"+(max+1),()->{if(page>0){page--;rebuildWidgets();}});
  button(left+half+6,height-52,half,"Next >",()->{if(page<max){page++;rebuildWidgets();}});
  button(left,height-28,half,"Cancel",this::onClose);
  button(left+half+6,height-28,half,"Save",()->{if(Rules.apply(draft))onClose();else status="Could not write configuration";});
 }
 @Override public void render(GuiGraphics g,int mouseX,int mouseY,float partial){super.render(g,mouseX,mouseY,partial);g.drawCenteredString(font,title,width/2,9,0xffffff);if(remote())g.drawCenteredString(font,"Remote server: use /worldlogic as an operator",width/2,40,0xffffff);else g.drawCenteredString(font,status.isEmpty()?"Preset: "+draft.getOrDefault("preset","custom"):status,width/2,height-65,0xbfbfbf);}
 @Override public void onClose(){if(minecraft!=null)minecraft.setScreen(parent);}
}
