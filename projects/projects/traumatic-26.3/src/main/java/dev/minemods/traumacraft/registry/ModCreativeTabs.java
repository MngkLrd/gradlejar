package dev.minemods.traumacraft.registry;
import dev.minemods.traumacraft.TraumaCraft;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> TABS=
        DeferredRegister.create(Registries.CREATIVE_MODE_TAB,TraumaCraft.MOD_ID);
    public static final DeferredHolder<CreativeModeTab,CreativeModeTab> MEDICAL=TABS.register("medical",
        ()->CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.traumacraft.medical"))
            .icon(()->new ItemStack(ModItems.MEDKIT.get()))
            .displayItems((params,out)->{
                for(var x:ModItems.COMPONENTS)out.accept(x.get());
                for(var x:ModItems.MEDICINE)out.accept(x.get());
            }).build());
    private ModCreativeTabs(){}
}
