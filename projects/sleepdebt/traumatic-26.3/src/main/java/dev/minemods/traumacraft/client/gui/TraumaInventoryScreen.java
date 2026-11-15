package dev.minemods.traumacraft.client.gui;
import dev.minemods.traumacraft.client.ClientBodyState;
import dev.minemods.traumacraft.health.BodyPart;
import dev.minemods.traumacraft.item.MedicalItem;
import dev.minemods.traumacraft.network.ApplyMedicalPacket;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import java.util.ArrayList;
import java.util.Locale;

public final class TraumaInventoryScreen extends InventoryScreen {
    private static final int PANEL_X=177,PANEL_WIDTH=141;
    private BodyPart hovered;
    public TraumaInventoryScreen(Player player){super(player);}
    @Override protected void init(){super.init();this.leftPos=(this.width-320)/2;}
    private int panelX(){return leftPos+PANEL_X;}

    @Override
    public void extractBackground(GuiGraphicsExtractor gui,int mouseX,int mouseY,float alpha){
        super.extractBackground(gui,mouseX,mouseY,alpha);
        int x=panelX(),y=topPos;
        gui.fill(x-2,y,x+PANEL_WIDTH+1,y+166,0xFF373737);
        gui.fill(x-1,y,x+PANEL_WIDTH,y+165,0xFFF7F7F7);
        gui.fill(x,y+1,x+PANEL_WIDTH,y+165,0xFFC6C6C6);
        gui.fill(x+PANEL_WIDTH-1,y+1,x+PANEL_WIDTH,y+165,0xFF555555);
        gui.fill(x+1,y+16,x+PANEL_WIDTH-3,y+17,0xFF777777);
        gui.text(font,Component.translatable("screen.traumacraft.body"),x+7,y+5,0xFF373737,false);
        gui.fill(x+5,y+21,x+55,y+156,0xFF5B6062);
        gui.fill(x+7,y+23,x+53,y+154,0xFF262B2C);
        BodyVisual.draw(gui,x+12,y+43,false);
        int rx=x+59;
        hovered=BodyVisual.hit(mouseX,mouseY,x+12,y+43);
        BodyPart[] zones=BodyPart.values();
        for(int i=0;i<zones.length;i++){
            BodyPart p=zones[i];
            int py=y+19+i*19;
            if(mouseX>=rx&&mouseX<x+PANEL_WIDTH-4&&mouseY>=py&&mouseY<py+18)hovered=p;
            drawZone(gui,p,rx,py,x+PANEL_WIDTH-5,hovered==p);
        }
        gui.text(font,Component.translatable("screen.traumacraft.drag_to_treat"),x+6,y+158,0xFF5F5F5F,false);
    }
    private void drawZone(GuiGraphicsExtractor gui,BodyPart p,int x,int y,int right,boolean hot){
        if(hot)gui.fill(x-2,y-1,right+2,y+17,0x559CBAA2);
        String title=switch(p){case HEAD->"HEAD";case CHEST->"CHEST";case STOMACH->"ABDOMEN";
          case LEFT_ARM->"L. ARM";case RIGHT_ARM->"R. ARM";case LEFT_LEG->"L. LEG";case RIGHT_LEG->"R. LEG";};
        gui.text(font,title,x,y,0xFF414141,false);
        String counter=Math.round(ClientBodyState.hp(p))+"/"+p.maximum;
        gui.text(font,counter,right-font.width(counter),y,0xFF414141,false);
        int bw=right-x;
        gui.fill(x,y+11,x+bw,y+16,0xFF333333);
        gui.fill(x+1,y+12,x+bw-1,y+15,0xFF4A4F50);
        int f=Math.round((bw-2)*ClientBodyState.fraction(p));
        if(f>0)gui.fill(x+1,y+12,x+1+f,y+15,BodyVisual.hpColor(p,false));
        if(ClientBodyState.bleeding(p)>0)gui.fill(right-8,y+1,right-5,y+5,0xFFE04343);
        if(ClientBodyState.fractured(p))gui.fill(right-13,y+1,right-10,y+5,0xFFEEEEEE);
    }
    @Override
    public void extractRenderState(GuiGraphicsExtractor gui,int mouseX,int mouseY,float alpha){
        super.extractRenderState(gui,mouseX,mouseY,alpha);
        if(hovered==null)return;
        var tooltip=new ArrayList<Component>();
        tooltip.add(Component.literal(hovered.label));
        tooltip.add(Component.literal(String.format(Locale.ROOT,"HP: %.0f / %d",ClientBodyState.hp(hovered),hovered.maximum)));
        if(ClientBodyState.bleeding(hovered)>0)tooltip.add(Component.literal(ClientBodyState.bleeding(hovered)==2?"Heavy bleeding":"Light bleeding"));
        if(ClientBodyState.fractured(hovered))tooltip.add(Component.literal("Fracture"));
        if(menu.getCarried().getItem() instanceof MedicalItem)
            tooltip.add(Component.translatable("screen.traumacraft.click_to_treat"));
        gui.setComponentTooltipForNextFrame(font,tooltip,mouseX,mouseY);
    }
    @Override
    public boolean mouseClicked(MouseButtonEvent event,boolean doubleClick){
        if(event.button()==0||event.button()==1){
            BodyPart p=findPart((int)event.x(),(int)event.y());
            if(p!=null){
                if(menu.getCarried().getItem() instanceof MedicalItem)
                    ClientPacketDistributor.sendToServer(new ApplyMedicalPacket(p.ordinal()));
                return true;
            }
        }
        return super.mouseClicked(event,doubleClick);
    }
    private BodyPart findPart(int mx,int my){
        int x=panelX(),y=topPos;
        BodyPart p=BodyVisual.hit(mx,my,x+12,y+43);
        if(p!=null)return p;
        if(mx>=x+59&&mx<x+PANEL_WIDTH-4&&my>=y+19){
            int i=(my-y-19)/19;
            if(i>=0&&i<7&&my<y+19+i*19+18)return BodyPart.values()[i];
        }
        return null;
    }
}
