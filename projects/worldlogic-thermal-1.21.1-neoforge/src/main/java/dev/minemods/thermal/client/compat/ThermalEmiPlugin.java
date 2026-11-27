package dev.minemods.thermal.client.compat;

import dev.emi.emi.api.EmiEntrypoint;
import dev.emi.emi.api.EmiPlugin;
import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.recipe.EmiInfoRecipe;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.minemods.thermal.ThermalMod;
import dev.minemods.thermal.content.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.*;
import net.minecraft.world.level.ItemLike;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Discovered only by EMI. No common/server-side entry point links this optional API. */
@EmiEntrypoint
public final class ThermalEmiPlugin implements EmiPlugin {
    private static ResourceLocation id(String path){return ResourceLocation.fromNamespaceAndPath(ThermalMod.ID,path);}
    private static EmiRecipeCategory category(EmiRegistry registry,String name,ItemLike icon){
        var c=new EmiRecipeCategory(id(name),EmiStack.of(icon));
        registry.addCategory(c);return c;
    }
    private static String category(Item item,String name){
        if(item instanceof ThermalWearable)return "clothing";
        if(item instanceof ThermalPackItem)return "portable";
        return switch(name){
            case "clay_hearth","clay_brazier","stone_stove","brick_fireplace","oil_lamp","chimney","thermal_damper" -> "heating";
            case "evaporative_cooler","evaporative_wall","ice_cooler","wind_tower","woven_canopy" -> "cooling";
            case "textile_loom","drying_rack","hide_frame","felting_board","spindle","bone_needle","wool_comb","stone_scraper" -> "textile";
            default -> "materials";
        };
    }
    private static List<ItemLike> related(String name){
        return switch(name){
            case "clay_hearth","clay_brazier","stone_stove","brick_fireplace" -> List.of(Items.COAL,Items.CHARCOAL);
            case "oil_lamp" -> List.of(ThermalContent.OIL.get());
            case "evaporative_cooler","evaporative_wall" -> List.of(Items.WATER_BUCKET);
            case "ice_cooler" -> List.of(Items.ICE,Items.PACKED_ICE,Items.BLUE_ICE);
            case "hot_stone","leather_warmer","charcoal_warmer","insulated_flask" -> List.of(Items.CAMPFIRE,Items.FURNACE);
            case "wet_cloth" -> List.of(Items.CAULDRON,Items.WATER_BUCKET);
            case "textile_loom" -> List.of(ThermalContent.THREAD.get(),ThermalContent.MAT.get());
            case "drying_rack" -> List.of(ThermalContent.WET_CLOTH.get());
            case "hide_frame" -> List.of(Items.LEATHER,ThermalContent.PREPARED_HIDE.get());
            case "felting_board" -> List.of(ThermalContent.COMBED_WOOL.get(),ThermalContent.FELT.get());
            case "spindle" -> List.of(ThermalContent.FIBER.get(),ThermalContent.THREAD.get());
            case "wool_comb" -> List.of(Items.WHITE_WOOL,ThermalContent.COMBED_WOOL.get());
            case "bone_needle" -> List.of(ThermalContent.THREAD.get(),ThermalContent.GRASS_COAT.get());
            case "stone_scraper" -> List.of(ThermalContent.RAW_FIBER.get(),ThermalContent.FIBER.get());
            default -> List.of();
        };
    }
    @Override public void register(EmiRegistry registry){
        Map<String,EmiRecipeCategory> categories=new LinkedHashMap<>();
        categories.put("heating",category(registry,"heating",ThermalContent.HEARTH_ITEM.get()));
        categories.put("cooling",category(registry,"cooling",ThermalContent.ICE_BOX_ITEM.get()));
        categories.put("textile",category(registry,"textile",ThermalContent.LOOM_ITEM.get()));
        categories.put("portable",category(registry,"portable",ThermalContent.HOT_STONE.get()));
        categories.put("clothing",category(registry,"clothing",ThermalContent.GRASS_COAT.get()));
        categories.put("materials",category(registry,"materials",ThermalContent.MAT.get()));
        for(Item item:ThermalContent.creativeItems()){
            String name=BuiltInRegistries.ITEM.getKey(item).getPath();
            EmiRecipeCategory category=categories.get(category(item,name));
            List<EmiIngredient> stacks=new ArrayList<>();
            stacks.add(EmiStack.of(item));
            for(ItemLike other:related(name))stacks.add(EmiStack.of(other));
            List<Component> lines=new ArrayList<>();
            lines.add(Component.translatable("tooltip.worldlogic_thermal.emi_hint"));
            item.appendHoverText(new ItemStack(item),Item.TooltipContext.EMPTY,lines,TooltipFlag.NORMAL);
            // Real crafting recipes are already picked up by EMI; these entries describe usage.
            registry.addRecipe(new EmiInfoRecipe(stacks,lines,id("/usage/"+name)){
                @Override public EmiRecipeCategory getCategory(){return category;}
            });
        }
    }
}
