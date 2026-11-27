package dev.minemods.traumacraft.qa;
import dev.minemods.traumacraft.registry.ModItems;
import dev.minemods.traumacraft.content.HerbSpecies;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.*;
import java.util.List;
public final class PrimitiveRecipeChecks {
    private static void check(boolean ok,String msg){if(!ok)throw new IllegalStateException("Primitive recipes: "+msg);}
    private static CraftingRecipe recipe(ServerLevel l,String path){
        var r=l.getServer().getRecipeManager().byKey(ResourceLocation.fromNamespaceAndPath("traumacraft",path)).orElseThrow();
        if(!(r.value() instanceof CraftingRecipe c))throw new IllegalStateException("not crafting recipe "+path);
        return c;
    }
    private static void verify(ServerLevel l,String path,CraftingInput grid,Item result,int count){
        var r=recipe(l,path);check(r.matches(grid,l),path+" matches");var out=r.assemble(grid,l.registryAccess());
        check(out.is(result)&&out.getCount()==count,path+" exact output");
    }
    public static void verify(ServerLevel l){
        verify(l,"rag_dressing_from_string",CraftingInput.of(2,2,List.of(new ItemStack(Items.STRING),new ItemStack(Items.STRING),new ItemStack(Items.STRING),new ItemStack(Items.STRING))),ModItems.RAG_DRESSING.get(),1);
        verify(l,"bandage_from_rag",CraftingInput.of(2,1,List.of(new ItemStack(ModItems.RAG_DRESSING.get()),new ItemStack(Items.STRING))),ModItems.BANDAGE.get(),1);
        verify(l,"splint",CraftingInput.of(2,1,List.of(new ItemStack(Items.STICK),new ItemStack(Items.STRING))),ModItems.SPLINT.get(),1);
        check(!recipe(l,"bandage_from_rag").matches(CraftingInput.of(2,1,List.of(new ItemStack(Items.PAPER),new ItemStack(Items.STRING))),l),"reject non-dressing");
        check(!recipe(l,"herbal_poultice").matches(CraftingInput.of(2,1,List.of(new ItemStack(ModItems.RAG_DRESSING.get()),HerbSpecies.powder(HerbSpecies.CHAMOMILE,1,0))),l),"typed powder cannot become generic via legacy recipe");
        var cattail=net.minecraft.core.registries.BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("worldlogicterrain","cattail"));
        var reed=net.minecraft.core.registries.BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("worldlogicterrain","reed"));
        if(cattail!=Items.AIR)verify(l,"rag_dressing_from_string",CraftingInput.of(2,2,List.of(new ItemStack(cattail),new ItemStack(cattail),new ItemStack(cattail),new ItemStack(cattail))),ModItems.RAG_DRESSING.get(),1);
        if(reed!=Items.AIR)verify(l,"splint",CraftingInput.of(2,1,List.of(new ItemStack(reed),new ItemStack(Items.STRING))),ModItems.SPLINT.get(),1);
    }
}
