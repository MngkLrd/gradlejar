package dev.minemods.thermal.client;
import dev.minemods.thermal.network.ThermalSync;
import dev.minemods.thermal.network.HistorySync;
import dev.minemods.thermal.ThermalHistory;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
public final class ThermalScreen extends Screen {
    private final Screen parent;
    private int page,dayIndex;
    private Button older,newer;
    private Button[] tabs=new Button[3];
    ThermalScreen(Screen parent){super(ThermalPanel.tr("title"));this.parent=parent;}
    private int panelW(){return Math.min(252,width-8);}
    private int panelY(){return Math.max(22,(height-166)/2);}
    @Override protected void init(){
        int w=panelW(),x=(width-w)/2,y=panelY();
        String[] keys={"body_tab","weather_tab","history_tab"};
        for(int i=0;i<3;i++){final int index=i;int left=x+i*w/3,right=x+(i+1)*w/3;
            tabs[i]=addRenderableWidget(Button.builder(ThermalPanel.tr(keys[i]),b->{page=index;}).bounds(left,y-21,right-left-1,20).build());}
        int bottom=y+168;
        addRenderableWidget(Button.builder(Component.translatable("gui.back"),b->onClose()).bounds(width/2-40,bottom,80,20).build());
        older=addRenderableWidget(Button.builder(Component.literal("<"),b->dayIndex++).bounds(x,bottom,28,20).build());
        newer=addRenderableWidget(Button.builder(Component.literal(">"),b->dayIndex=Math.max(0,dayIndex-1)).bounds(x+w-28,bottom,28,20).build());
    }
    @Override public void onClose(){if(minecraft!=null)minecraft.setScreen(parent);}
    @Override public boolean isPauseScreen(){return false;}
    @Override public void renderBackground(GuiGraphics g,int mx,int my,float delta){g.fill(0,0,width,height,0x99000000);}
    @Override public void render(GuiGraphics g,int mx,int my,float delta){
        var p=minecraft==null?null:minecraft.player;
        var data=p==null?null:ThermalSync.current(p.getId());
        var history=p==null?java.util.List.<ThermalHistory.Point>of():HistorySync.current(p.getId());
        int count=ThermalHistory.days(history).size();dayIndex=Math.max(0,Math.min(dayIndex,count-1));
        older.visible=newer.visible=page==2;older.active=dayIndex+1<count;newer.active=dayIndex>0;
        for(int i=0;i<3;i++)tabs[i].active=i!=page;
        super.render(g,mx,my,delta);
        int w=panelW();var r=new ThermalLayout.Rect((width-w)/2,panelY(),w,166);
        if(page==0)ThermalPanel.draw(g,r,data);else if(page==1)WeatherPanel.draw(g,r,data);else JournalPanel.draw(g,r,history,dayIndex,mx,my);
    }
}
