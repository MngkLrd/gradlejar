package dev.minemods.traumacraft.client.gui;

import dev.minemods.traumacraft.client.ClientBodyState;
import dev.minemods.traumacraft.health.BodyPart;
import dev.minemods.traumacraft.item.MedicalItem;
import dev.minemods.traumacraft.network.ApplyMedicalPacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.PacketDistributor;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Reuses vanilla InventoryMenu: slot coordinates and normal crafting stay vanilla. */
public final class TraumaInventoryScreen extends InventoryScreen {
    private static final int PANEL_X = 177;
    private static final int PANEL_WIDTH = 141;
    private BodyPart hovered;

    public TraumaInventoryScreen(Player player) {
        super(player);
        this.imageWidth = 320;
    }

    private int panelX() { return leftPos + PANEL_X; }

    @Override
    protected void renderBg(GuiGraphics gui, float partialTick, int mouseX, int mouseY) {
        super.renderBg(gui, partialTick, mouseX, mouseY);
        int x = panelX(), y = topPos;
        // Recessed vanilla-gray extension, anchored to existing inventory border.
        gui.fill(x-2, y, x+PANEL_WIDTH+1, y+166, 0xFF373737);
        gui.fill(x-1, y, x+PANEL_WIDTH, y+165, 0xFFF7F7F7);
        gui.fill(x, y+1, x+PANEL_WIDTH, y+165, 0xFFC6C6C6);
        gui.fill(x+PANEL_WIDTH-1, y+1, x+PANEL_WIDTH, y+165, 0xFF555555);
        gui.fill(x+1, y+16, x+PANEL_WIDTH-3, y+17, 0xFF777777);
        gui.drawString(font, Component.translatable("screen.traumacraft.body"), x+7, y+5, 0xFF373737, false);

        // Left schematic body panel.
        gui.fill(x+5, y+21, x+55, y+156, 0xFF5B6062);
        gui.fill(x+7, y+23, x+53, y+154, 0xFF262B2C);
        BodyVisual.draw(gui, x+12, y+43, false);
        int rowX = x+59;
        BodyPart[] parts = BodyPart.values();
        hovered = BodyVisual.hit(mouseX, mouseY, x+12, y+43);
        for (int i=0; i<parts.length; i++) {
            BodyPart part=parts[i];
            int rowY = y+19+i*19;
            if (mouseX>=rowX && mouseX< x+PANEL_WIDTH-4 && mouseY>=rowY && mouseY<rowY+18) {
                hovered=part;
            }
            drawPartRow(gui, part, rowX, rowY, x+PANEL_WIDTH-5, hovered == part);
        }
        gui.drawString(font, Component.translatable("screen.traumacraft.drag_to_treat"),
                x+6, y+158, 0xFF5F5F5F, false);
    }

    private void drawPartRow(GuiGraphics gui, BodyPart part, int x, int y, int right, boolean hot) {
        if (hot) gui.fill(x-2,y-1,right+2,y+17,0x559CBAA2);
        String shortName = switch (part) {
            case HEAD -> "HEAD"; case CHEST -> "CHEST"; case STOMACH -> "ABDOMEN";
            case LEFT_ARM -> "L. ARM"; case RIGHT_ARM -> "R. ARM";
            case LEFT_LEG -> "L. LEG"; case RIGHT_LEG -> "R. LEG";
        };
        gui.drawString(font, shortName, x,y,0xFF414141,false);
        String counter = Math.round(ClientBodyState.hp(part))+"/"+part.maximum;
        int tw=font.width(counter);
        gui.drawString(font,counter,right-tw,y,0xFF414141,false);
        int bx=x, by=y+11, bw=right-x;
        gui.fill(bx,by,bx+bw,by+5,0xFF333333);
        gui.fill(bx+1,by+1,bx+bw-1,by+4,0xFF4A4F50);
        int filled=Math.round((bw-2)*ClientBodyState.fraction(part));
        if(filled>0)gui.fill(bx+1,by+1,bx+1+filled,by+4,BodyVisual.hpColor(part,false));
        if(ClientBodyState.bleeding(part)>0) gui.fill(right-8,y+1,right-5,y+5,0xFFE04343);
        if(ClientBodyState.fractured(part)) gui.fill(right-13,y+1,right-10,y+5,0xFFEEEEEE);
    }

    @Override
    public void render(GuiGraphics gui, int mouseX, int mouseY, float partialTick) {
        super.render(gui,mouseX,mouseY,partialTick);
        if(hovered==null)return;
        var tip = new ArrayList<Component>();
        tip.add(Component.literal(hovered.label));
        tip.add(Component.literal(String.format(Locale.ROOT, "HP: %.0f / %d",ClientBodyState.hp(hovered),hovered.maximum)));
        if(ClientBodyState.bleeding(hovered)>0)
            tip.add(Component.literal(ClientBodyState.bleeding(hovered)==2 ? "Heavy bleeding" : "Light bleeding"));
        if(ClientBodyState.fractured(hovered))tip.add(Component.literal("Fracture"));
        if(this.menu.getCarried().getItem() instanceof MedicalItem)
            tip.add(Component.translatable("screen.traumacraft.click_to_treat"));
        gui.renderComponentTooltip(font,tip,mouseX,mouseY);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if(button == 0 || button == 1) {
            BodyPart body = findPart((int)mouseX, (int)mouseY);
            if(body != null) {
                if (this.menu.getCarried().getItem() instanceof MedicalItem)
                    PacketDistributor.sendToServer(new ApplyMedicalPacket(body.ordinal()));
                return true;
            }
        }
        return super.mouseClicked(mouseX,mouseY,button);
    }

    private BodyPart findPart(int mx,int my) {
        int x = panelX(), y = topPos;
        BodyPart p = BodyVisual.hit(mx,my,x+12,y+43);
        if(p!=null)return p;
        if(mx>=x+59 && mx<x+PANEL_WIDTH-4) {
            int index=(my-(y+19))/19;
            if(my>=y+19 && index>=0 && index<BodyPart.values().length
                    && my<y+19+index*19+18) return BodyPart.values()[index];
        }
        return null;
    }
}
