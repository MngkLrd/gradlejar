package dev.mngklrd.hearthbound;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public final class VesselScreen extends AbstractContainerScreen<VesselMenu> {
    private Button lid;
    public VesselScreen(VesselMenu menu,Inventory inv,Component title){super(menu,inv,title);imageWidth=176;imageHeight=194;inventoryLabelY=101;}
    @Override protected void init(){
        super.init();
        lid=addRenderableWidget(Button.builder(lidLabel(),b->{
            if(minecraft!=null&&minecraft.gameMode!=null)minecraft.gameMode.handleInventoryButtonClick(menu.containerId,0);
        }).bounds(leftPos+96,topPos+73,72,18).build());
    }
    private Component lidLabel(){return Component.translatable(menu.value(3)==1?"hearthbound.gui.open":"hearthbound.gui.close");}
    @Override protected void containerTick(){super.containerTick();if(lid!=null)lid.setMessage(lidLabel());}
    @Override public void render(GuiGraphics g,int x,int y,float delta){renderBackground(g);super.render(g,x,y,delta);renderTooltip(g,x,y);}
    private void rect(GuiGraphics g,int x,int y,int w,int h,int color){g.fill(leftPos+x,topPos+y,leftPos+x+w,topPos+y+h,color);}
    private void slot(GuiGraphics g,int x,int y){rect(g,x-1,y-1,18,18,0xff6a5140);rect(g,x,y,17,17,0xffc8b8a0);rect(g,x,y,16,16,0xff9e8c72);}
    @Override protected void renderBg(GuiGraphics g,float d,int x,int y){
        rect(g,0,0,176,194,0xff47362a);rect(g,1,1,174,192,0xffd5c1a1);rect(g,3,3,170,188,0xffe0cfb1);
        rect(g,4,4,168,25,0xff6d4934);rect(g,4,29,168,1,0xffb3946d);
        rect(g,8,32,22,40,0xff654c38);rect(g,9,33,20,38,0xffb7a184);rect(g,11,35,16,34,0xff51493d);
        int h=Math.max(0,Math.min(34,34*menu.value(0)/Math.max(1,menu.value(1))));
        if(h>0){rect(g,11,69-h,16,h,0xff4d83a0);rect(g,11,69-h,16,1,0xff9bd0d6);}
        for(int i=0;i<5;i++)slot(g,43+i*22,39);
        if(menu.value(4)>0){int w=Math.max(0,Math.min(82,82*menu.value(5)/100));rect(g,8,86,82,5,0xff80684f);if(w>0)rect(g,8,86,w,5,0xffb97a36);}
        rect(g,4,96,168,1,0xffb3946d);
        for(int row=0;row<3;row++)for(int col=0;col<9;col++)slot(g,8+col*18,112+row*18);
        for(int col=0;col<9;col++)slot(g,8+col*18,170);
    }
    @Override protected void renderLabels(GuiGraphics g,int x,int y){
        g.drawString(font,font.plainSubstrByWidth(title.getString(),160),8,8,0xfff5e6cc,false);
        String state=menu.value(4)>0?"ready":menu.value(6)==0?"cold":menu.value(2)==2?(menu.value(3)==1?"simmering":"boiling"):"warming";
        g.drawString(font,Component.translatable("hearthbound.gui."+state),8,20,0xfff5e6cc,false);
        g.drawString(font,Component.translatable("hearthbound.gui.ingredients"),43,30,0xff4b3828,false);
        g.drawString(font,Component.literal(menu.value(0)+"/"+menu.value(1)+" mB"),8,75,0xff4b3828,false);
        if(menu.value(4)>0)g.drawString(font,Component.translatable("hearthbound.gui.servings",menu.value(4),menu.value(5)),43,59,0xff4b3828,false);
        else{
            String key=switch(menu.value(7)){case 1->"water";case 2->"no_recipe";case 3->"planned";case 4->"rotten";default->"add_food";};
            Component hint=Component.translatable("hearthbound.gui."+key,menu.value(8),menu.value(9));
            g.drawString(font,font.plainSubstrByWidth(hint.getString(),123),43,58,0xff4b3828,false);
            g.drawString(font,Component.translatable("hearthbound.gui.remove"),43,67,0xff4b3828,false);
        }
        g.drawString(font,playerInventoryTitle,8,101,0xff4b3828,false);
    }
}
