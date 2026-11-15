package dev.minemods.traumacraft.jei;
import dev.minemods.traumacraft.TraumaCraft;
import dev.minemods.traumacraft.registry.ModItems;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import java.util.List;
@JeiPlugin
public final class TraumaJeiPlugin implements IModPlugin {
    @Override public Identifier getPluginUid(){
        return Identifier.fromNamespaceAndPath(TraumaCraft.MOD_ID,"medical");
    }
    @Override public void registerRecipes(IRecipeRegistration recipes){
        for(var item:ModItems.MEDICINE){
            String id=item.getId().getPath();
            recipes.addItemStackInfo(List.of(new ItemStack(item.get())),Component.translatable("jei.traumacraft."+id));
        }
        for(var item:ModItems.COMPONENTS){
            String id=item.getId().getPath();
            recipes.addItemStackInfo(List.of(new ItemStack(item.get())),Component.translatable("jei.traumacraft."+id));
        }
    }
}
