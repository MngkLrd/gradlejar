package dev.minemods.naturalbuoyancy;
import net.minecraft.world.entity.item.ItemEntity;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
@Mod("naturalbuoyancy")
public final class NaturalBuoyancy {
    public NaturalBuoyancy(){ NeoForge.EVENT_BUS.addListener(NaturalBuoyancy::entityTick); }
    private static void entityTick(EntityTickEvent.Post event) {
        if(event.getEntity() instanceof ItemEntity item) BuoyancyPhysics.tick(item);
    }
}
