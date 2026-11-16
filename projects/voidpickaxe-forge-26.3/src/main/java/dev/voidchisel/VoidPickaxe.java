package dev.voidchisel;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.*;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
@Mod(VoidPickaxe.MODID)
public final class VoidPickaxe{
    public static final String MODID="voidchisel";
    public static final DeferredRegister<Item> ITEMS=DeferredRegister.create(ForgeRegistries.ITEMS,MODID);
    public static final RegistryObject<Item> PICKAXE=ITEMS.register("void_chisel",()->{
        var key=ResourceKey.create(Registries.ITEM,Identifier.fromNamespaceAndPath(MODID,"void_chisel"));
        return new Item(new Item.Properties().setId(key)
                .pickaxe(ToolMaterial.NETHERITE,1.0F,-2.8F)
                .durability(5).rarity(Rarity.EPIC));
    });
    public VoidPickaxe(FMLJavaModLoadingContext context){
        ITEMS.register(context.getModBusGroup());
    }
}
