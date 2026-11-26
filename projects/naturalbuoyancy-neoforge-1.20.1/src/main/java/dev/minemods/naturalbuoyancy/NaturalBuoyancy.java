package dev.minemods.naturalbuoyancy;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.fml.common.Mod;
@Mod("naturalbuoyancy")
public final class NaturalBuoyancy {
    public NaturalBuoyancy(){ MinecraftForge.EVENT_BUS.addListener(NaturalBuoyancy::levelTick); }
    private static void levelTick(TickEvent.LevelTickEvent event) {
        if(event.phase!=TickEvent.Phase.END||!(event.level instanceof ServerLevel level)) return;
        for(var e:level.getAllEntities()) if(e instanceof ItemEntity item) BuoyancyPhysics.tick(item);
    }
}
