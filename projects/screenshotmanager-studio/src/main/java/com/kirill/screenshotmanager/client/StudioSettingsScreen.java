package com.kirill.screenshotmanager.client;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import java.util.ArrayList;
import java.util.List;

public final class StudioSettingsScreen extends Screen {
    private final Screen parent;
    private final List<StudioUi.Hit> hits=new ArrayList<>();
    private String status="Changes are saved automatically";
    public StudioSettingsScreen(Screen parent){super(Component.literal("Screenshot Manager Settings"));this.parent=parent;}
    private int value(String k,int d,int min,int max){return StudioClient.preferences().number(k,d,min,max);}
    private void set(String k,int v){try{StudioClient.preferences().number(k,v);status="Saved";}catch(Exception e){status="Could not save settings";}}
    private void button(GuiGraphics g,String label,int x,int y,int w,int h,int mx,int my,Runnable run){StudioUi.button(g,font,label,x,y,w,h,mx,my,false);hits.add(new StudioUi.Hit(x,y,w,h,run));}
    private void row(GuiGraphics g,String title,String value,int y,int mx,int my,Runnable run){
        int x=Math.max(8,width/2-150),w=Math.min(300,width-16);
        StudioUi.box(g,x,y,w,22,StudioUi.PANEL);
        StudioUi.label(g,font,title,x+5,y+7,w/2-7,StudioUi.TEXT);
        button(g,value,x+w/2,y+3,w/2-4,16,mx,my,run);
    }
    @Override public void render(GuiGraphics g,int mx,int my,float dt){
        hits.clear();StudioUi.box(g,0,0,width,height,StudioUi.BG);
        g.drawCenteredString(font,"SCREENSHOT MANAGER / SETTINGS",width/2,12,StudioUi.ACCENT);
        int en=value("hud.enabled",1,0,1),seconds=value("hud.seconds",5,2,20),size=value("hud.width",168,100,260),corner=value("hud.corner",0,0,3),flash=value("hud.flash",1,0,1),grid=value("photo.grid",1,0,1);
        int step=Math.max(19,Math.min(27,(height-68)/6)),y=32;
        row(g,"Screenshot preview",en==1?"On":"Off",y,mx,my,()->set("hud.enabled",1-en));
        row(g,"Preview duration",seconds+" s",y+=step,mx,my,()->set("hud.seconds",seconds>=15?2:seconds+1));
        row(g,"Preview size",size+" px",y+=step,mx,my,()->set("hud.width",size>=240?100:Math.min(260,size+20)));
        row(g,"Preview position",new String[]{"Bottom right","Bottom left","Top right","Top left"}[corner],y+=step,mx,my,()->set("hud.corner",(corner+1)%4));
        row(g,"Capture flash",flash==1?"On":"Off",y+=step,mx,my,()->set("hud.flash",1-flash));
        row(g,"Photo grid",grid==1?"On":"Off",y+=step,mx,my,()->set("photo.grid",1-grid));
        g.drawCenteredString(font,status,width/2,height-31,StudioUi.MUTED);
        button(g,"Back",width/2-45,height-22,90,17,mx,my,this::onClose);
    }
    @Override public boolean mouseClicked(double x,double y,int button){if(button==0)for(var h:List.copyOf(hits))if(h.contains(x,y)){h.action().run();return true;}return super.mouseClicked(x,y,button);}
    @Override public void onClose(){Minecraft.getInstance().setScreen(parent);}
    @Override public boolean isPauseScreen(){return false;}
}
