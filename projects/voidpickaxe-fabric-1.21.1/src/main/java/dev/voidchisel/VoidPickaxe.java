package dev.voidchisel;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.*;
import net.minecraft.resources.ResourceLocation;
public final class VoidPickaxe implements ModInitializer{
    public static final String MODID="voidchisel";
    public static final Item PICKAXE=Registry.register(BuiltInRegistries.ITEM,ResourceLocation.fromNamespaceAndPath(MODID,"void_chisel"),new PickaxeItem(Tiers.NETHERITE,new Item.Properties().durability(5).rarity(Rarity.EPIC).attributes(PickaxeItem.createAttributes(Tiers.NETHERITE,1.0F,-2.8F))));
    
    @Override public void onInitialize(){
        ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.TOOLS_AND_UTILITIES)
            .register(entries->entries.accept(PICKAXE));
    }
}
