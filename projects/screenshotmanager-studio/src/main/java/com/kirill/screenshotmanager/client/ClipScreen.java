package com.kirill.screenshotmanager.client;
import com.kirill.screenshotmanager.core.ClipCodec;
import com.kirill.screenshotmanager.core.ShotFiles;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import java.util.*;
import java.util.concurrent.CompletableFuture;

final class ClipScreen extends StudioBaseScreen {
    private final List<ClipCodec.Frame> frames;
    private final List<StudioUi.Hit> hits=new ArrayList<>();
    private int first,last,index,fps=10,side=640,budget=1990,displayed=-1;
    private boolean playing,busy,alive;
    private long next;
    private StudioTexture texture;
    private String status="Drag or click timeline; set start / end, then export";
    ClipScreen(List<ClipCodec.Frame> frames){super(Component.literal("GIF Studio"));this.frames=frames;last=frames.size()-1;}
    @Override protected void init(){alive=true;}
    private void button(GuiGraphics g,String name,int x,int y,int w,int h,int mx,int my,boolean active,Runnable action){
        StudioUi.button(g,font,name,x,y,w,h,mx,my,active);hits.add(new StudioUi.Hit(x,y,w,h,action));
    }
    private void seek(int i){index=Math.max(0,Math.min(frames.size()-1,i));playing=false;}
    private void export(){
        if(busy)return;busy=true;playing=false;status="Encoding GIF...";
        int a=first,b=last+1,f=fps,s=side,k=budget;
        CompletableFuture.supplyAsync(()->{
            try{return ClipCodec.save(StudioClient.directory().resolve("clips"),frames,a,b,f,s,k*1000L);}
            catch(Exception e){throw new RuntimeException(e);}
        },ShotFiles.IO).whenComplete((result,error)->Minecraft.getInstance().execute(()->{
            if(!alive)return;busy=false;
            if(error==null)status="Saved "+result.width()+"x"+result.height()+" / "+result.frames()+" frames / "+result.bytes()/1000+" KB";
            else status="GIF export failed: "+(error.getCause()==null?error.getMessage():error.getCause().getMessage());
        }));
    }
    @Override public void render(GuiGraphics g,int mx,int my,float dt){
        hits.clear();StudioUi.box(g,0,0,width,height,StudioUi.BG);
        g.drawString(font,"GIF STUDIO",8,9,StudioUi.ACCENT,false);
        StudioUi.label(g,font,frames.size()+" captured frames",95,9,width-102,StudioUi.MUTED);
        if(playing&&System.nanoTime()>=next){index=index>=last?first:index+1;next=System.nanoTime()+100_000_000L;}
        if(index!=displayed){
            if(texture!=null)texture.close();texture=new StudioTexture(frames.get(index).image());displayed=index;
        }
        int previewH=Math.max(8,height-126);
        double k=Math.min((width-20)/(double)texture.width,previewH/(double)texture.height);
        int w=Math.max(1,(int)(texture.width*k)),h=Math.max(1,(int)(texture.height*k));
        texture.draw(g,(width-w)/2,22+(previewH-h)/2,w,h);
        int y=height-101,bar=Math.max(1,width-20),steps=Math.max(1,frames.size()-1);
        StudioUi.box(g,10,y,bar,12,StudioUi.EDGE);
        int start=first*bar/steps,end=(last+1)*bar/Math.max(1,frames.size());
        StudioUi.box(g,10+start,y,Math.max(1,end-start),12,StudioUi.ACTIVE);
        StudioUi.box(g,10+Math.min(bar-2,index*bar/steps),y-2,2,16,StudioUi.ACCENT);
        g.drawCenteredString(font,"Frame "+(index+1)+" | Trim "+(first+1)+"-"+(last+1),width/2,y-12,StudioUi.TEXT);
        int bw=Math.max(1,(width-12)/5);
        button(g,"<",6,y+17,bw-2,16,mx,my,false,()->seek(index-1));
        button(g,playing?"Pause":"Play",6+bw,y+17,bw-2,16,mx,my,playing,()->{playing=!playing;if(index<first||index>last)index=first;next=0;});
        button(g,">",6+2*bw,y+17,bw-2,16,mx,my,false,()->seek(index+1));
        button(g,"Set start",6+3*bw,y+17,bw-2,16,mx,my,false,()->{first=Math.min(index,last);});
        button(g,"Set end",6+4*bw,y+17,bw-2,16,mx,my,false,()->{last=Math.max(index,first);});
        button(g,"FPS "+fps,6,y+36,bw-2,16,mx,my,false,()->fps=fps==10?8:fps==8?5:10);
        button(g,side+" px",6+bw,y+36,bw-2,16,mx,my,false,()->side=side==640?480:side==480?320:640);
        button(g,budget+" KB",6+2*bw,y+36,bw-2,16,mx,my,false,()->budget=budget==1990?1000:budget==1000?500:budget==500?5000:1990);
        button(g,"Reset trim",6+3*bw,y+36,bw-2,16,mx,my,false,()->{first=0;last=frames.size()-1;});
        button(g,busy?"Encoding":"Export GIF",6+4*bw,y+36,bw-2,16,mx,my,false,this::export);
        StudioUi.label(g,font,status,8,height-18,width-16,StudioUi.MUTED);
    }
    @Override public boolean mouseClicked(double x,double y,int button){
        if(button==0){
            for(var h:List.copyOf(hits))if(h.contains(x,y)){h.action().run();return true;}
            if(y>=height-104&&y<=height-87){seek((int)Math.round((x-10)/Math.max(1,width-20)*(frames.size()-1)));return true;}
        }return super.mouseClicked(x,y,button);
    }
    @Override public boolean mouseDragged(double x,double y,int button,double dx,double dy){
        if(button==0&&y>=height-104&&y<=height-87){seek((int)Math.round((x-10)/Math.max(1,width-20)*(frames.size()-1)));return true;}
        return super.mouseDragged(x,y,button,dx,dy);
    }
    @Override public boolean isPauseScreen(){return false;}
    @Override public void removed(){alive=false;playing=false;if(texture!=null){texture.close();texture=null;}displayed=-1;}
}
