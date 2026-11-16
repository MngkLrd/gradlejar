package dev.voidchisel;
import net.minecraft.world.item.*;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
@Mod(VoidPickaxe.MODID)
public final class VoidPickaxe {
    public static final String MODID="voidchisel";
    public static final DeferredRegister<Item> ITEMS=DeferredRegister.create(ForgeRegistries.ITEMS,MODID);
    public static final RegistryObject<Item> PICKAXE=ITEMS.register("void_chisel",
        ()->new PickaxeItem(Tiers.NETHERITE,1,-2.8F,new Item.Properties().durability(5).rarity(Rarity.EPIC)));
    public VoidPickaxe(){
        var bus=FMLJavaModLoadingContext.get().getModEventBus();
        ITEMS.register(bus);
        bus.addListener(this::creative);
    }
    private void creative(BuildCreativeModeTabContentsEvent e){
        if(e.getTabKey().equals(CreativeModeTabs.TOOLS_AND_UTILITIES))e.accept(PICKAXE.get());
    }
}
