package com.kirill.screenshotmanager.client;

import com.kirill.screenshotmanager.core.ImageDocument;
import com.kirill.screenshotmanager.core.ProPhoto;
import com.kirill.screenshotmanager.core.AdvancedPhoto;
import com.kirill.screenshotmanager.core.WorkspacePhoto;
import com.kirill.screenshotmanager.core.DepthMap;
import com.kirill.screenshotmanager.core.ProjectStore;
import com.kirill.screenshotmanager.core.CreatorExport;
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

public final class StudioScreen extends StudioBaseScreen {
    private static final Tool[] TOOLS=Tool.values();
    private static final String[] TOOL_LABELS={"Pan","Crop","Rect","Ellipse","Arrow","Line","Pen","Text","Highlight","Mosaic","Blur","Redact","Number","Copy area","Marker","Eraser","Focus blur","Focus shade","Depth focus","Pick color","Fill rect"};
    private static final int[] COLORS={0xffff535e,0xffffd04e,0xff5de2ac,0xff62bafa,0xfff4f6fa,0xff111111};
    private final Shot shot;
    private final Screen parent;
    private final ArrayList<StudioUi.Hit> hits=new ArrayList<>();
    private ImageDocument doc;
    private BufferedImage rendered;
    private StudioTexture texture,comparisonTexture;
    private boolean compareMode;
    private int panel,selected=-1,layerScroll,toolScroll,adjustScroll,exportQuality=90,exportSide=0,exportBudget=0;
    private boolean exportJpeg,renderedCurrent;
    private final int[] grade=new int[6];
    private static final String[] GRADE_NAMES={"Brightness","Contrast","Saturation","Warmth","Gamma","Vignette"};
    private boolean movingLayer;private int moveX,moveY;
    private EditBox text;
    private boolean textPending,glass,peek,confirmReset;
    private int textX,textY,opacity=33,focusRadius=25,focusAmount=65,gradeMode;
    private final int[] tone=new int[ProPhoto.TONE_COUNT],effects=new int[ProPhoto.EFFECT_COUNT];
    private static final String[] TONE_NAMES={"Shadows","Midtones","Highlights","Black point","White clip","Gamma","RGB low","RGB mid","RGB high","Red low","Red high","Green low","Green high","Blue low","Blue high","Saturation","Vibrance"};
    private static final String[] EFFECT_NAMES={"Bloom","Film grain","Color fringe","Vignette","Sharpen","Atmosphere","Letterbox"};
    private final ArrayList<StudioSlider> sliders=new ArrayList<>();
    private StudioSlider activeSlider;
    private final int[][] advanced=java.util.Arrays.stream(AdvancedPhoto.Kind.values()).map(k->new int[k.count]).toArray(int[][]::new);
    private static final String[] MODES={"Basic","Tone / Curves","Cinematic","HSL mixer","Color wheels","Tilt Shift","Clarity / Dehaze","Gradient","RGB curves","3-way wheels","Perspective"};
    private static final String[][] ADV_NAMES={
        {"Red hue","Red sat","Red light","Yellow hue","Yellow sat","Yellow light","Green hue","Green sat","Green light","Cyan hue","Cyan sat","Cyan light","Blue hue","Blue sat","Blue light","Magenta hue","Magenta sat","Magenta light"},
        {"Shadow warmth","Shadow tint","Mid warmth","Mid tint","Light warmth","Light tint"},
        {"Focus position","Band width","Angle","Blur","Feather","Saturation"},
        {"Clarity","Dehaze","Saturation","Exposure"},
        {"Position","Angle","Feather","Exposure","Warmth"}
    };
    private StudioTexture liveTexture;
    private int[][] curves=WorkspacePhoto.identity(),histogram=new int[4][256],liveHistogram;
    private final int[] wheels=new int[9],perspective=new int[2];
    private int curveChannel,wheelChannel,curveNode=-1,graphX,graphY,graphW,graphH,wheelX,wheelY,wheelR;
    private boolean wheelDrag,compareDrag,resizePanel,workspaceWide;
    private double split=.5;
    private int layerOpacity=100,layerPage,preferredSide=0,fontStyle=1;
    private WorkspacePhoto.Blend layerBlend=WorkspacePhoto.Blend.NORMAL;
    private EditBox hexBox;
    private String fontFamily="SansSerif";
    private String[] fonts={"SansSerif","Serif","Monospaced","Dialog"};
    private int fontIndex;
    private boolean fontsLoaded;
    private int layerEditing=-1;
    private boolean draftDirty,previewRunning,autoSave=true;
    private long previewRevision,previewDue;
    private int captionStyle=1,captionAngle,canvasPreset,toolPage=1,optionsPage;
    private static final String[] GROUPS={"Navigate","Draw","Privacy","Focus"};
    private static final int[][] GROUP_TOOLS={{0,1,13,19},{2,3,4,5,6,7,8,12,14,15,20},{9,10,11},{16,17,18}};
    private static final int[][] CANVAS={{1280,720},{840,473},{512,512},{1920,1080},{1080,1920},{2560,1080}};
    private int editColorChannel;
    private static final java.util.concurrent.ExecutorService AUTOSAVE=java.util.concurrent.Executors.newSingleThreadExecutor(r->{Thread t=new Thread(r,"sm-autosave");t.setDaemon(true);return t;});
    private int imageSize=100,imageAngle=0,imageOpacity=100,importIndex=-1;
    private java.nio.file.Path importPath;
    private String draft="",status="Opening original...";
    private Tool tool=Tool.ARROW;
    private int color=COLORS[0],thickness=6,strength=18,number=1;
    private int cx,cy,cw,ch,ix,iy,iw,ih,sideX,sideW,toolW;
    private double fit=1,zoom=1,panX=0,panY=0,scale=1;
    private boolean alive,busy,dragging,panning;
    private int ax,ay,bx,by;
    private final ArrayList<Point> points=new ArrayList<>();
    private long epoch;
    public StudioScreen(Shot shot,Screen parent){super(Component.literal("Screenshot Studio"));this.shot=shot;this.parent=parent;}

    @Override protected void init(){
        alive=true;
        glass=StudioClient.preferences().number("studio.glass",0,0,1)==1;
        layout();
        if(text!=null)draft=text.getValue();
        text=new EditBox(font,sideX+7,151,Math.max(20,sideW-14),18,Component.literal("Annotation text"));
        text.setMaxLength(160);text.setHint(Component.literal("Type annotation"));text.setValue(draft);text.setResponder(v->draft=v);
        addRenderableWidget(text);text.setFocused(false);setFocused(null);
        hexBox=new EditBox(font,sideX+6,112,Math.max(30,sideW-12),17,Component.literal("HEX color"));
        hexBox.setMaxLength(7);hexBox.setValue(String.format("#%06X",color&0xffffff));hexBox.visible=false;addRenderableWidget(hexBox);
        if(!fontsLoaded){fontsLoaded=true;try{var names=java.awt.GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames();var all=new java.util.LinkedHashSet<String>(java.util.List.of(fonts));all.addAll(java.util.Arrays.asList(names));fonts=all.toArray(String[]::new);}catch(Exception ignored){}}
        if(doc==null&&!busy)load();
    }
    private void layout(){
        toolW=width>=440?70:52;
        sideW=preferredSide>0?Math.max(104,Math.min(width/2,preferredSide)):width>=560?166:width>=420?140:104;
        if(workspaceWide){toolW=0;sideW=0;}
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
    private record Result(BufferedImage full,BufferedImage preview,int[][] histogram){}
    private void renderDoc(boolean resetView){
        if(doc==null)return;
        clearPreview();compareMode=false;
        renderedCurrent=false;busy=true;status="Rendering...";long token=++epoch;
        ImageDocument current=doc;var ops=current.snapshot();
        CompletableFuture.supplyAsync(()->{
            BufferedImage full=current.render(ops);
            return new Result(full,ImageDocument.resize(full,2048),WorkspacePhoto.histogram(ImageDocument.resize(full,768)));
        },ShotFiles.IO).whenComplete((result,error)->Minecraft.getInstance().execute(()->{
            if(!alive||token!=epoch)return;
            busy=false;
            if(error!=null){status="Render failed: "+message(error);return;}
            try{
                StudioTexture next=new StudioTexture(result.preview());
                if(texture!=null)texture.close();
                if(comparisonTexture!=null){comparisonTexture.close();comparisonTexture=null;}
                selected=Math.min(selected,doc.count()-1);
                texture=next;rendered=result.full();histogram=result.histogram();renderedCurrent=true;
                if(resetView){zoom=1;panX=panY=0;}
                imageBounds();
                status="Ready  |  "+doc.count()+" edit(s)  |  Original protected";
            }catch(Exception e){status="Texture failed: "+message(e);}
        }));
    }
    private static String message(Throwable e){Throwable t=e.getCause()==null?e:e.getCause();return t.getMessage()==null?t.getClass().getSimpleName():t.getMessage();}
    private void apply(ImageDocument.Operation operation,boolean resetView){
        if(compareMode)return;
        if(doc==null||busy)return;
        confirmReset=false;
        if(!doc.add(operation)){status="100 edit limit reached. Export this image to continue.";return;}
        selected=doc.count()-1;saveRecovery();renderDoc(resetView);
    }
    private void undo(){if(doc!=null&&!busy&&doc.canUndo()){doc.undo();saveRecovery();renderDoc(false);}}
    private void redo(){if(doc!=null&&!busy&&doc.canRedo()){doc.redo();saveRecovery();renderDoc(false);}}
    private void reset(){if(doc!=null&&!busy&&doc.count()>0){
        if(!confirmReset){confirmReset=true;status="Click Undo all again to clear edits (reversible)";return;}
        confirmReset=false;doc.reset();number=1;selected=-1;saveRecovery();renderDoc(true);
    }}
    private void output(boolean copy,boolean jpg,int max){
        if(rendered==null||busy||!renderedCurrent)return;
        if(draftDirty){status="Apply or cancel preview before export";return;}
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
        String[] names={"Library","Undo","Redo","Undo all","Fit","Copy","PNG","JPG","Compare","1080"};
        Runnable[] actions={()->Minecraft.getInstance().setScreen(new LibraryScreen()),this::undo,this::redo,this::reset,()->{zoom=1;panX=panY=0;imageBounds();},()->output(true,false,0),()->output(false,false,0),()->output(false,true,0),this::compare,()->output(false,false,1920)};
        int w=Math.max(12,(width-12)/names.length);
        for(int i=0;i<names.length;i++)action(g,names[i],6+i*w,25,w-3,17,mx,my,false,actions[i]);
    }
    @Override public void render(GuiGraphics g,int mx,int my,float dt){
        hits.clear();sliders.clear();graphW=wheelR=0;layout();
        StudioUi.box(g,0,0,width,height,glass?0x38101520:StudioUi.BG);
        StudioUi.box(g,0,0,width,45,glass?0x951b2736:StudioUi.PANEL);
        g.drawString(font,"SCREENSHOT STUDIO",8,8,StudioUi.ACCENT,false);
        StudioUi.label(g,font,shot.name(),137,8,Math.max(0,width-260),StudioUi.MUTED);
        action(g,glass?"Glass":"Solid",width-112,4,45,16,mx,my,glass,()->{glass=!glass;peek=false;try{StudioClient.preferences().number("studio.glass",glass?1:0);}catch(Exception ignored){}});
        action(g,peek?"Show":"Peek",width-64,4,38,16,mx,my,peek,()->{glass=true;peek=!peek;});
        action(g,"X",width-23,4,19,16,mx,my,false,this::onClose);
        toolbar(g,mx,my);
        StudioUi.box(g,0,46,toolW,height-68,glass?0x971b2736:StudioUi.PANEL);
        StudioUi.box(g,sideX,46,sideW,height-68,glass?0x971b2736:StudioUi.PANEL);
        StudioUi.box(g,cx,cy,cw,ch,glass?0x18090c10:0xff090c10);
        if(!workspaceWide){action(g,GROUPS[toolPage],4,49,toolW-8,15,mx,my,false,()->{toolPage=(toolPage+1)%GROUPS.length;toolScroll=0;});
        int row=16,visibleTools=Math.max(1,(height-92)/row);int[] group=GROUP_TOOLS[toolPage];
        toolScroll=Math.max(0,Math.min(toolScroll,group.length-visibleTools));
        for(int i=toolScroll;i<Math.min(group.length,toolScroll+visibleTools);i++){
            int index=group[i];Tool chosen=TOOLS[index];action(g,TOOL_LABELS[index],4,68+(i-toolScroll)*row,toolW-8,14,mx,my,tool==chosen,()->selectTool(chosen));
        }
        }
        if(texture!=null&&!peek){
            g.enableScissor(cx,cy,cx+cw,cy+ch);
            if(compareMode&&comparisonTexture!=null){
                texture.draw(g,ix,iy,iw,ih);
                int divider=cx+(int)Math.round(cw*split);
                if(divider>cx){
                    g.enableScissor(cx,cy,divider,cy+ch);comparisonTexture.draw(g,ix,iy,iw,ih);g.disableScissor();
                }
                StudioUi.box(g,divider-1,cy,2,ch,0xffffffff);
                StudioUi.box(g,divider-9,cy+ch/2-8,18,16,0xff273746);
                g.drawCenteredString(font,"<>",divider,cy+ch/2-4,0xffffffff);
                StudioUi.label(g,font,"BEFORE",cx+5,cy+5,Math.max(1,cw/2-10),0xffffffff);
                StudioUi.label(g,font,"AFTER",cx+cw/2,cy+5,Math.max(1,cw/2-8),0xffffffff);
            }else{
                (liveTexture!=null&&panel==2&&draftDirty?liveTexture:texture).draw(g,ix,iy,iw,ih);
                if(dragging)selection(g);
                if(textPending)StudioUi.label(g,font,draft.isBlank()?"Type text, Enter to place":draft,ix+(int)(textX*scale),iy+(int)(textY*scale),Math.max(20,cw),0xffffffff);
                if(brush()&&image(mx,my))brushCursor(g,mx,my);
                if(panel==1&&movingLayer) {
                    StudioUi.label(g,font,"Moving layer "+(selected+1),cx+6,cy+6,cw-12,StudioUi.ACCENT);
                }
            }
            g.disableScissor();
        }else if(!peek)g.drawCenteredString(font,busy?"Loading...":"No image",cx+cw/2,cy+ch/2,StudioUi.MUTED);
        if(!workspaceWide){
        StudioUi.box(g,sideX-3,66,2,Math.max(1,height-88),StudioUi.EDGE);
        int sx=sideX+6,sw=sideW-12;
        String[] tabs={"Tools","Layers","Color","File"};
        int tabWidth=sw/4;
        for(int i=0;i<4;i++){int chosen=i;action(g,tabs[i],sx+i*tabWidth,50,tabWidth-2,15,mx,my,panel==i,()->{if(panel!=chosen)clearPreview();panel=chosen;dragging=false;textPending=false;clearTextFocus();});}
        if(panel==0)drawTools(g,mx,my,sx,sw);
        else if(panel==1)drawLayers(g,mx,my,sx,sw);
        else if(panel==2)drawGrade(g,mx,my,sx,sw);
        else drawFile(g,mx,my,sx,sw);
        }
        text.visible=!workspaceWide&&panel==0&&optionsPage==0&&!compareMode&&needsText();
        hexBox.visible=!workspaceWide&&panel==0&&optionsPage==1&&!compareMode&&!(selected>=0&&doc!=null&&selected<doc.count()&&ImageDocument.unwrap(doc.snapshot().get(selected)) instanceof ProPhoto.ImageLayer);
        if(hexBox.visible){syncHex();hexBox.render(g,mx,my,dt);}else if(hexBox.isFocused())clearTextFocus();
        if(!text.visible&&text.isFocused())clearTextFocus();
        StudioUi.box(g,0,height-21,width,21,glass?0x951b2736:StudioUi.PANEL);
        String info=rendered==null?"":rendered.getWidth()+" x "+rendered.getHeight()+"  |  "+Math.round(scale*100)+"%";
        StudioUi.label(g,font,status,7,height-15,Math.max(40,width-font.width(info)-22),StudioUi.MUTED);
        g.drawString(font,info,width-font.width(info)-7,height-15,StudioUi.ACCENT,false);
        if(text.visible)text.render(g,mx,my,dt);
        if(!workspaceWide&&panel==2&&gradeMode!=8&&gradeMode!=9&&height>=290){WorkspaceControls.histogram(g,sideX+7,height-89,sideW-14,18,liveHistogram==null?histogram:liveHistogram,0);}
    }
    private void selectTool(Tool chosen){clearPreview();tool=chosen;selected=-1;dragging=false;textPending=false;panel=0;optionsPage=0;clearTextFocus();for(int i=0;i<GROUP_TOOLS.length;i++)for(int id:GROUP_TOOLS[i])if(id==chosen.ordinal()){toolPage=i;toolScroll=0;}}
    private boolean needsText(){
        return tool==Tool.TEXT||(doc!=null&&selected>=0&&selected<doc.count()&&ImageDocument.unwrap(doc.snapshot().get(selected)) instanceof WorkspacePhoto.Text)||(doc!=null&&selected>=0&&selected<doc.count()&&ImageDocument.unwrap(doc.snapshot().get(selected)) instanceof AdvancedPhoto.Caption)||(doc!=null&&selected>=0&&selected<doc.count()&&ImageDocument.unwrap(doc.snapshot().get(selected)) instanceof ImageDocument.MarkOperation m&&m.value().tool()==Tool.TEXT);
    }
    private void clearTextFocus(){text.setFocused(false);if(hexBox!=null)hexBox.setFocused(false);setFocused(null);}
    private void syncHex(){if(hexBox!=null&&!hexBox.isFocused())hexBox.setValue(String.format("#%06X",color&0xffffff));}
    private void applyHex(){try{color=WorkspacePhoto.hex(hexBox.getValue());clearTextFocus();syncHex();status="Color updated";}catch(IllegalArgumentException e){status="Enter 6 HEX digits, e.g. #FFAA00";}}
    private void slider(GuiGraphics g,String label,int x,int y,int w,int min,int max,int value,int reset,java.util.function.IntConsumer setter){
        StudioSlider slider=new StudioSlider(x,y+11,w-2,12,min,max,reset,setter);
        sliders.add(slider);slider.draw(g,font,label,value);
    }
    private boolean brush(){return tool==Tool.PEN||tool==Tool.HIGHLIGHT_BRUSH||tool==Tool.ERASER;}
    private void brushCursor(GuiGraphics g,int mx,int my){
        int r=Math.max(2,Math.min(80,(int)Math.round(thickness*scale/2)));
        for(int i=0;i<48;i++){double a=2*Math.PI*i/48;int x=mx+(int)Math.round(Math.cos(a)*r),y=my+(int)Math.round(Math.sin(a)*r);StudioUi.box(g,x,y,1,1,0xffffffff);}
    }
    private void drawTools(GuiGraphics g,int mx,int my,int sx,int sw){
        int y=73;
        if(doc!=null&&selected>=0&&selected<doc.count()&&ImageDocument.unwrap(doc.snapshot().get(selected)) instanceof ProPhoto.ImageLayer){
            slider(g,"Scale "+imageSize+"%",sx,y,sw,10,200,imageSize,100,v->imageSize=v);y+=27;
            slider(g,"Rotate "+imageAngle,sx,y,sw,-180,180,imageAngle,0,v->imageAngle=v);y+=27;
            slider(g,"Opacity "+imageOpacity+"%",sx,y,sw,0,100,imageOpacity,100,v->imageOpacity=v);y+=27;
            action(g,"Apply transform",sx,y,sw,16,mx,my,false,this::applyStyle);return;
        }
        action(g,optionsPage==0?"Tool settings":"Color / style",sx,y,sw,14,mx,my,false,()->{optionsPage=1-optionsPage;clearTextFocus();});y+=18;
        if(needsText()&&optionsPage==0){
            StudioUi.label(g,font,textPending?"Enter to place":"Click image to place",sx,y,sw,StudioUi.MUTED);
            text.setX(sx);text.setY(y+12);text.setWidth(sw);y+=34;
        }
        if(optionsPage==0){
            slider(g,"Size "+thickness+" px",sx,y,sw,1,80,thickness,6,v->thickness=v);y+=27;
            if(tool==Tool.HIGHLIGHT||tool==Tool.HIGHLIGHT_BRUSH){slider(g,"Opacity "+opacity+"%",sx,y,sw,5,99,opacity,33,v->opacity=v);y+=27;}
            if(tool==Tool.BLUR||tool==Tool.PIXELATE){slider(g,"Censor "+strength,sx,y,sw,3,96,strength,18,v->strength=v);y+=27;}
            if(tool==Tool.FOCUS||tool==Tool.FOCUS_SHADE||tool==Tool.DEPTH_FOCUS){
                slider(g,(tool==Tool.DEPTH_FOCUS?"Focus range ":"Radius ")+focusRadius+"%",sx,y,sw,5,100,focusRadius,25,v->focusRadius=v);y+=27;
                slider(g,"Strength "+focusAmount+"%",sx,y,sw,0,100,focusAmount,65,v->focusAmount=v);y+=27;
            }
            if(needsText()&&y+14<height-22){
                action(g,new String[]{"Plain","Outline","Shadow","Label"}[captionStyle%4],sx,y,sw,14,mx,my,false,()->captionStyle=(captionStyle+1)%4);y+=18;
            }
        }else{
            int cell=Math.max(8,(sw-2)/6);
            for(int i=0;i<COLORS.length;i++){
                int chosen=COLORS[i],xx=sx+i*cell,yy=y;StudioUi.box(g,xx,y,cell-2,15,color==chosen?StudioUi.ACCENT:StudioUi.EDGE);StudioUi.box(g,xx+1,y+1,cell-4,13,chosen);
                hits.add(new StudioUi.Hit(xx,yy,cell-2,15,()->color=chosen));
            }
            y+=19;
            hexBox.setX(sx);hexBox.setY(y);hexBox.setWidth(sw-28);
            action(g,"Set",sx+sw-26,y,26,17,mx,my,false,this::applyHex);y+=21;
            action(g,"RGB: "+new String[]{"R","G","B"}[editColorChannel],sx,y,sw,13,mx,my,false,()->editColorChannel=(editColorChannel+1)%3);y+=16;
            int shift=(2-editColorChannel)*8;
            slider(g,"Channel "+((color>>>shift)&255),sx,y,sw,0,255,(color>>>shift)&255,255,v->color=(color&~(255<<shift))|(v<<shift));y+=27;
            if(needsText()){action(g,"<",sx,y,16,14,mx,my,false,()->cycleFont(-1));action(g,fontFamily,sx+18,y,sw-36,14,mx,my,false,()->cycleFont(1));action(g,">",sx+sw-16,y,16,14,mx,my,false,()->cycleFont(1));y+=17;
                action(g,new String[]{"Regular","Bold","Italic","Bold italic"}[fontStyle],sx,y,sw,14,mx,my,false,()->fontStyle=(fontStyle+1)%4);y+=17;
                if(y+25<height-22){slider(g,"Text angle "+captionAngle,sx,y,sw,-180,180,captionAngle,0,v->captionAngle=v);y+=27;}}
        }
        if(y+16<height-22){
            action(g,"Rotate",sx,y,sw/2-2,16,mx,my,false,()->apply(ImageDocument.filter(Filter.ROTATE),true));
            action(g,"Flip",sx+sw/2,y,sw/2-2,16,mx,my,false,()->apply(ImageDocument.filter(Filter.FLIP),true));
        }
    }
    private void cycleFont(int dir){fontIndex=Math.floorMod(fontIndex+dir,fonts.length);fontFamily=fonts[fontIndex];}
    private void changed(boolean success){if(success){selected=Math.min(selected,doc.count()-1);saveRecovery();renderDoc(false);}else status="Layer cannot be changed across crop / rotate";}
    private void drawLayers(GuiGraphics g,int mx,int my,int sx,int sw){
        if(doc==null)return;
        var ops=doc.snapshot();
        action(g,layerPage==0?"Layers "+ops.size():"Blend / opacity",sx,70,sw,14,mx,my,false,()->layerPage=1-layerPage);
        if(layerPage==1){drawComposite(g,mx,my,sx,sw);return;}
        int slots=Math.max(1,(height-187)/15);
        layerScroll=Math.max(0,Math.min(layerScroll,Math.max(0,ops.size()-slots)));
        for(int r=0;r<Math.min(slots,ops.size()-layerScroll);r++){
            int i=ops.size()-1-r-layerScroll,y=86+r*15;var op=ops.get(i);
            action(g,(ImageDocument.geometry(op)?"LOCK ":ImageDocument.visible(op)?"ON ":"OFF ")+op.label(),sx,y,sw,13,mx,my,i==selected,()->{
                clearPreview();selected=i;layerOpacity=ImageDocument.opacity(op);layerBlend=ImageDocument.blend(op);layerEditing=-1;
                if(ImageDocument.unwrap(op) instanceof ImageDocument.MarkOperation mark){
                    var m=mark.value();color=m.color()|0xff000000;thickness=m.thickness();strength=m.strength();draft=m.text();text.setValue(draft);if(m.tool()==Tool.HIGHLIGHT||m.tool()==Tool.HIGHLIGHT_BRUSH)opacity=(m.color()>>>24)==255?33:Math.max(5,(m.color()>>>24)*100/255);
                }else if(ImageDocument.unwrap(op) instanceof ImageDocument.Adjustment gr){
                    grade[0]=gr.brightness();grade[1]=gr.contrast();grade[2]=gr.saturation();grade[3]=gr.warmth();grade[4]=gr.gamma();grade[5]=gr.vignette();gradeMode=0;
                }else if(ImageDocument.unwrap(op) instanceof ProPhoto.Tone t){System.arraycopy(t.values(),0,tone,0,tone.length);gradeMode=1;}
                else if(ImageDocument.unwrap(op) instanceof ProPhoto.Effects e){System.arraycopy(e.values(),0,effects,0,effects.length);gradeMode=2;}
                else if(ImageDocument.unwrap(op) instanceof ProPhoto.ImageLayer im){imageSize=im.size();imageAngle=im.rotation();imageOpacity=im.opacity();}
                else if(ImageDocument.unwrap(op) instanceof AdvancedPhoto.Adjust ad){gradeMode=3+ad.kind().ordinal();System.arraycopy(ad.values(),0,advanced[ad.kind().ordinal()],0,ad.kind().count);}
                else if(ImageDocument.unwrap(op) instanceof WorkspacePhoto.Curves c){curves=c.nodes();gradeMode=8;}
                else if(ImageDocument.unwrap(op) instanceof WorkspacePhoto.Wheels c){System.arraycopy(c.values(),0,wheels,0,9);gradeMode=9;}
                else if(ImageDocument.unwrap(op) instanceof WorkspacePhoto.Text c){draft=c.text();text.setValue(draft);color=c.color();thickness=Math.max(1,Math.min(80,c.size()/4));captionStyle=c.style();captionAngle=c.angle();fontFamily=c.family();fontStyle=c.fontStyle();int idx=java.util.Arrays.asList(fonts).indexOf(fontFamily);if(idx>=0)fontIndex=idx;}
                else if(ImageDocument.unwrap(op) instanceof AdvancedPhoto.Caption c){draft=c.text();text.setValue(draft);color=c.color();thickness=Math.max(1,Math.min(80,c.size()/4));captionStyle=c.style();captionAngle=c.angle();}
            });
        }
        int y=height-100,w=sw/3;
        action(g,"On/Off",sx,y,w-2,15,mx,my,false,()->{if(!busy&&!compareMode)changed(doc.toggle(selected));});
        action(g,"Up",sx+w,y,w-2,15,mx,my,false,()->{if(!busy&&!compareMode){boolean ok=doc.move(selected,1);if(ok)selected++;changed(ok);}});
        action(g,"Down",sx+2*w,y,w-2,15,mx,my,false,()->{if(!busy&&!compareMode){boolean ok=doc.move(selected,-1);if(ok)selected--;changed(ok);}});
        action(g,"Delete",sx,y+17,sw/2-2,15,mx,my,false,()->{if(!busy&&!compareMode)changed(doc.delete(selected));});
        action(g,"Apply style",sx+sw/2,y+17,sw/2-2,15,mx,my,false,this::applyStyle);
        action(g,"Duplicate",sx,y+34,sw,15,mx,my,false,()->{if(!busy&&!compareMode){boolean ok=doc.duplicate(selected);if(ok)selected++;changed(ok);}});
        action(g,"Edit selected",sx,y+51,sw,15,mx,my,false,()->{if(selected>=0&&selected<doc.count()){var op=ImageDocument.unwrap(doc.snapshot().get(selected));if(op instanceof WorkspacePhoto.Curves||op instanceof WorkspacePhoto.Wheels||op instanceof ImageDocument.Adjustment||op instanceof ProPhoto.Tone||op instanceof ProPhoto.Effects||op instanceof AdvancedPhoto.Adjust){panel=2;layerEditing=selected;}else{panel=0;optionsPage=0;}}});
    }
    private void drawComposite(GuiGraphics g,int mx,int my,int sx,int sw){
        if(selected<0||selected>=doc.count()){StudioUi.label(g,font,"Select a layer first",sx,93,sw,StudioUi.MUTED);return;}
        var op=doc.snapshot().get(selected);
        StudioUi.label(g,font,op.label(),sx,90,sw,StudioUi.TEXT);
        if(ImageDocument.geometry(op)||ImageDocument.unwrap(op) instanceof ImageDocument.MarkOperation m&&(m.value().tool()==Tool.REDACT||m.value().tool()==Tool.ERASER)){
            StudioUi.label(g,font,"Protected operation",sx,108,sw,StudioUi.MUTED);return;
        }
        slider(g,"Opacity "+layerOpacity+"%",sx,108,sw,0,100,layerOpacity,100,v->layerOpacity=v);
        action(g,"<",sx,138,16,16,mx,my,false,()->layerBlend=WorkspacePhoto.Blend.values()[Math.floorMod(layerBlend.ordinal()-1,WorkspacePhoto.Blend.values().length)]);
        action(g,layerBlend.name(),sx+18,138,sw-36,16,mx,my,false,()->layerBlend=WorkspacePhoto.Blend.values()[(layerBlend.ordinal()+1)%WorkspacePhoto.Blend.values().length]);
        action(g,">",sx+sw-16,138,16,16,mx,my,false,()->layerBlend=WorkspacePhoto.Blend.values()[(layerBlend.ordinal()+1)%WorkspacePhoto.Blend.values().length]);
        action(g,"Apply blend",sx,158,sw,16,mx,my,false,()->{if(!busy&&!compareMode)changed(doc.composite(selected,layerOpacity,layerBlend));});
    }
    private void applyStyle(){
        if(doc==null||busy||compareMode||selected<0||selected>=doc.count())return;
        var op=ImageDocument.unwrap(doc.snapshot().get(selected));
        if(op instanceof ImageDocument.MarkOperation mark){
            var m=mark.value();
            changed(doc.replace(selected,ImageDocument.mark(new ImageDocument.Mark(m.tool(),m.x1(),m.y1(),m.x2(),m.y2(),markColor(m.tool()),thickness,strength,m.tool()==Tool.TEXT?draft:m.text(),m.points()))));
        }else if(op instanceof ImageDocument.Adjustment)changed(doc.replace(selected,currentGrade()));
        else if(op instanceof ProPhoto.Tone)changed(doc.replace(selected,new ProPhoto.Tone(tone)));
        else if(op instanceof ProPhoto.Effects)changed(doc.replace(selected,new ProPhoto.Effects(effects)));
        else if(op instanceof ProPhoto.ImageLayer im)changed(doc.replace(selected,im.transform(im.x(),im.y(),imageSize,imageAngle,imageOpacity)));
        else if(op instanceof AdvancedPhoto.Adjust ad)changed(doc.replace(selected,new AdvancedPhoto.Adjust(ad.kind(),advanced[ad.kind().ordinal()])));
        else if(op instanceof WorkspacePhoto.Text c)changed(doc.replace(selected,new WorkspacePhoto.Text(draft,fontFamily,c.x(),c.y(),thickness*4,color,captionStyle,captionAngle,fontStyle)));
        else if(op instanceof WorkspacePhoto.Curves)changed(doc.replace(selected,new WorkspacePhoto.Curves(curves)));
        else if(op instanceof WorkspacePhoto.Wheels)changed(doc.replace(selected,new WorkspacePhoto.Wheels(wheels)));
        else if(op instanceof AdvancedPhoto.Caption c)changed(doc.replace(selected,new AdvancedPhoto.Caption(draft,c.x(),c.y(),thickness*4,color,captionStyle,captionAngle)));
        else status="Select an annotation or color layer";
    }
    private ImageDocument.Adjustment currentGrade(){return new ImageDocument.Adjustment(grade[0],grade[1],grade[2],grade[3],grade[4],grade[5]);}
    private ImageDocument.Operation draftOperation(){
        if(gradeMode==8)return new WorkspacePhoto.Curves(curves);
        if(gradeMode==9)return new WorkspacePhoto.Wheels(wheels);
        if(gradeMode==10)return new WorkspacePhoto.Perspective(perspective[0],perspective[1]);
        return gradeMode==0?currentGrade():gradeMode==1?new ProPhoto.Tone(tone):gradeMode==2?new ProPhoto.Effects(effects):new AdvancedPhoto.Adjust(AdvancedPhoto.Kind.values()[gradeMode-3],advanced[gradeMode-3]);
    }
    private void clearPreview(){
        draftDirty=false;previewRevision++;liveHistogram=null;
        if(liveTexture!=null){liveTexture.close();liveTexture=null;}
    }
    private void requestPreview(){
        if(busy||rendered==null||compareMode||panel!=2)return;
        draftDirty=true;previewRevision++;previewDue=System.nanoTime();
        status="Live preview | Apply to keep / Cancel to discard";
    }
    @Override public void tick(){
        super.tick();
        if(!draftDirty||previewRunning||busy||rendered==null||panel!=2||System.nanoTime()<previewDue)return;
        long rev=previewRevision,token=epoch;BufferedImage base=rendered;var op=draftOperation();
        previewRunning=true;
        CompletableFuture.supplyAsync(()->{var img=op.apply(ImageDocument.copy(ImageDocument.resize(base,768)));return new Result(img,img,WorkspacePhoto.histogram(img));},ShotFiles.IO)
            .whenComplete((img,error)->Minecraft.getInstance().execute(()->{
                previewRunning=false;
                if(!alive||epoch!=token||!draftDirty||panel!=2)return;
                if(error!=null){status="Preview failed";clearPreview();return;}
                try{var next=new StudioTexture(img.preview());if(liveTexture!=null)liveTexture.close();liveTexture=next;liveHistogram=img.histogram();}
                catch(Exception e){clearPreview();return;}
                if(rev==previewRevision)previewDue=Long.MAX_VALUE;
            }));
    }
    private void applyDraft(){
        if(busy||doc==null)return;
        var op=draftOperation();clearPreview();if(layerEditing>=0&&layerEditing<doc.count()&&!ImageDocument.geometry(op)){changed(doc.replace(layerEditing,op));}else apply(op,ImageDocument.geometry(op));layerEditing=-1;
        java.util.Arrays.fill(grade,0);java.util.Arrays.fill(tone,0);java.util.Arrays.fill(effects,0);for(int[] v:advanced)java.util.Arrays.fill(v,0);curves=WorkspacePhoto.identity();java.util.Arrays.fill(wheels,0);java.util.Arrays.fill(perspective,0);
    }
    private void drawGrade(GuiGraphics g,int mx,int my,int sx,int sw){
        int w=sw/3;
        action(g,"<",sx,68,17,15,mx,my,false,()->{clearPreview();layerEditing=-1;gradeMode=(gradeMode+MODES.length-1)%MODES.length;adjustScroll=0;});
        action(g,MODES[gradeMode],sx+19,68,sw-38,15,mx,my,false,()->{clearPreview();layerEditing=-1;gradeMode=(gradeMode+1)%MODES.length;adjustScroll=0;});
        action(g,">",sx+sw-17,68,17,15,mx,my,false,()->{clearPreview();layerEditing=-1;gradeMode=(gradeMode+1)%MODES.length;adjustScroll=0;});
        if(gradeMode>=8){drawWorkspaceGrade(g,mx,my,sx,sw);return;}
        int[] values=gradeMode==0?grade:gradeMode==1?tone:gradeMode==2?effects:advanced[gradeMode-3];
        String[] names=gradeMode==0?GRADE_NAMES:gradeMode==1?TONE_NAMES:gradeMode==2?EFFECT_NAMES:ADV_NAMES[gradeMode-3];
        int slots=Math.max(1,Math.min(values.length,(height-151)/25));
        adjustScroll=Math.max(0,Math.min(adjustScroll,values.length-slots));
        for(int r=0;r<slots;r++){
            int i=r+adjustScroll,y=87+r*25;
            int min=gradeMode==2||gradeMode==0&&i==5||gradeMode==1&&(i==3||i==4)||gradeMode==5&&(i==1||i==3||i==4)||gradeMode==7&&i==2?0:-100;
            slider(g,names[i]+" "+values[i],sx,y,sw,min,100,values[i],0,v->{values[i]=v;requestPreview();});
        }
        int y=height-66;
        if(gradeMode==5){
            action(g,"Miniature",sx,y,sw/2-2,14,mx,my,false,()->{int[] a={0,22,0,85,30,18};System.arraycopy(a,0,values,0,6);requestPreview();});
            action(g,"Diagonal",sx+sw/2,y,sw/2-2,14,mx,my,false,()->{int[] a={0,22,25,85,30,18};System.arraycopy(a,0,values,0,6);requestPreview();});
        }else if(gradeMode==0){
            action(g,"Film",sx,y,w-2,14,mx,my,false,()->{preset(4,22,12,18,0,25);requestPreview();});
            action(g,"B&W",sx+w,y,w-2,14,mx,my,false,()->{preset(0,18,-100,0,0,15);requestPreview();});
            action(g,"Night",sx+w*2,y,w-2,14,mx,my,false,()->{preset(15,10,12,-16,25,10);requestPreview();});
        }else if(gradeMode==2){
            action(g,"Film",sx,y,w-2,14,mx,my,false,()->{int[] a={35,18,6,25,8,0,65};System.arraycopy(a,0,values,0,7);requestPreview();});
            action(g,"Dream",sx+w,y,w-2,14,mx,my,false,()->{int[] a={70,4,0,8,0,30,0};System.arraycopy(a,0,values,0,7);requestPreview();});
            action(g,"Horror",sx+w*2,y,w-2,14,mx,my,false,()->{int[] a={12,35,15,60,15,20,40};System.arraycopy(a,0,values,0,7);requestPreview();});
        }else StudioUi.label(g,font,"Scroll for more controls",sx,y+3,sw,StudioUi.MUTED);
        action(g,"Apply",sx,y+16,w-2,15,mx,my,false,this::applyDraft);
        action(g,"Cancel",sx+w,y+16,w-2,15,mx,my,false,this::clearPreview);
        action(g,"Zero",sx+2*w,y+16,w-2,15,mx,my,false,()->{java.util.Arrays.fill(values,0);requestPreview();});
    }
    private void drawWorkspaceGrade(GuiGraphics g,int mx,int my,int sx,int sw){
        int bottom=height-49;
        if(gradeMode==8){
            int tab=sw/4;String[] channels={"RGB","R","G","B"};
            for(int i=0;i<4;i++){int c=i;action(g,channels[i],sx+i*tab,87,tab-2,14,mx,my,i==curveChannel,()->curveChannel=c);}
            graphX=sx+2;graphY=105;graphW=Math.max(20,sw-4);graphH=Math.max(20,Math.min(graphW,bottom-graphY-21));
            WorkspaceControls.curve(g,graphX,graphY,graphW,graphH,curves[curveChannel],histogram[curveChannel],curveChannel);
            if(graphY+graphH+10<bottom)StudioUi.label(g,font,"Drag 9 curve points",sx,graphY+graphH+4,sw,StudioUi.MUTED);
        }else if(gradeMode==9){
            int tab=sw/3;String[] channels={"Shadows","Midtones","Lights"};
            for(int i=0;i<3;i++){int c=i;action(g,channels[i],sx+i*tab,87,tab-2,14,mx,my,i==wheelChannel,()->wheelChannel=c);}
            wheelR=Math.max(10,Math.min((sw-8)/2,(bottom-130)/2));wheelX=sx+sw/2;wheelY=105+wheelR;
            WorkspaceControls.wheel(g,wheelX,wheelY,wheelR,wheels[wheelChannel*3],wheels[wheelChannel*3+1]);
            int k=wheelChannel*3+2;
            if(wheelY+wheelR+26<bottom)slider(g,"Luminance "+wheels[k],sx,wheelY+wheelR+3,sw,-100,100,wheels[k],0,v->{wheels[k]=v;requestPreview();});
        }else{
            slider(g,"Horizontal "+perspective[0],sx,90,sw,-100,100,perspective[0],0,v->{perspective[0]=v;requestPreview();});
            slider(g,"Vertical "+perspective[1],sx,118,sw,-100,100,perspective[1],0,v->{perspective[1]=v;requestPreview();});
            StudioUi.label(g,font,"Projective warp",sx,149,sw,StudioUi.MUTED);
        }
        int w=sw/3;
        action(g,"Apply",sx,bottom,w-2,16,mx,my,false,this::applyDraft);
        action(g,"Cancel",sx+w,bottom,w-2,16,mx,my,false,()->{clearPreview();layerEditing=-1;});
        action(g,"Reset",sx+w*2,bottom,w-2,16,mx,my,false,()->{if(gradeMode==8)curves=WorkspacePhoto.identity();else if(gradeMode==9)java.util.Arrays.fill(wheels,0);else java.util.Arrays.fill(perspective,0);requestPreview();});
    }
    private void updateCurve(double y){if(curveNode<0)return;curves[curveChannel][curveNode]=(int)Math.round(255*Math.max(0,Math.min(1,1-(y-graphY)/Math.max(1,graphH))));requestPreview();}
    private void updateWheel(double x,double y){double dx=(x-wheelX)/wheelR,dy=(y-wheelY)/wheelR,k=Math.max(1,Math.hypot(dx,dy));wheels[wheelChannel*3]=(int)Math.round(dx/k*100);wheels[wheelChannel*3+1]=(int)Math.round(dy/k*100);requestPreview();}
    private void preset(int... v){System.arraycopy(v,0,grade,0,6);}
    private void compare(){
        if(busy||rendered==null||doc==null||!renderedCurrent)return;
        if(draftDirty){status="Apply or cancel preview first";return;}
        if(compareMode){compareMode=false;return;}
        if(comparisonTexture!=null){compareMode=true;return;}
        busy=true;long token=++epoch;var current=doc;var image=rendered;
        var ops=current.snapshot();
        CompletableFuture.supplyAsync(()->ImageDocument.resize(current.beforeGeometry(ops),2048),ShotFiles.IO)
                .whenComplete((result,error)->Minecraft.getInstance().execute(()->{
                    if(!alive||token!=epoch)return;busy=false;
                    if(error!=null){status="Comparison failed";return;}
                    try{comparisonTexture=new StudioTexture(result);compareMode=true;status="Drag divider | Ctrl+Shift+S exports side-by-side";}
                    catch(Exception e){status="Comparison unavailable";}
                }));
    }
    private void exportCompare(){
        if(busy||doc==null||rendered==null||!renderedCurrent)return;
        busy=true;long token=++epoch;var current=doc;var image=rendered;
        CompletableFuture.supplyAsync(()->{
            try{return ShotFiles.export(StudioClient.directory(),shot,current.compare(image),false,0);}
            catch(Exception e){throw new RuntimeException(e);}
        },ShotFiles.IO).whenComplete((result,error)->Minecraft.getInstance().execute(()->{
            if(!alive||token!=epoch)return;busy=false;status=error==null?"Comparison saved: "+result.getFileName():"Comparison export failed";
        }));
    }
    private void drawFile(GuiGraphics g,int mx,int my,int sx,int sw){
        int y=69,step=Math.max(10,Math.min(16,(height-96)/13)),h=step-2;
        action(g,"Save project",sx,y,sw,h,mx,my,false,()->project(false));
        action(g,"Load / recover",sx,y+=step,sw,h,mx,my,false,()->project(true));
        action(g,exportJpeg?"Format: JPEG":"Format: PNG",sx,y+=step,sw,h,mx,my,false,()->exportJpeg=!exportJpeg);
        action(g,"Quality: "+exportQuality+"%",sx,y+=step,sw,h,mx,my,false,()->exportQuality=exportQuality<=40?100:exportQuality-10);
        action(g,exportSide==0?"Size: original":"Long side: "+exportSide,sx,y+=step,sw,h,mx,my,false,()->exportSide=exportSide==0?3840:exportSide==3840?1920:exportSide==1920?1280:exportSide==1280?840:exportSide==840?640:0);
        action(g,exportBudget==0?"Limit: none":"Limit: "+exportBudget+" KB",sx,y+=step,sw,h,mx,my,false,()->exportBudget=exportBudget==0?2000:exportBudget==2000?1000:exportBudget==1000?500:exportBudget==500?250:0);
        action(g,"Export image",sx,y+=step,sw,h,mx,my,false,this::customExport);
        action(g,importPath==null?"Pick import":importPath.getFileName().toString(),sx,y+=step,sw,h,mx,my,false,this::pickImport);
        action(g,"Add image",sx,y+=step,sw,h,mx,my,false,this::importImage);
        action(g,"Scene info",sx,y+=step,sw,h,mx,my,false,this::sceneInfo);
        action(g,"Thumbnail Studio",sx,y+=step,sw,h,mx,my,false,()->{if(!busy&&rendered!=null&&!draftDirty)Minecraft.getInstance().setScreen(new ThumbnailScreen(shot,rendered,new StudioScreen(shot,parent)));});
        int[] wh=CANVAS[canvasPreset];action(g,"Canvas "+wh[0]+"x"+wh[1],sx,y+=step,sw,h,mx,my,false,()->canvasPreset=(canvasPreset+1)%CANVAS.length);
        action(g,"Resize / fit",sx,y+=step,sw,h,mx,my,false,()->apply(new AdvancedPhoto.Canvas(wh[0],wh[1],false),true));
    }
    private void saveRecovery(){
        if(!autoSave||doc==null)return;var ops=doc.snapshot();var path=shot.path();
        AUTOSAVE.execute(()->{try{com.kirill.screenshotmanager.core.RecoveryStore.save(path,ops);}catch(Exception ignored){}});
    }
    private void project(boolean load){
        if(busy||doc==null)return;
        busy=true;status=load?"Loading project...":"Saving project...";long token=++epoch;
        var ops=doc.snapshot();
        CompletableFuture.supplyAsync(()->{
            try{if(load)return com.kirill.screenshotmanager.core.RecoveryStore.exists(shot.path())?com.kirill.screenshotmanager.core.RecoveryStore.load(shot.path()):ProjectStore.load(shot.path());ProjectStore.save(shot.path(),ops);return ops;}
            catch(Exception e){throw new RuntimeException(e);}
        },ShotFiles.IO).whenComplete((result,error)->Minecraft.getInstance().execute(()->{
            if(!alive||token!=epoch)return;busy=false;
            if(error!=null){status="Project: "+message(error);return;}
            if(load){doc.restore(result);selected=-1;renderDoc(true);}else status="Project saved next to original";
        }));
    }
    private void customExport(){
        if(busy||rendered==null||!renderedCurrent)return;
        if(draftDirty){status="Apply or cancel preview first";return;}
        busy=true;status="Exporting...";long token=++epoch;
        var image=rendered;boolean jpeg=exportJpeg;int quality=exportQuality,side=exportSide,budget=exportBudget;
        CompletableFuture.supplyAsync(()->{
            try{return CreatorExport.save(StudioClient.directory().resolve("edited"),image,jpeg,quality,side,budget*1000L);}
            catch(Exception e){throw new RuntimeException(e);}
        },ShotFiles.IO).whenComplete((path,error)->Minecraft.getInstance().execute(()->{
            if(!alive||token!=epoch)return;busy=false;status=error==null?"Saved "+path.getFileName():"Export: "+message(error);
        }));
    }
    private void pickImport(){
        if(busy)return;busy=true;long token=++epoch;int wanted=importIndex+1;
        CompletableFuture.supplyAsync(()->{
            try{var folder=StudioClient.directory().resolve("imports");java.nio.file.Files.createDirectories(folder);
                var found=new java.util.ArrayList<java.nio.file.Path>();
                try(var list=java.nio.file.Files.list(folder)){list.filter(java.nio.file.Files::isRegularFile).filter(path->{String n=path.getFileName().toString().toLowerCase(java.util.Locale.ROOT);return n.endsWith(".png")||n.endsWith(".jpg")||n.endsWith(".jpeg");}).sorted().limit(250).forEach(found::add);}
                var edits=StudioClient.directory().resolve("edited");
                if(java.nio.file.Files.isDirectory(edits))try(var list=java.nio.file.Files.list(edits)){list.filter(java.nio.file.Files::isRegularFile).filter(path->path.getFileName().toString().startsWith("cutout_")&&path.getFileName().toString().endsWith(".png")).sorted(java.util.Comparator.reverseOrder()).limit(100).forEach(found::add);}
                return java.util.List.copyOf(found);
            }catch(Exception e){throw new RuntimeException(e);}
        },ShotFiles.IO).whenComplete((list,error)->Minecraft.getInstance().execute(()->{
            if(!alive||epoch!=token)return;busy=false;
            if(error!=null||list.isEmpty()){status="Put PNG/JPG in screenshots/imports, then Pick import";importPath=null;return;}
            importIndex=wanted%list.size();importPath=list.get(importIndex);status="Selected "+importPath.getFileName();
        }));
    }
    private void importImage(){
        if(busy||doc==null||rendered==null||importPath==null)return;
        busy=true;long token=++epoch;var path=importPath;int x=rendered.getWidth()/2,y=rendered.getHeight()/2;
        CompletableFuture.supplyAsync(()->{try{return ProPhoto.ImageLayer.load(path,x,y);}catch(Exception e){throw new RuntimeException(e);}},ShotFiles.IO)
            .whenComplete((layer,error)->Minecraft.getInstance().execute(()->{
                if(!alive||epoch!=token)return;busy=false;if(error!=null){status="Import failed: "+message(error);return;}
                imageSize=100;imageAngle=0;imageOpacity=100;apply(layer,false);panel=1;
            }));
    }
    private void depthFocus(){
        if(busy||doc==null||rendered==null)return;
        if(doc.snapshot().stream().anyMatch(ImageDocument::geometry)){status="Depth focus must be added before crop / rotate";return;}
        busy=true;long token=++epoch;int x=ax,y=ay,w=rendered.getWidth(),h=rendered.getHeight(),radius=focusRadius,amount=focusAmount;
        CompletableFuture.supplyAsync(()->{
            try{var map=DepthMap.load(shot.path());return new DepthMap.Focus(map,map.valueAt(x/(double)Math.max(1,w-1),y/(double)Math.max(1,h-1)),radius,amount);}
            catch(Exception e){throw new RuntimeException(e);}
        },ShotFiles.IO).whenComplete((op,error)->Minecraft.getInstance().execute(()->{
            if(!alive||epoch!=token)return;busy=false;
            if(error!=null){status="No usable depth map: capture a clean depth shot with F4";return;}
            apply(op,false);
        }));
    }
    private void sceneInfo(){
        if(busy)return;busy=true;long token=++epoch;
        CompletableFuture.supplyAsync(()->{try{return com.kirill.screenshotmanager.core.WorldCapture.read(shot.path());}catch(Exception e){return "No scene metadata for this screenshot";}},ShotFiles.IO)
            .whenComplete((value,error)->Minecraft.getInstance().execute(()->{if(!alive||epoch!=token)return;busy=false;status=value==null?"No scene metadata":value;}));
    }
    private void selection(GuiGraphics g){
        int x1=ix+(int)Math.round(ax*scale),y1=iy+(int)Math.round(ay*scale);
        int x2=ix+(int)Math.round(bx*scale),y2=iy+(int)Math.round(by*scale);
        if(brush()||tool==Tool.LINE||tool==Tool.ARROW){
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
        for(int i=0;i<=steps;i++){double t=steps==0?0:i/(double)steps;int x=(int)(x1+(x2-x1)*t),y=(int)(y1+(y2-y1)*t);int size=brush()?Math.max(1,Math.min(40,(int)Math.round(thickness*scale))):2;StudioUi.box(g,x-size/2,y-size/2,size,size,tool==Tool.ERASER?0x66ffffff:markColor(tool));}
    }
    private boolean canvas(double x,double y){return x>=cx&&x<cx+cw&&y>=cy&&y<cy+ch;}
    private boolean image(double x,double y){return rendered!=null&&canvas(x,y)&&x>=ix&&x<ix+iw&&y>=iy&&y<iy+ih;}
    private int px(double x){return Math.max(0,Math.min(rendered.getWidth()-1,(int)((x-ix)/scale)));}
    private int py(double y){return Math.max(0,Math.min(rendered.getHeight()-1,(int)((y-iy)/scale)));}
    private int markColor(Tool t){return t==Tool.HIGHLIGHT||t==Tool.HIGHLIGHT_BRUSH?(color&0xffffff)|((opacity*255/100)<<24):color|0xff000000;}
    private void commitMark(){
        if(doc==null||busy||compareMode||!renderedCurrent)return;
        if(tool==Tool.COPY_REGION){
            int x=Math.min(ax,bx),y=Math.min(ay,by),w=Math.abs(ax-bx)+1,h=Math.abs(ay-by)+1;
            BufferedImage source=rendered;busy=true;long token=++epoch;
            CompletableFuture.runAsync(()->ShotFiles.copyImage(ImageDocument.crop(source,x,y,w,h)),ShotFiles.IO).whenComplete((v,e)->Minecraft.getInstance().execute(()->{
                if(alive&&epoch==token){busy=false;status=e==null?"Region copied":"Copy failed: "+message(e);}
            }));return;
        }
        if(tool==Tool.TEXT&&draft.isBlank()){status="Enter annotation text";return;}
        if(tool==Tool.TEXT){apply(new WorkspacePhoto.Text(draft,fontFamily,ax,ay+thickness*4,thickness*4,color,captionStyle,captionAngle,fontStyle),false);return;}
        if(tool==Tool.EYEDROPPER){color=rendered.getRGB(ax,ay)|0xff000000;status=String.format("Picked #%06X",color&0xffffff);return;}
        if(tool==Tool.DEPTH_FOCUS){depthFocus();return;}
        if(tool==Tool.FOCUS||tool==Tool.FOCUS_SHADE){apply(new ProPhoto.Focus(ax,ay,focusRadius,focusAmount,tool==Tool.FOCUS_SHADE),false);return;}
        if(tool==Tool.CROP&&(Math.abs(ax-bx)<2||Math.abs(ay-by)<2)){status="Select a larger region";return;}
        String value=tool==Tool.NUMBER?Integer.toString(number++):tool==Tool.TEXT?draft:"";
        apply(ImageDocument.mark(new ImageDocument.Mark(tool,ax,ay,bx,by,markColor(tool),thickness,strength,value,List.copyOf(points))),tool==Tool.CROP);
    }
    @Override public boolean mouseClicked(double x,double y,int button){
        if(button==0&&((text.visible&&text.isMouseOver(x,y))||(hexBox.visible&&hexBox.isMouseOver(x,y))))return super.mouseClicked(x,y,button);
        if(button==0&&!workspaceWide&&Math.abs(x-sideX)<=4&&y>=66&&y<height-23){resizePanel=true;setDragging(true);return true;}
        if(panel==2&&gradeMode==8&&graphW>0&&x>=graphX-4&&x<=graphX+graphW+4&&y>=graphY-4&&y<=graphY+graphH+4){if(button==0){curveNode=Math.max(0,Math.min(WorkspacePhoto.CURVE_POINTS-1,(int)Math.round((x-graphX)/graphW*(WorkspacePhoto.CURVE_POINTS-1))));updateCurve(y);setDragging(true);return true;}}
        if(panel==2&&gradeMode==9&&wheelR>0&&Math.hypot(x-wheelX,y-wheelY)<=wheelR+3){if(button==0){wheelDrag=true;updateWheel(x,y);setDragging(true);return true;}if(button==1){wheels[wheelChannel*3]=wheels[wheelChannel*3+1]=0;requestPreview();return true;}}
        for(StudioSlider slider:sliders)if(slider.contains(x,y)){
            clearTextFocus();if(button==1){slider.reset();return true;}if(button==0){activeSlider=slider;slider.update(x);setDragging(true);return true;}
        }
        clearTextFocus();
        if(button==0){
            for(StudioUi.Hit hit:List.copyOf(hits))if(hit.contains(x,y)){hit.action().run();return true;}
        }
        if(compareMode&&canvas(x,y)&&button==0){compareDrag=true;split=Math.max(0,Math.min(1,(x-cx)/cw));setDragging(true);return true;}
        if((compareMode||peek)&&canvas(x,y))return true;
        if(panel==1&&button==0&&image(x,y)&&!busy&&selected>=0&&selected<doc.count()){
            if((ImageDocument.unwrap(doc.snapshot().get(selected)) instanceof ImageDocument.MarkOperation||ImageDocument.unwrap(doc.snapshot().get(selected)) instanceof ProPhoto.ImageLayer||ImageDocument.unwrap(doc.snapshot().get(selected)) instanceof AdvancedPhoto.Caption||ImageDocument.unwrap(doc.snapshot().get(selected)) instanceof WorkspacePhoto.Text)&&!ImageDocument.geometry(doc.snapshot().get(selected))){
                movingLayer=true;moveX=px(x);moveY=py(y);return true;
            }
        }
        if(canvas(x,y)&&rendered!=null&&(button==2||(button==0&&tool==Tool.PAN))){panning=true;return true;}
        if((panel==1||panel==2||panel==3)&&button==0&&canvas(x,y))return true;
        if(button==0&&image(x,y)&&!busy){
            text.setFocused(false);setFocused(null);
            ax=bx=px(x);ay=by=py(y);points.clear();
            if(tool==Tool.TEXT){textPending=true;textX=ax;textY=ay;panel=0;optionsPage=0;text.visible=true;setFocused(text);text.setFocused(true);status="Type annotation, Enter to place";return true;}
            if(tool==Tool.NUMBER||tool==Tool.FOCUS||tool==Tool.FOCUS_SHADE||tool==Tool.DEPTH_FOCUS||tool==Tool.EYEDROPPER){commitMark();return true;}
            dragging=true;setDragging(true);return true;
        }
        return super.mouseClicked(x,y,button);
    }
    @Override public boolean mouseDragged(double x,double y,int button,double dx,double dy){
        if(button==0){if(resizePanel){preferredSide=(int)(width-x);layout();return true;}if(compareDrag){split=Math.max(0,Math.min(1,(x-cx)/cw));return true;}if(curveNode>=0){updateCurve(y);return true;}if(wheelDrag){updateWheel(x,y);return true;}}
        if(activeSlider!=null&&button==0){activeSlider.update(x);return true;}
        if(movingLayer)return true;
        if(panning){panX+=dx;panY+=dy;imageBounds();return true;}
        if(dragging&&button==0){bx=px(x);by=py(y);if(brush()&&points.size()<4096)points.add(new Point(bx,by));return true;}
        return super.mouseDragged(x,y,button,dx,dy);
    }
    @Override public boolean mouseReleased(double x,double y,int button){
        if(button==0&&(resizePanel||compareDrag||curveNode>=0||wheelDrag)){resizePanel=compareDrag=wheelDrag=false;curveNode=-1;setDragging(false);return true;}
        if(activeSlider!=null&&button==0){activeSlider.update(x);activeSlider=null;setDragging(false);return true;}
        if(movingLayer){movingLayer=false;if(!busy&&doc!=null)changed(doc.translate(selected,px(x)-moveX,py(y)-moveY));return true;}
        if(panning){panning=false;return true;}
        if(dragging&&button==0){bx=px(x);by=py(y);dragging=false;commitMark();return true;}
        return super.mouseReleased(x,y,button);
    }
    @Override public boolean mouseScrolled(double x,double y,double horizontal,double vertical){
        if(x<toolW&&y>=47){toolScroll=Math.max(0,toolScroll-(int)Math.signum(vertical));return true;}
        if(x>=sideX&&y>=65){if(panel==1)layerScroll=Math.max(0,layerScroll-(int)Math.signum(vertical));else if(panel==2)adjustScroll=Math.max(0,adjustScroll-(int)Math.signum(vertical));return true;}
        if(compareMode)return true;
        if(!canvas(x,y)||rendered==null)return super.mouseScrolled(x,y,horizontal,vertical);
        double fx=(x-ix)/scale,fy=(y-iy)/scale;
        zoom=Math.max(.2,Math.min(12,zoom*Math.pow(1.18,vertical)));
        imageBounds();panX+=x-(ix+fx*scale);panY+=y-(iy+fy*scale);imageBounds();return true;
    }
    @Override public boolean keyPressed(int key,int scancode,int mods){
        if(hexBox.visible&&hexBox.isFocused()){if(key==GLFW.GLFW_KEY_ENTER||key==GLFW.GLFW_KEY_KP_ENTER){applyHex();return true;}if(key==GLFW.GLFW_KEY_ESCAPE){clearTextFocus();syncHex();return true;}return super.keyPressed(key,scancode,mods);}
        if(key==GLFW.GLFW_KEY_TAB&&!text.isFocused()){workspaceWide=!workspaceWide;clearTextFocus();return true;}
        if(key==GLFW.GLFW_KEY_ESCAPE){if(draftDirty){clearPreview();status="Preview cancelled";return true;}if(textPending||text.isFocused()){textPending=false;clearTextFocus();return true;}onClose();return true;}
        if(textPending&&(key==GLFW.GLFW_KEY_ENTER||key==GLFW.GLFW_KEY_KP_ENTER)){if(draft.isBlank()){status="Type text first";return true;}ax=bx=textX;ay=by=textY;points.clear();textPending=false;clearTextFocus();commitMark();return true;}
        if(text.visible&&text.isFocused())return super.keyPressed(key,scancode,mods);
        if(!hasControlDown()&&(mods&GLFW.GLFW_MOD_ALT)==0){
            Tool shortcut=switch(key){case GLFW.GLFW_KEY_B->Tool.PEN;case GLFW.GLFW_KEY_E->Tool.ERASER;case GLFW.GLFW_KEY_T->Tool.TEXT;case GLFW.GLFW_KEY_C->Tool.CROP;case GLFW.GLFW_KEY_I->Tool.EYEDROPPER;case GLFW.GLFW_KEY_V->Tool.PAN;default->null;};
            if(shortcut!=null){selectTool(shortcut);return true;}
        }
        if(hasControlDown()){
            if(key==GLFW.GLFW_KEY_V){pasteImage();return true;}
            if(key==GLFW.GLFW_KEY_Z){if(hasShiftDown())redo();else undo();return true;}
            if(key==GLFW.GLFW_KEY_Y){redo();return true;}
            if(key==GLFW.GLFW_KEY_S){if(hasShiftDown())exportCompare();else output(false,false,0);return true;}
            if(key==GLFW.GLFW_KEY_C&&!text.isFocused()){output(true,false,0);return true;}
        }
        return super.keyPressed(key,scancode,mods);
    }
    private void pasteImage(){
        if(busy||doc==null||rendered==null)return;busy=true;long token=++epoch;int x=rendered.getWidth()/2,y=rendered.getHeight()/2;
        CompletableFuture.supplyAsync(()->{try{return ClipboardLayers.read(x,y);}catch(Exception e){throw new RuntimeException(e);}},ShotFiles.IO).whenComplete((layer,error)->Minecraft.getInstance().execute(()->{
            if(!alive||token!=epoch)return;busy=false;if(error!=null){status="Clipboard has no supported image";return;}apply(layer,false);panel=1;
        }));
    }
    @Override public boolean isPauseScreen(){return false;}
    @Override public void onClose(){Minecraft.getInstance().setScreen(parent);}
    @Override public void removed(){clearPreview();alive=false;epoch++;busy=false;dragging=panning=false;if(texture!=null){texture.close();texture=null;}if(comparisonTexture!=null){comparisonTexture.close();comparisonTexture=null;}doc=null;rendered=null;compareMode=false;movingLayer=false;activeSlider=null;textPending=false;}
}
