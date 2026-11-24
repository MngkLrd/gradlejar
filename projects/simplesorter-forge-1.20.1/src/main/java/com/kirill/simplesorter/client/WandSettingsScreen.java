package com.kirill.simplesorter.client;

import com.kirill.simplesorter.network.ClientNetwork;
import com.kirill.simplesorter.network.WandActionPayload;
import com.kirill.simplesorter.network.WandSettingsPayload;
import com.kirill.simplesorter.network.WandSuppliesPayload;
import com.kirill.simplesorter.settings.SortingMode;
import com.kirill.simplesorter.settings.SortingWandSettings;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;

/** Compact vanilla-style settings. Widgets and panel share one layout. */
public final class WandSettingsScreen extends Screen {
    private static final String KEY="screen.simplesorter.wand_settings.";
    private final InteractionHand hand;
    private SortingWandSettings settings;
    private boolean advanced;
    private int panelX,panelY,panelW,panelH,rowH,gap,bodyX,bodyW,bodyY;

    public WandSettingsScreen(ItemStack stack,InteractionHand hand){
        super(Component.translatable(KEY+"title"));
        this.hand=hand;
        settings=SortingWandSettings.readFrom(stack);
    }

    @Override protected void init(){rebuild();}

    private static Component tr(String key,Object... args){return Component.translatable(KEY+key,args);}

    private void rebuild(){
        clearWidgets();
        // Fit the scaled viewport, including small windows and high GUI scale.
        panelW=Math.min(248,Math.max(1,width-12));
        gap=height>=196?4:2;
        rowH=Math.min(20,Math.max(12,(height-12-26-6*gap)/7));
        panelH=26+7*rowH+6*gap;
        panelX=(width-panelW)/2;panelY=(height-panelH)/2;
        bodyX=panelX+8;bodyW=Math.max(1,panelW-16);bodyY=panelY+21;
        if(advanced)advancedPage();else mainPage();
    }

    private int y(int row){return bodyY+row*(rowH+gap);}

    private void add(Component text,Component hint,int x,int row,int w,Button.OnPress action){
        Button.Builder b=Button.builder(text,action).bounds(x,y(row),Math.max(1,w),rowH);
        if(hint!=null)b.tooltip(Tooltip.create(hint));
        addRenderableWidget(b.build());
    }
    private void full(Component text,Component hint,int row,Button.OnPress action){
        add(text,hint,bodyX,row,bodyW,action);
    }
    private void pair(Component left,Component leftTip,Button.OnPress l,
                      Component right,Component rightTip,Button.OnPress r,int row){
        int half=(bodyW-gap)/2;
        add(left,leftTip,bodyX,row,half,l);
        add(right,rightTip,bodyX+half+gap,row,bodyW-half-gap,r);
    }
    private Component toggle(String key,boolean value){
        return tr(key).append(Component.literal(": ")).append(tr(value?"on":"off"));
    }

    private void mainPage(){
        full(tr("compact.mode",Component.translatable(settings.mode().translationKey())),
                Component.translatable(settings.mode().descriptionKey()),0,b->{settings=settings.nextMode();rebuild();});
        int side=Math.min(28,bodyW/4);
        add(Component.literal("-"),tr("compact.radius_tip"),bodyX,1,side,b->{settings=settings.previousRadius();rebuild();});
        add(tr("radius_value",settings.radius()),tr("compact.radius_tip"),bodyX+side+gap,1,
                bodyW-2*(side+gap),b->{settings=settings.nextRadius();rebuild();});
        add(Component.literal("+"),tr("compact.radius_tip"),bodyX+bodyW-side,1,side,b->{settings=settings.nextRadius();rebuild();});
        full(tr("compact.sort"),Component.translatable(settings.mode().descriptionKey()),2,b->run(settings.mode()));
        full(tr("compact.deposit"),tr("compact.deposit_tip"),3,b->run(SortingMode.PLAYER_DEPOSIT));
        full(tr("compact.advanced"),tr("compact.advanced_tip"),4,b->{advanced=true;rebuild();});
        // One deliberately quiet row for key help. It is a tooltip, not a long
        // unbounded line competing with the title and mode buttons.
        full(tr("compact.controls"),tr("keys.line",ClientKeyMappings.OPEN_SETTINGS.getTranslatedKeyMessage(),
                ClientKeyMappings.QUICK_SORT.getTranslatedKeyMessage()),5,b->{});
        pair(tr("save"),null,b->{save(false);onClose();},
                Component.translatable("gui.cancel"),null,b->onClose(),6);
    }

    private void advancedPage(){
        full(toggle("show_radius.short",settings.showRadius()),tr("compact.radius_tip"),0,b->{
            settings=settings.withShowRadius(!settings.showRadius());rebuild();});
        full(toggle("highlight_chests.short",settings.showContainerHighlights()),null,1,b->{
            settings=settings.withShowContainerHighlights(!settings.showContainerHighlights());rebuild();});
        full(toggle("transfer_fx.short",settings.showTransferParticles()),null,2,b->{
            settings=settings.withShowTransferParticles(!settings.showTransferParticles());rebuild();});
        full(toggle("compact.auto",settings.autoOrganize()),tr("compact.auto_tip"),3,b->{
            settings=settings.withAutoOrganize(!settings.autoOrganize());rebuild();});
        Component label=tr("compact.labels",tr(switch(settings.labelMode()){
            case 1->"compact.text";case 2->"compact.icons";default->"off";
        }));
        pair(label,tr("compact.labels_tip"),b->{settings=settings.nextLabelMode();rebuild();},
                tr("compact.load_labels"),tr("compact.load_tip"),b->{
                    save(false);ClientNetwork.send(new WandSuppliesPayload(hand==InteractionHand.OFF_HAND?1:0));
                },4);
        pair(tr("defaults"),null,b->{settings=SortingWandSettings.defaults();rebuild();},
                tr("compact.reload"),tr("compact.reload_tip"),b->save(true),5);
        pair(tr("compact.back"),null,b->{advanced=false;rebuild();},
                tr("save"),null,b->{save(false);onClose();},6);
    }

    private void save(boolean reload){
        Minecraft mc=Minecraft.getInstance();
        if(mc.player!=null)settings.writeTo(mc.player.getItemInHand(hand));
        ClientNetwork.send(WandSettingsPayload.save(hand,settings,reload));
    }
    private void run(SortingMode mode){
        Minecraft mc=Minecraft.getInstance();
        if(mc.player!=null)settings.writeTo(mc.player.getItemInHand(hand));
        ClientNetwork.send(WandActionPayload.run(hand,settings,mode));onClose();
    }

    @Override public void render(GuiGraphics g,int mouseX,int mouseY,float partialTick){
        renderBackground(g);
        int x=panelX,y=panelY,w=panelW,h=panelH;
        // Vanilla inventory-inspired bevel; no opaque full-screen backdrop.
        g.fill(x,y,x+w,y+h,0xFF373737);
        g.fill(x+1,y+1,x+w-1,y+h-1,0xFFC6C6C6);
        g.fill(x+1,y+1,x+w-2,y+2,0xFFFFFFFF);
        g.fill(x+1,y+1,x+2,y+h-2,0xFFFFFFFF);
        g.fill(x+2,y+h-2,x+w-1,y+h-1,0xFF555555);
        g.fill(x+w-2,y+2,x+w-1,y+h-1,0xFF555555);
        String heading=(advanced?tr("compact.advanced"):title).getString();
        heading=font.plainSubstrByWidth(heading,Math.max(1,w-16));
        g.drawString(font,heading,x+8,y+7,0xFF404040,false);
        super.render(g,mouseX,mouseY,partialTick);
    }
    @Override public boolean isPauseScreen(){return false;}
}
