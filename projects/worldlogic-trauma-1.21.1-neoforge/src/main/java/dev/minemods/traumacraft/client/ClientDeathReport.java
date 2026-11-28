package dev.minemods.traumacraft.client;
import dev.minemods.traumacraft.health.DeathReport;
import dev.minemods.traumacraft.client.gui.TraumaDeathScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.DeathScreen;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/** Wait for both the authoritative snapshot and the actual vanilla death screen (either arrival order). */
public final class ClientDeathReport {
    private static DeathReport report,archive;
    private static boolean shown,deadSeen;
    private static Object player;
    private ClientDeathReport(){}
    public static void accept(dev.minemods.traumacraft.network.DeathReportPacket packet){
        if(packet.archived()){archive=packet.report();return;}
        report=packet.report();shown=false;deadSeen=false;player=Minecraft.getInstance().player;
    }
    public static void clear(){archive=null;report=null;shown=false;deadSeen=false;player=null;}
    public static void tick(ClientTickEvent.Post ignored){
        var mc=Minecraft.getInstance();var p=mc.player;
        if(p==null){clear();return;}
        if(player!=null&&player!=p){report=null;shown=false;deadSeen=false;player=null;}
        if(archive!=null){var value=archive;archive=null;if(p.isAlive()&&dev.minemods.traumacraft.client.gui.DeathReportLayout.available(mc.getWindow().getGuiScaledWidth(),mc.getWindow().getGuiScaledHeight()))mc.setScreen(new TraumaDeathScreen(null,value));}
        if(report==null)return;
        if(!p.isAlive())deadSeen=true;
        if(p.isAlive()&&deadSeen&&!(mc.screen instanceof DeathScreen)&&!(mc.screen instanceof TraumaDeathScreen)){clear();return;}
        if(!shown&&mc.screen instanceof DeathScreen death&&mc.getWindow().getGuiScaledWidth()>=300&&mc.getWindow().getGuiScaledHeight()>=174){
            shown=true;mc.setScreen(new TraumaDeathScreen(death,report));
        }
    }
}
