package dev.voidchisel;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.*;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.core.registries.Registries;
public final class VoidPickaxe implements ModInitializer{
    public static final String MODID="voidchisel";
    public static final Item PICKAXE=create();
    private static Item create(){
        var key=ResourceKey.create(Registries.ITEM,Identifier.fromNamespaceAndPath(MODID,"void_chisel"));
        return Registry.register(BuiltInRegistries.ITEM,key,
            new Item(new Item.Properties().setId(key).pickaxe(ToolMaterial.NETHERITE,1.0F,-2.8F)
                .durability(5).rarity(Rarity.EPIC)));
    }
    @Override public void onInitialize(){
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.TOOLS_AND_UTILITIES)
            .register(entries->entries.accept(PICKAXE));
    }
}
