package dev.minemods.traumacraft.content;

import dev.minemods.traumacraft.registry.ModItems;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import java.util.List;
import java.util.Locale;

/** Namespaced botanical identities. Plants are provided by optional datapack tags, never by Trauma worldgen. */
public enum HerbSpecies {
    LEGACY, CHAMOMILE, YARROW, PLANTAIN, CALENDULA, WATER_MINT, MARSHMALLOW, WILLOW_BARK, UNKNOWN;
    public static final String SPECIES="trauma_herb_species", DRY="trauma_herb_dry_seconds";
    public static final int DRY_SECONDS=180;
    public String id(){return name().toLowerCase(Locale.ROOT);}
    public boolean botanical(){return this!=LEGACY&&this!=UNKNOWN;}
    public TagKey<Item> tag(){return TagKey.create(Registries.ITEM,ResourceLocation.fromNamespaceAndPath("traumacraft","herbs/"+id()));}
    public static HerbSpecies decode(String value){
        if(value.isEmpty())return LEGACY;
        try{return valueOf(value.toUpperCase(Locale.ROOT));}catch(IllegalArgumentException ex){return UNKNOWN;}
    }
    public static HerbSpecies read(ItemStack stack){return decode(stack.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag().getString(SPECIES));}
    public static int dry(ItemStack stack){return clampDry(stack.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag().getInt(DRY));}
    public static int clampDry(int seconds){return Math.max(0,Math.min(DRY_SECONDS,seconds));}
    public static HerbSpecies plant(ItemStack stack){
        if(stack.isEmpty())return UNKNOWN;
        HerbSpecies found=UNKNOWN;
        for(var s:values())if(s.botanical()&&stack.is(s.tag())){
            if(found!=UNKNOWN)return UNKNOWN; // ambiguous datapack identity: fail closed
            found=s;
        }
        return found;
    }
    public static HerbSpecies input(ItemStack stack){
        return stack.is(ModItems.RAW_HERBS.get())?read(stack):plant(stack);
    }
    public static void write(ItemStack stack,HerbSpecies species,int dry){
        if(species==LEGACY)return;
        CompoundTag tag=stack.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag();
        tag.putString(SPECIES,species.id());tag.putInt(DRY,clampDry(dry));
        stack.set(DataComponents.CUSTOM_DATA,CustomData.of(tag));
    }
    public static void tooltip(ItemStack stack,List<Component> lines){
        var s=read(stack);
        if(s==LEGACY)return;
        lines.add(Component.translatable("tooltip.traumacraft.herb_species",Component.translatable("herb.traumacraft."+s.id())));
        if(s.botanical())lines.add(Component.translatable("tooltip.traumacraft.herb_dry",dry(stack)*100/DRY_SECONDS));
    }
    public static ItemStack powder(HerbSpecies s,int count,int dry){
        if(!s.botanical()||count<1)return ItemStack.EMPTY;
        var stack=new ItemStack(ModItems.BOTANICAL_POWDER.get(),count);write(stack,s,dry);return stack;
    }
}
