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
    private Button[] tabs=new Button[4];
    public ThermalScreen(Screen parent){super(ThermalPanel.tr("title"));this.parent=parent;}
    private int contentW(){return ThermalCoreClient.contentWidth(width);}
    private int panelW(){return Math.min(252,Math.max(1,contentW()-8));}
    private int panelY(){return Math.max(22,(height-166)/2);}
    @Override protected void init(){
        int w=panelW(),x=(contentW()-w)/2,y=panelY();
        String[] keys={"body_tab","weather_tab","history_tab","room_tab"};
        net.minecraft.world.item.Item[] icons={net.minecraft.world.item.Items.LEATHER_CHESTPLATE,net.minecraft.world.item.Items.CLOCK,net.minecraft.world.item.Items.WRITABLE_BOOK,net.minecraft.world.item.Items.CAMPFIRE};
        for(int i=0;i<4;i++){final int index=i;int left=x+i*w/4,right=x+(i+1)*w/4;
            tabs[i]=addRenderableWidget(new InnerTab(left,y-21,right-left-1,ThermalPanel.tr(keys[i]),icons[i],b->{page=index;}));}
        int bottom=y+168;
        addRenderableWidget(Button.builder(Component.translatable("gui.back"),b->onClose()).bounds(contentW()/2-40,bottom,80,20).build());
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
        for(int i=0;i<4;i++)tabs[i].active=i!=page;
        super.render(g,mx,my,delta);
        int w=panelW();var r=new ThermalLayout.Rect((contentW()-w)/2,panelY(),w,166);
        if(page==0)ThermalPanel.draw(g,r,data);else if(page==1)WeatherPanel.draw(g,r,data);else if(page==2)JournalPanel.draw(g,r,history,dayIndex,mx,my);else RoomPanel.draw(g,r,data);
        String tip=tooltip(page,r,mx,my);
        if(page==1&&tip!=null&&data!=null){var wdata=data.weather();if(tip.equals("thunder")&&!wdata.weatherKind().isEmpty())tip="weather_kind";if(tip.equals("reference")&&Double.isFinite(wdata.pressure()))tip="pressure";if(tip.equals("daily")&&Double.isFinite(wdata.gust()))tip="gust";}
        if(tip!=null)g.renderTooltip(font,font.split(Component.translatable("tooltip.worldlogic_thermal.info."+tip),Math.min(220,width-16)),mx,my);
    }
    private static final class InnerTab extends Button {
        private final Component caption;
        private final net.minecraft.world.item.ItemStack icon;
        InnerTab(int x,int y,int w,Component label,net.minecraft.world.item.Item item,OnPress press){
            super(x,y,w,20,Component.empty(),press,DEFAULT_NARRATION);
            caption=label;icon=new net.minecraft.world.item.ItemStack(item);
            setTooltip(net.minecraft.client.gui.components.Tooltip.create(label));
        }
        @Override protected void renderWidget(GuiGraphics g,int mx,int my,float delta){
            super.renderWidget(g,mx,my,delta);
            var f=net.minecraft.client.Minecraft.getInstance().font;
            g.renderItem(icon,getX()+2,getY()+2);
            if(getWidth()>23)g.drawString(f,f.plainSubstrByWidth(caption.getString(),getWidth()-22),getX()+20,getY()+6,active?0xffffffff:0xffb0b0b0,true);
        }
        @Override protected net.minecraft.network.chat.MutableComponent createNarrationMessage(){return caption.copy();}
    }
    static String tooltip(int page,ThermalLayout.Rect r,int mx,int my){
        if(!r.contains(mx,my))return null;
        int y=my-r.y();
        if(page==0){
            if(y>=20&&y<48)return "body";
            if(y>=50&&y<160){String[] rows={"air","felt","wet","insulation","heat","trend","adaptation","nutrition","room_heat","condition"};return rows[Math.min(9,(y-50)/11)];}
            if(y<18)return "body_tab";
        }else if(page==1){
            if(y<18)return "weather_tab";
            if(y>=20&&y<152){String[] rows={"biome","elevation","air","humidity","wind","precip","thunder","solar","reference","daily","annual"};return rows[Math.min(10,(y-20)/12)];}
            if(y>=152)return "shelter";
        }else if(page==3){
            if(y<20)return "room_tab";
            if(y>=20&&y<104){String[] rows={"room_air","air","room_heat","room_humidity","room_ventilation","room_mass","room_volume"};return rows[Math.min(6,(y-20)/12)];}
        }else{
            if(y<20)return "history_tab";
            if(y>=110&&y<125)return "history_values";
            if(y>=125&&y<150)return "history_location";
            if(y>=150)return "history_samples";
        }
        return null;
    }
}
