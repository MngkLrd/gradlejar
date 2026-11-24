package com.kirill.screenshotmanager.client;
import com.kirill.screenshotmanager.core.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import java.awt.image.BufferedImage;
import java.util.*;
import java.util.concurrent.CompletableFuture;

final class ThumbnailScreen extends StudioBaseScreen{
    private final ShotFiles.Shot shot;
    private final BufferedImage source;
    private final Screen parent;
    private final List<StudioUi.Hit> hits=new ArrayList<>();
    private final List<StudioSlider> sliders=new ArrayList<>();
    private StudioSlider active;
    private EditBox title,subtitle;
    private String heading="",sub="",status="Choose format and template";
    private int format,layout=1,color=0xffffffff,tx=6,ty=72,size=11;
    private boolean dirty=true,alive,busy,guides=true,rendering;
    private StudioTexture preview;
    private int bx,by,bw,bh,side;
    ThumbnailScreen(ShotFiles.Shot shot,BufferedImage source,Screen parent){super(Component.literal("Thumbnail Studio"));this.shot=shot;this.source=source;this.parent=parent;}
    @Override protected void init(){
        alive=true;side=Math.max(104,Math.min(160,width/3));
        title=new EditBox(font,width-side+6,55,side-12,17,Component.literal("Title"));title.setMaxLength(100);title.setValue(heading);title.setHint(Component.literal("Title"));title.setResponder(v->{heading=v;dirty=true;});addRenderableWidget(title);
        subtitle=new EditBox(font,width-side+6,77,side-12,17,Component.literal("Subtitle"));subtitle.setMaxLength(140);subtitle.setValue(sub);subtitle.setHint(Component.literal("Subtitle"));subtitle.setResponder(v->{sub=v;dirty=true;});addRenderableWidget(subtitle);
        setFocused(null);dirty=true;
    }
    private ThumbnailComposer.Settings settings(){return new ThumbnailComposer.Settings(format,layout,heading,sub,color,tx,ty,size,guides);}
    @Override public void tick(){
        super.tick();if(!dirty||rendering||!alive)return;dirty=false;rendering=true;var set=settings();
        CompletableFuture.supplyAsync(()->ThumbnailComposer.compose(source,set,720),ShotFiles.IO).whenComplete((img,e)->Minecraft.getInstance().execute(()->{
            rendering=false;if(!alive||e!=null)return;var next=new StudioTexture(img);if(preview!=null)preview.close();preview=next;
        }));
    }
    private void button(GuiGraphics g,String label,int x,int y,int w,int h,int mx,int my,Runnable run){
        StudioUi.button(g,font,label,x,y,w,h,mx,my,false);hits.add(new StudioUi.Hit(x,y,w,h,run));
    }
    private void slider(GuiGraphics g,String label,int x,int y,int w,int min,int max,int v,java.util.function.IntConsumer set){
        var slider=new StudioSlider(x,y+11,w,12,min,max,v,n->{set.accept(n);dirty=true;});sliders.add(slider);slider.draw(g,font,label,v);
    }
    @Override public void render(GuiGraphics g,int mx,int my,float dt){
        hits.clear();sliders.clear();StudioUi.box(g,0,0,width,height,StudioUi.BG);StudioUi.box(g,width-side,0,side,height,StudioUi.PANEL);
        g.drawString(font,"THUMBNAIL STUDIO",8,8,StudioUi.ACCENT,false);
        button(g,"X",width-22,4,18,17,mx,my,this::onClose);
        int x=width-side+6,w=side-12;
        button(g,ThumbnailComposer.NAMES[format],x,27,w,17,mx,my,()->{format=(format+1)%ThumbnailComposer.NAMES.length;dirty=true;});
        title.render(g,mx,my,dt);subtitle.render(g,mx,my,dt);
        button(g,new String[]{"Clean","Cinematic","Showcase","Left fade"}[layout],x,99,w,16,mx,my,()->{layout=(layout+1)%4;dirty=true;});
        int[] colors={0xffffffff,0xffffda68,0xff89d6ff,0xffff7777};int cell=w/4;
        for(int i=0;i<4;i++){int c=colors[i],xx=x+i*cell;StudioUi.box(g,xx,119,cell-2,10,c);hits.add(new StudioUi.Hit(xx,119,cell-2,10,()->{color=c;dirty=true;}));}
        slider(g,"Title size "+size+"%",x,134,w,5,25,size,n->size=n);
        if(height>=245){slider(g,"X "+tx+"%",x,160,w,3,75,tx,n->tx=n);slider(g,"Y "+ty+"%",x,186,w,10,88,ty,n->ty=n);}
        button(g,guides?"Guides on":"Guides off",8,27,65,17,mx,my,()->guides=!guides);
        button(g,"Export PNG",width-side-82,27,76,17,mx,my,()->export(false));
        if(preview!=null){
            double k=Math.min((width-side-16)/(double)preview.width,(height-74)/(double)preview.height);
            bw=Math.max(1,(int)(preview.width*k));bh=Math.max(1,(int)(preview.height*k));bx=(width-side-bw)/2;by=49+(height-74-bh)/2;
            preview.draw(g,bx,by,bw,bh);
            if(guides){for(int i=1;i<=2;i++){StudioUi.box(g,bx+bw*i/3,by,1,bh,0x66ffffff);StudioUi.box(g,bx,by+bh*i/3,bw,1,0x66ffffff);}}
        }
        StudioUi.label(g,font,status,8,height-16,width-side-16,StudioUi.MUTED);
        button(g,"Open in Studio",x,height-24,w,18,mx,my,()->export(true));
    }
    private void export(boolean edit){
        if(busy)return;busy=true;status="Rendering full size...";var set=settings();
        CompletableFuture.supplyAsync(()->{
            try{return CreatorExport.save(StudioClient.directory().resolve("edited"),ThumbnailComposer.compose(source,set,0),false,100,0,0);}
            catch(Exception e){throw new RuntimeException(e);}
        },ShotFiles.IO).whenComplete((path,e)->Minecraft.getInstance().execute(()->{
            busy=false;if(!alive)return;if(e!=null){status="Export failed";return;}
            status="Saved "+path.getFileName();if(edit)Minecraft.getInstance().setScreen(new StudioScreen(new ShotFiles.Shot(path,System.currentTimeMillis()),parent));
        }));
    }
    @Override public boolean mouseClicked(double x,double y,int button){
        if(button==0&&(title.isMouseOver(x,y)||subtitle.isMouseOver(x,y)))return super.mouseClicked(x,y,button);
        setFocused(null);title.setFocused(false);subtitle.setFocused(false);
        if(button==0){for(var slider:sliders)if(slider.contains(x,y)){active=slider;slider.update(x);setDragging(true);return true;}
            for(var hit:List.copyOf(hits))if(hit.contains(x,y)){hit.action().run();return true;}
            if(preview!=null&&x>=bx&&x<bx+bw&&y>=by&&y<by+bh){tx=Math.max(3,Math.min(75,(int)((x-bx)*100/bw)));ty=Math.max(10,Math.min(88,(int)((y-by)*100/bh)));dirty=true;return true;}}
        return super.mouseClicked(x,y,button);
    }
    @Override public boolean mouseDragged(double x,double y,int button,double dx,double dy){if(active!=null&&button==0){active.update(x);return true;}return super.mouseDragged(x,y,button,dx,dy);}
    @Override public boolean mouseReleased(double x,double y,int button){if(active!=null){active.update(x);active=null;setDragging(false);return true;}return super.mouseReleased(x,y,button);}
    @Override public boolean isPauseScreen(){return false;}
    @Override public void onClose(){Minecraft.getInstance().setScreen(parent);}
    @Override public void removed(){alive=false;if(preview!=null){preview.close();preview=null;}dirty=true;active=null;}
}
