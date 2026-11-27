package dev.minemods.worldlogic;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.minecraft.server.level.ServerLevel;
public final class Cleanup {
 public static void broken(BlockEvent.BreakEvent e){if(!e.isCanceled()&&e.getLevel() instanceof ServerLevel l)WorldState.clear(l,e.getPos());}
}
