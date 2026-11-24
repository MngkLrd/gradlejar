package com.kirill.screenshotmanager.client;

import com.kirill.screenshotmanager.core.ImageDocument;
import com.kirill.screenshotmanager.core.ImageDocument.Tool;
import com.kirill.screenshotmanager.core.ImageDocument.Filter;
import com.kirill.screenshotmanager.core.ShotFiles;
import com.kirill.screenshotmanager.core.ShotFiles.Shot;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.awt.Point;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

public final class StudioScreen extends Screen {
    private static final Tool[] TOOLS=Tool.values();
    private static final String[] TOOL_LABELS={"Pan","Crop","Rect","Ellipse","Arrow","Line","Pen","Text","Highlight","Mosaic","Blur","Redact","Number","Copy area"};
    private static final int[] COLORS={0xffff535e,0xffffd04e,0xff5de2ac,0xff62bafa,0xfff4f6fa,0xff111111};
    private final Shot shot;
    private final Screen parent;
    private final ArrayList<StudioUi.Hit> hits=new ArrayList<>();
    private ImageDocument doc;
    private BufferedImage rendered;
    private StudioTexture texture;
    private EditBox text;
    private String draft="Look here",status="Opening original...";
    private Tool tool=Tool.ARROW;
    private int color=COLORS[0],thickness=6,strength=18,number=1;
    private int cx,cy,cw,ch,ix,iy,iw,ih,sideX,sideW,toolW;
    private double fit=1,zoom=1,panX=0,panY=0,scale=1;
    private boolean alive,busy,dragging,panning,filterPanel;
    private int ax,ay,bx,by;
    private final ArrayList<Point> points=new ArrayList<>();
    private long epoch;
    public StudioScreen(Shot shot,Screen parent){super(Component.literal("Screenshot Studio"));this.shot=shot;this.parent=parent;}

    @Override protected void init(){
        alive=true;
        layout();
        if(text!=null)draft=text.getValue();
        text=new EditBox(font,sideX+7,151,Math.max(20,sideW-14),18,Component.literal("Annotation text"));
        text.setMaxLength(160);text.setValue(draft);text.setResponder(v->draft=v);
        addRenderableWidget(text);
        if(doc==null&&!busy)load();
    }
    private void layout(){
        toolW=width>=440?72:60;
        sideW=width>=560?142:width>=420?116:96;
        sideX=width-sideW;
        cx=toolW+5;cy=47;cw=Math.max(1,sideX-cx-5);ch=Math.max(1,height-cy-23);
        imageBounds();
    }
    private void load(){
        busy=true;long token=++epoch;
        CompletableFuture.supplyAsync(()->{
            try{return ShotFiles.read(shot.path(),0);}
            catch(Exception e){throw new RuntimeException(e);}
        },ShotFiles.IO).whenComplete((img,error)->Minecraft.getInstance().execute(()->{
            if(!alive||token!=epoch)return;
            busy=false;
            if(error!=null){status="Cannot open image: "+message(error);return;}
            doc=new ImageDocument(img);renderDoc(true);
        }));
    }
    private void imageBounds(){
        if(rendered==null)return;
        fit=Math.min(cw/(double)rendered.getWidth(),ch/(double)rendered.getHeight());
        scale=Math.max(.001,fit*zoom);
        iw=Math.max(1,(int)Math.round(rendered.getWidth()*scale));ih=Math.max(1,(int)Math.round(rendered.getHeight()*scale));
        double limitX=Math.max(0,(iw+cw)/2.0-16),limitY=Math.max(0,(ih+ch)/2.0-16);
        panX=Math.max(-limitX,Math.min(limitX,panX));panY=Math.max(-limitY,Math.min(limitY,panY));
        ix=cx+(cw-iw)/2+(int)Math.round(panX);iy=cy+(ch-ih)/2+(int)Math.round(panY);
    }
    private record Result(BufferedImage full,BufferedImage preview){}
    private void renderDoc(boolean resetView){
        if(doc==null)return;
        busy=true;status="Rendering...";long token=++epoch;
        ImageDocument current=doc;var ops=current.snapshot();
        CompletableFuture.supplyAsync(()->{
            BufferedImage full=current.render(ops);
            return new Result(full,ImageDocument.resize(full,2048));
        },ShotFiles.IO).whenComplete((result,error)->Minecraft.getInstance().execute(()->{
            if(!alive||token!=epoch)return;
            busy=false;
            if(error!=null){status="Render failed: "+message(error);return;}
            try{
                StudioTexture next=new StudioTexture(result.preview());
                if(texture!=null)texture.close();
                texture=next;rendered=result.full();
                if(resetView){zoom=1;panX=panY=0;}
                imageBounds();
                status="Ready  |  "+doc.count()+" edit(s)  |  Original protected";
            }catch(Exception e){status="Texture failed: "+message(e);}
        }));
    }
    private static String message(Throwable e){Throwable t=e.getCause()==null?e:e.getCause();return t.getMessage()==null?t.getClass().getSimpleName():t.getMessage();}
    private void apply(ImageDocument.Operation operation,boolean resetView){
        if(doc==null||busy)return;
        if(!doc.add(operation)){status="100 edit limit reached. Export this image to continue.";return;}
        renderDoc(resetView);
    }
    private void undo(){if(doc!=null&&!busy&&doc.canUndo()){doc.undo();renderDoc(false);}}
    private void redo(){if(doc!=null&&!busy&&doc.canRedo()){doc.redo();renderDoc(false);}}
    private void reset(){if(doc!=null&&!busy){doc.reset();number=1;renderDoc(true);}}
    private void output(boolean copy,boolean jpg,int max){
        if(rendered==null||busy)return;
        BufferedImage image=rendered;busy=true;status=copy?"Copying...":"Exporting...";
        long token=++epoch;
        CompletableFuture.supplyAsync(()->{
            try{
                if(copy){ShotFiles.copyImage(image);return "Copied image to clipboard";}
                return "Saved "+ShotFiles.export(StudioClient.directory(),shot,image,jpg,max).getFileName();
            }catch(Exception e){throw new RuntimeException(e);}
        },ShotFiles.IO).whenComplete((value,error)->Minecraft.getInstance().execute(()->{
            if(!alive||token!=epoch)return;busy=false;status=error==null?value:"Export failed: "+message(error);
        }));
    }
    private void action(GuiGraphics g,String name,int x,int y,int w,int h,int mx,int my,boolean selected,Runnable fn){
        StudioUi.button(g,font,name,x,y,w,h,mx,my,selected);hits.add(new StudioUi.Hit(x,y,w,h,fn));
    }
    private void toolbar(GuiGraphics g,int mx,int my){
        String[] names={"Library","Undo","Redo","Reset","Fit","Copy","PNG","JPG","1080"};
        Runnable[] actions={()->Minecraft.getInstance().setScreen(new LibraryScreen()),this::undo,this::redo,this::reset,()->{zoom=1;panX=panY=0;imageBounds();},()->output(true,false,0),()->output(false,false,0),()->output(false,true,0),()->output(false,false,1920)};
        int w=Math.max(22,(width-12)/names.length);
        for(int i=0;i<names.length;i++)action(g,names[i],6+i*w,25,w-3,17,mx,my,false,actions[i]);
    }
    @Override public void render(GuiGraphics g,int mx,int my,float dt){
        hits.clear();layout();
        StudioUi.box(g,0,0,width,height,StudioUi.BG);
        StudioUi.box(g,0,0,width,45,StudioUi.PANEL);
        g.drawString(font,"SCREENSHOT STUDIO",8,8,StudioUi.ACCENT,false);
        StudioUi.label(g,font,shot.name(),137,8,Math.max(0,width-145),StudioUi.MUTED);
        toolbar(g,mx,my);
        StudioUi.box(g,0,46,toolW,height-68,StudioUi.PANEL);
        StudioUi.box(g,sideX,46,sideW,height-68,StudioUi.PANEL);
        StudioUi.box(g,cx,cy,cw,ch,0xff090c10);
        int row=Math.max(9,Math.min(19,(height-72)/TOOLS.length));
        for(int i=0;i<TOOLS.length;i++){
            Tool chosen=TOOLS[i];action(g,TOOL_LABELS[i],4,49+i*row,toolW-8,Math.max(8,row-2),mx,my,tool==chosen,()->{tool=chosen;dragging=false;});
        }
        if(texture!=null){
            g.enableScissor(cx,cy,cx+cw,cy+ch);
            texture.draw(g,ix,iy,iw,ih);
            if(dragging)selection(g);
            g.disableScissor();
        }else g.drawCenteredString(font,busy?"Loading...":"No image",cx+cw/2,cy+ch/2,StudioUi.MUTED);
        int sx=sideX+6,sw=sideW-12;
        g.drawString(font,filterPanel?"ADJUSTMENTS":"PROPERTIES",sx,53,StudioUi.ACCENT,false);
        action(g,filterPanel?"Tools":"Filters",sx,66,sw,15,mx,my,filterPanel,()->filterPanel=!filterPanel);
        if(filterPanel){
            Filter[] fs=Filter.values();
            int rh=Math.max(11,Math.min(19,(height-110)/fs.length));
            for(int i=0;i<fs.length;i++){
                Filter f=fs[i];String n=f.name().substring(0,1)+f.name().substring(1).toLowerCase(java.util.Locale.ROOT);
                action(g,n,sx,85+i*rh,sw,rh-2,mx,my,false,()->apply(ImageDocument.filter(f),f==Filter.ROTATE));
            }
        }else{
            int cell=Math.max(8,(sw-2)/6);
            for(int i=0;i<COLORS.length;i++){
                int chosen=COLORS[i],x=sx+i*cell;
                StudioUi.box(g,x,89,cell-2,15,color==chosen?StudioUi.ACCENT:StudioUi.EDGE);
                StudioUi.box(g,x+1,90,cell-4,13,chosen);
                hits.add(new StudioUi.Hit(x,89,cell-2,15,()->color=chosen));
            }
            g.drawString(font,"Size: "+thickness+" px",sx,110,StudioUi.TEXT,false);
            action(g,"-",sx,122,23,16,mx,my,false,()->thickness=Math.max(1,thickness-2));
            action(g,"+",sx+26,122,23,16,mx,my,false,()->thickness=Math.min(80,thickness+2));
            if(height>=220){
                g.drawString(font,"Text / marker",sx,140,StudioUi.MUTED,false);
                g.drawString(font,"Censor: "+strength,sx,177,StudioUi.TEXT,false);
                action(g,"-",sx,188,23,16,mx,my,false,()->strength=Math.max(3,strength-3));
                action(g,"+",sx+26,188,23,16,mx,my,false,()->strength=Math.min(96,strength+3));
            }
            if(doc!=null&&height>=265){
                g.drawString(font,"HISTORY",sx,213,StudioUi.ACCENT,false);
                List<String> labels=doc.labels();
                int slots=Math.max(0,(height-250)/13);
                for(int i=Math.max(0,labels.size()-slots),r=0;i<labels.size();i++,r++)
                    StudioUi.label(g,font,(i+1)+". "+labels.get(i),sx,226+r*13,sw,StudioUi.MUTED);
            }
        }
        text.visible=!filterPanel&&height>=220;
        StudioUi.box(g,0,height-21,width,21,StudioUi.PANEL);
        String info=rendered==null?"":rendered.getWidth()+" x "+rendered.getHeight()+"  |  "+Math.round(scale*100)+"%";
        StudioUi.label(g,font,status,7,height-15,Math.max(40,width-font.width(info)-22),StudioUi.MUTED);
        g.drawString(font,info,width-font.width(info)-7,height-15,StudioUi.ACCENT,false);
        super.render(g,mx,my,dt);
    }
    private void selection(GuiGraphics g){
        int x1=ix+(int)Math.round(ax*scale),y1=iy+(int)Math.round(ay*scale);
        int x2=ix+(int)Math.round(bx*scale),y2=iy+(int)Math.round(by*scale);
        if(tool==Tool.PEN||tool==Tool.LINE||tool==Tool.ARROW){
            int px=x1,py=y1;
            for(Point p:points){int nx=ix+(int)Math.round(p.x*scale),ny=iy+(int)Math.round(p.y*scale);previewLine(g,px,py,nx,ny);px=nx;py=ny;}
            previewLine(g,px,py,x2,y2);
        }else{
            int x=Math.min(x1,x2),y=Math.min(y1,y2),w=Math.abs(x1-x2)+1,h=Math.abs(y1-y2)+1;
            StudioUi.box(g,x,y,w,h,0x335dc8ed);
            StudioUi.box(g,x,y,w,1,StudioUi.ACCENT);StudioUi.box(g,x,y+h-1,w,1,StudioUi.ACCENT);
            StudioUi.box(g,x,y,1,h,StudioUi.ACCENT);StudioUi.box(g,x+w-1,y,1,h,StudioUi.ACCENT);
        }
    }
    private void previewLine(GuiGraphics g,int x1,int y1,int x2,int y2){
        int steps=Math.min(1024,Math.max(Math.abs(x2-x1),Math.abs(y2-y1)));
        for(int i=0;i<=steps;i++){double t=steps==0?0:i/(double)steps;int x=(int)(x1+(x2-x1)*t),y=(int)(y1+(y2-y1)*t);StudioUi.box(g,x,y,2,2,color);}
    }
    private boolean canvas(double x,double y){return x>=cx&&x<cx+cw&&y>=cy&&y<cy+ch;}
    private boolean image(double x,double y){return rendered!=null&&canvas(x,y)&&x>=ix&&x<ix+iw&&y>=iy&&y<iy+ih;}
    private int px(double x){return Math.max(0,Math.min(rendered.getWidth()-1,(int)((x-ix)/scale)));}
    private int py(double y){return Math.max(0,Math.min(rendered.getHeight()-1,(int)((y-iy)/scale)));}
    private void commitMark(){
        if(doc==null||busy)return;
        if(tool==Tool.COPY_REGION){
            int x=Math.min(ax,bx),y=Math.min(ay,by),w=Math.abs(ax-bx)+1,h=Math.abs(ay-by)+1;
            BufferedImage source=rendered;busy=true;long token=++epoch;
            CompletableFuture.runAsync(()->ShotFiles.copyImage(ImageDocument.crop(source,x,y,w,h)),ShotFiles.IO).whenComplete((v,e)->Minecraft.getInstance().execute(()->{
                if(alive&&epoch==token){busy=false;status=e==null?"Region copied":"Copy failed: "+message(e);}
            }));return;
        }
        if(tool==Tool.TEXT&&draft.isBlank()){status="Enter annotation text";return;}
        if(tool==Tool.CROP&&(Math.abs(ax-bx)<2||Math.abs(ay-by)<2)){status="Select a larger region";return;}
        String value=tool==Tool.NUMBER?Integer.toString(number++):draft;
        apply(ImageDocument.mark(new ImageDocument.Mark(tool,ax,ay,bx,by,color,thickness,strength,value,List.copyOf(points))),tool==Tool.CROP);
    }
    @Override public boolean mouseClicked(double x,double y,int button){
        if(button==0){
            for(StudioUi.Hit hit:List.copyOf(hits))if(hit.contains(x,y)){hit.action().run();return true;}
        }
        if(canvas(x,y)&&rendered!=null&&(button==2||(button==0&&tool==Tool.PAN))){panning=true;return true;}
        if(button==0&&image(x,y)&&!busy){
            text.setFocused(false);setFocused(null);
            ax=bx=px(x);ay=by=py(y);points.clear();
            if(tool==Tool.TEXT||tool==Tool.NUMBER){commitMark();return true;}
            dragging=true;return true;
        }
        return super.mouseClicked(x,y,button);
    }
    @Override public boolean mouseDragged(double x,double y,int button,double dx,double dy){
        if(panning){panX+=dx;panY+=dy;imageBounds();return true;}
        if(dragging&&button==0){bx=px(x);by=py(y);if(tool==Tool.PEN&&points.size()<4096)points.add(new Point(bx,by));return true;}
        return super.mouseDragged(x,y,button,dx,dy);
    }
    @Override public boolean mouseReleased(double x,double y,int button){
        if(panning){panning=false;return true;}
        if(dragging&&button==0){bx=px(x);by=py(y);dragging=false;commitMark();return true;}
        return super.mouseReleased(x,y,button);
    }
    @Override public boolean mouseScrolled(double x,double y,double horizontal,double vertical){
        if(!canvas(x,y)||rendered==null)return super.mouseScrolled(x,y,horizontal,vertical);
        double fx=(x-ix)/scale,fy=(y-iy)/scale;
        zoom=Math.max(.2,Math.min(12,zoom*Math.pow(1.18,vertical)));
        imageBounds();panX+=x-(ix+fx*scale);panY+=y-(iy+fy*scale);imageBounds();return true;
    }
    @Override public boolean keyPressed(int key,int scancode,int mods){
        if(hasControlDown()){
            if(key==GLFW.GLFW_KEY_Z){if(hasShiftDown())redo();else undo();return true;}
            if(key==GLFW.GLFW_KEY_Y){redo();return true;}
            if(key==GLFW.GLFW_KEY_S){output(false,false,0);return true;}
            if(key==GLFW.GLFW_KEY_C&&!text.isFocused()){output(true,false,0);return true;}
        }
        return super.keyPressed(key,scancode,mods);
    }
    @Override public boolean isPauseScreen(){return false;}
    @Override public void onClose(){Minecraft.getInstance().setScreen(parent);}
    @Override public void removed(){alive=false;epoch++;busy=false;dragging=panning=false;if(texture!=null){texture.close();texture=null;}doc=null;rendered=null;}
}
