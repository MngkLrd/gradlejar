package dev.voidchisel;

import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.SimpleTier;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

@Mod(VoidChisel.MOD_ID)
public final class VoidChisel {
    public static final String MOD_ID = "voidchisel";

    // Netherite mining level, iron-pickaxe-equivalent timing for bedrock is handled separately.
    public static final SimpleTier PICKAXE_TIER = new SimpleTier(
            BlockTags.INCORRECT_FOR_NETHERITE_TOOL, 5, 9.0F, 4.0F, 15,
            () -> Ingredient.EMPTY);

    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MOD_ID);

    // Keep original registry ID to preserve existing worlds and items.
    public static final Supplier<Item> VOID_CHISEL = ITEMS.register("void_chisel",
            () -> new VoidPickaxeItem(PICKAXE_TIER,
                    new Item.Properties().rarity(Rarity.EPIC).setNoRepair()
                            .attributes(PickaxeItem.createAttributes(PICKAXE_TIER, 1.0F, -2.8F))));

    public VoidChisel(IEventBus modBus) {
        ITEMS.register(modBus);
        modBus.addListener(this::addCreativeTab);
        modBus.addListener(this::registerNetwork);
        NeoForge.EVENT_BUS.addListener(BedrockMiningServer::tick);
    }

    private void registerNetwork(RegisterPayloadHandlersEvent event) {
        event.registrar("1").playToServer(MiningInputPacket.TYPE, MiningInputPacket.CODEC,
                MiningInputPacket::receive);
    }

    private void addCreativeTab(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey().equals(CreativeModeTabs.TOOLS_AND_UTILITIES)) {
            event.accept(VOID_CHISEL.get());
        }
    }
}
