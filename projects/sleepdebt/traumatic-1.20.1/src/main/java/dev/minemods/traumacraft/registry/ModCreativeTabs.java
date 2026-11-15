package dev.minemods.traumacraft.registry;
import dev.minemods.traumacraft.TraumaCraft;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public final class ModCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB,TraumaCraft.MOD_ID);
    public static final RegistryObject<CreativeModeTab> MEDICAL = TABS.register("medical",()->CreativeModeTab.builder()
        .title(Component.translatable("itemGroup.traumacraft.medical"))
        .icon(()->ModItems.MEDKIT.get().getDefaultInstance())
        .displayItems((params,output)->{
            for(var x:ModItems.COMPONENTS)output.accept(x.get());
            for(var x:ModItems.MEDICINE)output.accept(x.get());
        }).build());
    private ModCreativeTabs(){}
}
