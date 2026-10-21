package dev.creeperarmor.registry;
import dev.creeperarmor.CreeperArmorMod;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;
import net.minecraftforge.registries.ForgeRegistries;
public final class ModCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> TABS=DeferredRegister.create(ForgeRegistries.CREATIVE_MODE_TABS,CreeperArmorMod.MOD_ID);
    public static final RegistryObject<CreativeModeTab> CREEPER_ARMOR=TABS.register("creeper_armor",()->CreativeModeTab.builder()
        .title(Component.translatable("itemGroup.creeperarmor.creeper_armor"))
        .icon(()->new ItemStack(ModItems.CREEPER_CHESTPLATE.get()))
        .displayItems((p,out)->{
            out.accept(ModItems.CREEPER_PLATE.get());out.accept(ModItems.CREEPER_POWDER.get());
            out.accept(ModItems.CREEPER_HELMET.get());out.accept(ModItems.CREEPER_CHESTPLATE.get());
            out.accept(ModItems.CREEPER_LEGGINGS.get());out.accept(ModItems.CREEPER_BOOTS.get());
            out.accept(ModItems.CREEPER_SHIELD.get());
        }).build());
}
