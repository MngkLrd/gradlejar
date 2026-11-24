package dev.mngklrd.hearthbound;

import java.util.List;
import java.util.Comparator;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.dries007.tfc.common.blockentities.PotBlockEntity;
import net.dries007.tfc.common.recipes.PotRecipe;
import net.dries007.tfc.common.recipes.SoupPotRecipe;
import net.dries007.tfc.common.recipes.TFCRecipeTypes;
import net.dries007.tfc.common.recipes.ingredients.FluidStackIngredient;

/**
 * Internal plan for Hearthbound vessels only. Never registers, replaces or changes
 * the native pot's recipes. Output, nutritional categories, decay and servings
 * are always produced by TFC's SoupPotRecipe.
 */
public final class FlexibleSoup {
    private FlexibleSoup(){}
    // Native TFC 1.20.1 defaults: 3/4/5 foods, 1000/1150/1300 ticks, 300 degrees.
    // For 1/2 foods use the three-food timing so smaller soups cannot speedrun cooking.
    private static final SoupPotRecipe[] DEFAULTS=createDefaults();
    private static SoupPotRecipe[] createDefaults(){
        SoupPotRecipe[] out=new SoupPotRecipe[5];
        for(int i=0;i<out.length;i++){
            int count=i+1;
            out[i]=new SoupPotRecipe(new ResourceLocation(Hearthbound.ID,"flexible_soup_"+count),
                List.of(),FluidStackIngredient.EMPTY,1000+150*Math.max(0,count-3),300f);
        }
        return out;
    }
    public static SoupPotRecipe resolve(Level level,PotBlockEntity.PotInventory inventory){
        int count=0;
        for(int i=4;i<=8;i++)if(!inventory.getStackInSlot(i).isEmpty())count++;
        PotRecipe exact=level.getRecipeManager().getRecipeFor(TFCRecipeTypes.POT.get(),inventory,level).orElse(null);
        if(exact instanceof SoupPotRecipe soup)return soup;
        // A non-soup match must not hide a native soup match.
        for(PotRecipe candidate:level.getRecipeManager().getAllRecipesFor(TFCRecipeTypes.POT.get()))
            if(candidate instanceof SoupPotRecipe soup&&soup.matches(inventory,level))return soup;
        // Use normal native soup timing/temperature where possible, even if an addon
        // food is absent from tfc:foods/usable_in_soup. Exact matching is not needed
        // by the base TFC dynamic output generator.
        final int groups=count;
        return level.getRecipeManager().getAllRecipesFor(TFCRecipeTypes.POT.get()).stream()
            .filter(p->p.getClass()==SoupPotRecipe.class)
            .filter(p->p.getFluidIngredient().test(inventory.getFluidInTank(0)))
            .map(p->(SoupPotRecipe)p)
            .min(Comparator.<SoupPotRecipe>comparingInt(p->Math.abs(p.getItemIngredients().size()-groups))
                .thenComparing(p->p.getId().toString()))
            .orElse(DEFAULTS[Math.max(0,Math.min(DEFAULTS.length-1,count-1))]);
    }
}
