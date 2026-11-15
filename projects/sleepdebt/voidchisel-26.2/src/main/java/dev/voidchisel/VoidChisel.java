package dev.voidchisel;
import net.minecraft.world.item.*;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.registries.*;
@Mod(VoidChisel.MOD_ID)
public final class VoidChisel {
    public static final String MOD_ID = "voidchisel";
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MOD_ID);
    public static final DeferredItem<Item> VOID_CHISEL = ITEMS.registerItem("void_chisel",
            props -> new Item(props.pickaxe(ToolMaterial.NETHERITE, 1.0F, -2.8F)
                    .durability(5).rarity(Rarity.EPIC)));
    public VoidChisel(IEventBus bus) {
        ITEMS.register(bus);
        bus.addListener(VoidChisel::creativeTab);
        bus.addListener(VoidChisel::network);
        NeoForge.EVENT_BUS.addListener(BedrockMiningServer::tick);
        if (net.neoforged.fml.loading.FMLEnvironment.getDist() ==
                net.neoforged.api.distmarker.Dist.CLIENT)
            dev.voidchisel.client.ClientHooks.register();
    }
    private static void creativeTab(BuildCreativeModeTabContentsEvent e) {
        if (e.getTabKey().equals(CreativeModeTabs.TOOLS_AND_UTILITIES)) e.accept(VOID_CHISEL.get());
    }
    private static void network(RegisterPayloadHandlersEvent e) {
        e.registrar("1").playToServer(MiningInputPacket.TYPE, MiningInputPacket.CODEC, MiningInputPacket::receive);
    }
}
