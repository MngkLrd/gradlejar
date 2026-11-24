package dev.mngklrd.hearthbound;

import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.dries007.tfc.common.capabilities.food.*;
import net.dries007.tfc.common.capabilities.heat.HeatCapability;

public final class Quality {
    public static final String KEY="hearthbound_quality";
    private Quality(){}
    public static boolean has(ItemStack s){return !s.isEmpty()&&s.hasTag()&&s.getTag().contains(KEY);}
    public static int get(ItemStack s){return has(s)?Mth.clamp(s.getTag().getInt(KEY),0,100):70;}
    public static void set(ItemStack s,int q){
        if(!s.isEmpty()&&food(s)) s.getOrCreateTag().putInt(KEY,Mth.clamp(Math.round(q/5f)*5,0,100));
    }
    public static boolean food(ItemStack s){return !s.isEmpty()&&(s.isEdible()||FoodCapability.get(s)!=null);}
    public static boolean fresh(ItemStack s){IFood f=FoodCapability.get(s);return f==null||!f.isRotten();}
    public static float saturationMultiplier(int q){q=Mth.clamp(q,0,100);return q<=70?0.8f+q*(0.2f/70f):1f+(q-70)*(0.1f/30f);}
    public static void heating(ItemStack input,ItemStack output,float target){
        if(output.isEmpty()||!food(output)||!fresh(input))return;
        float actual=HeatCapability.getTemperature(input);
        if(target<=0||actual<target*0.85f)return; // do not grade recipe viewers / simulated cold results
        float excess=Math.max(0,actual-target);
        set(output,Math.round(100-Math.max(0,excess-target*0.15f)*0.18f));
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
