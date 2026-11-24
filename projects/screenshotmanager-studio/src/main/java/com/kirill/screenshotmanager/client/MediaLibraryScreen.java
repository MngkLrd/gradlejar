package com.kirill.screenshotmanager.client;
import com.kirill.screenshotmanager.core.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;

final class MediaLibraryScreen extends StudioBaseScreen{
    private final Screen parent;
    private final List<Path> initial;
    private final List<StudioUi.Hit> hits=new ArrayList<>();
    private List<MediaFiles.Entry> entries=List.of();
    private MediaFiles.Entry selected;
    private EditBox input;
    private int page,tab;
    private boolean busy,alive;
    private String status="Loading media",name="";
    MediaLibraryScreen(Screen parent,List<Path> initial){super(Component.literal("Library 2.0"));this.parent=parent;this.initial=initial;}
    @Override protected void init(){
        alive=true;input=new EditBox(font,8,height-53,Math.max(50,width-16),16,Component.literal("Rename selected media"));input.setValue(name);input.setMaxLength(100);input.setHint(Component.literal("New file name"));input.setResponder(v->name=v);addRenderableWidget(input);setFocused(null);scan();
    }
    private void scan(){
        if(busy)return;busy=true;
        CompletableFuture.supplyAsync(()->{try{return MediaFiles.list(StudioClient.directory());}catch(Exception e){throw new RuntimeException(e);}},ShotFiles.IO).whenComplete((list,error)->Minecraft.getInstance().execute(()->{
            busy=false;if(!alive)return;if(error!=null){status="Scan failed";return;}entries=list;if(selected==null&&!initial.isEmpty())selected=entries.stream().filter(e->e.path().equals(initial.getFirst())).findFirst().orElse(null);status=entries.size()+" files";
        }));
    }
    private void button(GuiGraphics g,String label,int x,int y,int w,int h,int mx,int my,Runnable run){StudioUi.button(g,font,label,x,y,w,h,mx,my,false);hits.add(new StudioUi.Hit(x,y,w,h,run));}
    private void change(boolean rename){
        if(selected==null||busy)return;busy=true;var entry=selected;String value=name;
        CompletableFuture.supplyAsync(()->{try{return rename?MediaFiles.rename(StudioClient.directory(),entry,value):MediaFiles.move(StudioClient.directory(),entry,!entry.trash());}catch(Exception e){throw new RuntimeException(e);}},ShotFiles.IO).whenComplete((path,e)->Minecraft.getInstance().execute(()->{
            busy=false;if(!alive)return;if(e!=null){status="Operation failed: "+(e.getCause()==null?e.getMessage():e.getCause().getMessage());return;}
            try{StudioClient.preferences().relocate(entry.path(),path);}catch(Exception ignored){}
            StudioClient.mediaMoved(entry.path(),path,!rename&&!entry.trash());
            selected=null;name="";input.setValue("");scan();
        }));
    }
    @Override public void render(GuiGraphics g,int mx,int my,float dt){
        hits.clear();StudioUi.box(g,0,0,width,height,StudioUi.BG);g.drawString(font,"LIBRARY / MEDIA",8,8,StudioUi.ACCENT,false);
        button(g,"X",width-22,4,18,16,mx,my,this::onClose);
        String[] tabs={"Images","GIF clips","Trash"};int w=(width-16)/3;
        for(int i=0;i<3;i++){int j=i;button(g,tabs[i],8+i*w,25,w-3,17,mx,my,()->{tab=j;page=0;selected=null;});}
        var list=entries.stream().filter(e->tab==2?e.trash():!e.trash()&&(tab==1)==e.path().getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".gif")).toList();
        int slots=Math.max(1,(height-114)/18);page=Math.max(0,Math.min(page,Math.max(0,(list.size()-1)/slots)));
        for(int i=page*slots;i<Math.min(list.size(),(page+1)*slots);i++){var e=list.get(i);int y=45+(i-page*slots)*18;StudioUi.button(g,font,e.path().getFileName().toString(),8,y,width-16,16,mx,my,e.equals(selected));hits.add(new StudioUi.Hit(8,y,width-16,16,()->{selected=e;input.setValue(e.path().getFileName().toString());}));}
        input.render(g,mx,my,dt);
        int q=(width-16)/5,y=height-33;
        button(g,"Prev",8,y,q-2,14,mx,my,()->page=Math.max(0,page-1));
        button(g,"Next",8+q,y,q-2,14,mx,my,()->page++);
        button(g,"Rename",8+q*2,y,q-2,14,mx,my,()->change(true));
        button(g,tab==2?"Restore":"Trash",8+q*3,y,q-2,14,mx,my,()->change(false));
        button(g,"Open",8+q*4,y,q-2,14,mx,my,()->{
            if(selected==null||selected.trash())return;var path=selected.path();
            if(path.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".gif"))net.minecraft.Util.getPlatform().openFile(path.toFile());
            else Minecraft.getInstance().setScreen(new StudioScreen(new ShotFiles.Shot(path,System.currentTimeMillis()),this));
        });
        StudioUi.label(g,font,status+" | "+list.size()+" results",8,height-14,width-16,StudioUi.MUTED);
    }
    @Override public boolean mouseClicked(double x,double y,int button){if(button==0&&input.isMouseOver(x,y))return super.mouseClicked(x,y,button);input.setFocused(false);setFocused(null);if(button==0)for(var h:List.copyOf(hits))if(h.contains(x,y)){h.action().run();return true;}return super.mouseClicked(x,y,button);}
    @Override public boolean isPauseScreen(){return false;}
    @Override public void onClose(){Minecraft.getInstance().setScreen(parent);}
    @Override public void removed(){alive=false;}
}
