package dev.mngklrd.hearthbound;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.dries007.tfc.common.capabilities.food.*;
import net.dries007.tfc.common.capabilities.heat.HeatCapability;

public final class Quality {
    public static final String KEY="hearthbound_quality";
    public static final TagKey<Item> UNGRADED=TagKey.create(Registries.ITEM,new ResourceLocation(Hearthbound.ID,"ungraded_outputs"));
    public static final TagKey<Item> SAME_ITEM=TagKey.create(Registries.ITEM,new ResourceLocation(Hearthbound.ID,"prepared_same_item"));
    private Quality(){}
    public static boolean has(ItemStack s){return !s.isEmpty()&&s.hasTag()&&s.getTag().contains(KEY,99)&&!s.is(UNGRADED);}
    public static int get(ItemStack s){return has(s)?Mth.clamp(s.getTag().getInt(KEY),0,100):70;}
    public static boolean food(ItemStack s){return !s.isEmpty()&&(s.isEdible()||FoodCapability.get(s)!=null);}
    public static boolean fresh(ItemStack s){IFood f=FoodCapability.get(s);return f==null||!f.isRotten();}
    public static boolean eligible(ItemStack s){return food(s)&&!s.is(UNGRADED)&&fresh(s);}
    public static void set(ItemStack s,int q){if(eligible(s))s.getOrCreateTag().putInt(KEY,QualityMath.quantize(q));}
    public static float saturationMultiplier(int q){return QualityMath.saturationMultiplier(q);}
    /** Raw, ungraded inputs contribute no inherited grade. Zero is a real grade. */
    public static int inherited(Iterable<ItemStack> inputs){
        long sum=0,weight=0;
        for(ItemStack s:inputs)if(food(s)&&has(s)&&fresh(s)){
            int n=s.getCount();sum+=(long)get(s)*n;weight+=n;
        }
        return weight==0?-1:(int)Math.round(sum/(double)weight);
    }
    public static int score(int process,Iterable<ItemStack> inputs){return QualityMath.finish(process,inherited(inputs));}
    public static void finish(ItemStack output,int process,Iterable<ItemStack> inputs){if(eligible(output))set(output,score(process,inputs));}
    public static ItemStack withoutGrade(ItemStack s){
        ItemStack copy=s.copy();
        if(copy.hasTag()&&copy.getTag().contains(KEY)){
            copy.getTag().remove(KEY);
            if(copy.getTag().isEmpty())copy.setTag(null);
        }
        return copy;
    }
    public static boolean furnaceCompatible(ItemStack existing,ItemStack result){
        if(!has(existing)&&!has(result))return ItemStack.isSameItemSameTags(existing,result);
        if(existing.getItem()!=result.getItem()||!food(existing)||!food(result))return ItemStack.isSameItemSameTags(existing,result);
        return ItemStack.isSameItemSameTags(withoutGrade(existing),withoutGrade(result));
    }
    public static boolean ingredientCompatible(ItemStack a,ItemStack b){
        if(has(a)!=has(b))return false;
        return FoodCapability.areStacksStackableExceptCreationDate(withoutGrade(a),withoutGrade(b));
    }
    public static boolean transformed(ItemStack input,ItemStack output){
        return !input.isEmpty()&&eligible(output)&&(input.getItem()!=output.getItem()||output.is(SAME_ITEM)||has(output));
    }
    public static void heating(ItemStack input,ItemStack output,float target){
        if(!transformed(input,output))return;
        completedHeating(input,output,target);
    }
    /** Use only for an appliance's explicit successful food preparation. */
    public static void completedHeating(ItemStack input,ItemStack output,float target){
        if(input.isEmpty()||!eligible(output)||!fresh(input))return;
        int process=QualityMath.heating(HeatCapability.getTemperature(input),target);
        if(process>=0)finish(output,process,java.util.List.of(input));
    }
    public static int role(ItemStack s){
        IFood f=FoodCapability.get(s);if(f==null)return 1;
        FoodData d=f.getData();
        if(d.protein()>Math.max(d.vegetables(),d.fruit())&&d.protein()>=d.grain())return 0;
        if(d.grain()>Math.max(d.fruit(),d.vegetables()))return 1;
        return 2;
    }
    public static int stageScore(int role,int warmTicks,int duration){
        double ratio=warmTicks/(double)Math.max(1,duration);
        double ideal=role==0?1.55:role==1?1.2:1.0;
        return Mth.clamp((int)Math.round(100-Math.abs(ratio-ideal)*42),0,100);
    }
}
