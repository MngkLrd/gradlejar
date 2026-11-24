package com.kirill.screenshotmanager.client;
import com.kirill.screenshotmanager.core.ShotFiles;
import com.kirill.screenshotmanager.core.ShotFiles.Shot;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CompletableFuture;

public final class LibraryScreen extends Screen {
    private static final DateTimeFormatter DATE=DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").withZone(ZoneId.systemDefault());
    private final ArrayList<StudioUi.Hit> hits=new ArrayList<>();
    private final Map<Path,StudioTexture> thumbs=new HashMap<>();
    private final Set<Path> requested=new HashSet<>();
    private List<Shot> all=List.of(),filtered=List.of();
    private EditBox search;
    private String query="",status="Loading library...";
    private int page,cols=3,rows=2,tab;
    private boolean alive,ascending;
    private long epoch,scanToken;
    public LibraryScreen(){super(Component.literal("Screenshot Library"));}
    @Override protected void init(){
        alive=true;
        search=new EditBox(font,8,26,Math.max(80,width-16),18,Component.literal("Search screenshots"));
        search.setMaxLength(100);search.setValue(query);search.setResponder(v->{query=v;page=0;filter();});
        addRenderableWidget(search);refresh();
    }
    private void refresh(){
        long token=++scanToken;status="Scanning screenshots...";
        CompletableFuture.supplyAsync(()->{
            try{return ShotFiles.list(StudioClient.directory());}catch(Exception e){throw new RuntimeException(e);}
        },ShotFiles.IO).whenComplete((shots,error)->Minecraft.getInstance().execute(()->{
            if(!alive||token!=scanToken)return;
            if(error!=null){status="Cannot read screenshot directory";return;}
            all=shots;filter();status=all.size()+" screenshots";
        }));
    }
    private void filter(){
        String q=query.toLowerCase(Locale.ROOT);
        filtered=all.stream().filter(s->s.name().toLowerCase(Locale.ROOT).contains(q))
                .filter(s->tab==0||(tab==1&&!edited(s))||(tab==2&&edited(s))).toList();
        if(ascending){ArrayList<Shot> reversed=new ArrayList<>(filtered);Collections.reverse(reversed);filtered=List.copyOf(reversed);}
        page=Math.max(0,Math.min(page,maxPage()));clearThumbs();
    }
    private boolean edited(Shot s){return s.path().getParent().getFileName().toString().equalsIgnoreCase("edited");}
    private int capacity(){return Math.max(1,cols*rows);}
    private int maxPage(){return Math.max(0,(filtered.size()-1)/capacity());}
    private void clearThumbs(){epoch++;thumbs.values().forEach(StudioTexture::close);thumbs.clear();requested.clear();}
    private void request(Shot shot){
        if(!requested.add(shot.path()))return;
        long token=epoch;
        CompletableFuture.supplyAsync(()->{
            try{return ShotFiles.read(shot.path(),256);}catch(Exception e){return null;}
        },ShotFiles.IO).whenComplete((image,error)->Minecraft.getInstance().execute(()->{
            if(!alive||token!=epoch||image==null||error!=null)return;
            try{thumbs.put(shot.path(),new StudioTexture(image));}catch(Exception ignored){}
        }));
    }
    private void button(GuiGraphics g,String label,int x,int y,int w,int h,int mx,int my,boolean active,Runnable action){
        StudioUi.button(g,font,label,x,y,w,h,mx,my,active);hits.add(new StudioUi.Hit(x,y,w,h,action));
    }
    @Override public void render(GuiGraphics g,int mx,int my,float dt){
        hits.clear();
        StudioUi.box(g,0,0,width,height,StudioUi.BG);
        StudioUi.box(g,0,0,width,73,StudioUi.PANEL);
        g.drawString(font,"SCREENSHOT LIBRARY",8,9,StudioUi.ACCENT,false);
        StudioUi.label(g,font,status,140,9,Math.max(1,width-148),StudioUi.MUTED);
        int w=Math.max(34,(width-12)/6);
        button(g,"All",6,50,w-3,18,mx,my,tab==0,()->{tab=0;page=0;filter();});
        button(g,"Originals",6+w,50,w-3,18,mx,my,tab==1,()->{tab=1;page=0;filter();});
        button(g,"Edited",6+2*w,50,w-3,18,mx,my,tab==2,()->{tab=2;page=0;filter();});
        button(g,ascending?"Oldest":"Newest",6+3*w,50,w-3,18,mx,my,false,()->{ascending=!ascending;filter();});
        button(g,"Refresh",6+4*w,50,w-3,18,mx,my,false,this::refresh);
        button(g,"Folder",6+5*w,50,w-3,18,mx,my,false,()->{
            try{Files.createDirectories(StudioClient.directory());Util.getPlatform().openFile(StudioClient.directory().toFile());}catch(Exception e){status="Unable to open folder";}
        });
        int newCols=Math.max(2,Math.min(5,(width-12)/108));
        int newRows=Math.max(1,Math.min(4,(height-101)/82));
        if(newCols!=cols||newRows!=rows){cols=newCols;rows=newRows;page=Math.min(page,maxPage());clearThumbs();}
        int tileW=Math.max(1,(width-12)/cols),tileH=Math.max(1,(height-101)/rows);
        int end=Math.min(filtered.size(),(page+1)*capacity());
        for(int i=page*capacity();i<end;i++){
            Shot shot=filtered.get(i);int slot=i-page*capacity();
            int x=6+(slot%cols)*tileW,y=77+(slot/cols)*tileH;
            int tw=tileW-5,th=tileH-5,previewH=Math.max(8,th-26);
            boolean hover=mx>=x&&mx<x+tw&&my>=y&&my<y+th;
            StudioUi.box(g,x,y,tw,th,hover?0xff354252:StudioUi.PANEL);
            StudioTexture texture=thumbs.get(shot.path());
            if(texture==null){request(shot);g.drawCenteredString(font,"Loading",x+tw/2,y+previewH/2,StudioUi.MUTED);}
            else{
                double scale=Math.min((tw-6)/(double)texture.width,(previewH-4)/(double)texture.height);
                int dw=Math.max(1,(int)(texture.width*scale)),dh=Math.max(1,(int)(texture.height*scale));
                texture.draw(g,x+(tw-dw)/2,y+(previewH-dh)/2,dw,dh);
            }
            StudioUi.label(g,font,shot.name(),x+3,y+previewH+3,tw-6,StudioUi.TEXT);
            StudioUi.label(g,font,DATE.format(Instant.ofEpochMilli(shot.time())),x+3,y+previewH+14,tw-6,StudioUi.MUTED);
            hits.add(new StudioUi.Hit(x,y,tw,th,()->Minecraft.getInstance().setScreen(new StudioScreen(shot,this))));
        }
        if(filtered.isEmpty())g.drawCenteredString(font,"No screenshots. Take one with F2.",width/2,height/2,StudioUi.MUTED);
        StudioUi.box(g,0,height-23,width,23,StudioUi.PANEL);
        button(g,"< Prev",6,height-20,55,17,mx,my,false,()->{if(page>0){page--;clearThumbs();}});
        g.drawCenteredString(font,"Page "+(page+1)+" / "+(maxPage()+1)+"  |  "+filtered.size(),width/2,height-16,StudioUi.MUTED);
        button(g,"Next >",width-61,height-20,55,17,mx,my,false,()->{if(page<maxPage()){page++;clearThumbs();}});
        super.render(g,mx,my,dt);
    }
    @Override public boolean mouseClicked(double x,double y,int button){
        if(button==0)for(StudioUi.Hit h:List.copyOf(hits))if(h.contains(x,y)){h.action().run();return true;}
        return super.mouseClicked(x,y,button);
    }
    @Override public boolean isPauseScreen(){return false;}
    @Override public void removed(){alive=false;scanToken++;clearThumbs();}
}
