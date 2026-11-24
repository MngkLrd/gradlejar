package dev.ataflore.catchtheblast;

import dev.ataflore.catchtheblast.item.BlastShieldItem;
import dev.ataflore.catchtheblast.combat.BlastCombat;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredItem;

@Mod(CatchTheBlast.ID)
public final class CatchTheBlast {
    public static final String ID = "catchtheblast";
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(ID);
    public static final DeferredItem<BlastShieldItem> BLAST_SHIELD = ITEMS.register("blast_shield",
            () -> new BlastShieldItem(new Item.Properties().durability(512)));
    public CatchTheBlast(IEventBus bus) {
        ITEMS.register(bus);
        bus.addListener(this::creative);
        NeoForge.EVENT_BUS.addListener(BlastCombat::onBlock);
    }
    private void creative(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.COMBAT) event.accept(BLAST_SHIELD.get());
    }
}
