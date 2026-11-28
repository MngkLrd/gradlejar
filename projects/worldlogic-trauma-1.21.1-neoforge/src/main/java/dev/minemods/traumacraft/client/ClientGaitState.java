package dev.minemods.traumacraft.client;
import dev.minemods.traumacraft.network.GaitPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import java.util.HashMap;
import java.util.UUID;
import java.util.Map;
public final class ClientGaitState {
    private record Entry(float left,float right,long expires){}
    private static final Map<UUID,Entry> STATES=new HashMap<>();
    private ClientGaitState(){}
    private static float clamp(float f){return Float.isFinite(f)?Math.max(0,Math.min(1,f)):1;}
    public static void accept(GaitPacket p){
        var level=Minecraft.getInstance().level;
        if(level==null)return;
        long now=level.getGameTime();
        STATES.entrySet().removeIf(e->e.getValue().expires()<now);
        STATES.put(p.player(),new Entry(clamp(p.left()),clamp(p.right()),now+100));
    }
    public static float leg(Player p,boolean left){
        var mc=Minecraft.getInstance();
        if(p==mc.player){
            var part=left?dev.minemods.traumacraft.health.BodyPart.LEFT_LEG:dev.minemods.traumacraft.health.BodyPart.RIGHT_LEG;
            return dev.minemods.traumacraft.health.PhysicalFunction.limb(ClientBodyState.fraction(part),ClientBodyState.fractured(part),ClientBodyState.stabilized(part));
        }
        var e=STATES.get(p.getUUID());
        if(e==null||e.expires()<p.level().getGameTime())return 1;
        return left?e.left():e.right();
    }
    public static void clear(){STATES.clear();}
}
