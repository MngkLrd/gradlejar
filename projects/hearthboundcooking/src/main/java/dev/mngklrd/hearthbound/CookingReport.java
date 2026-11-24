package dev.mngklrd.hearthbound;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.player.Player;

/** Five-point penalties are persisted with the actual prepared dish. */
public record CookingReport(int under,int over,int heat,int late,int prior) {
    public static final String KEY="hearthbound_report";
    public CookingReport {
        under=penalty(under);over=Math.min(100-under,penalty(over));
        heat=Math.min(100-under-over,penalty(heat));
        late=Math.min(100-under-over-heat,penalty(late));
        prior=Math.min(100-under-over-heat-late,penalty(prior));
    }
    private static int penalty(int p){return QualityMath.quantize(p);}
    public int score(){return 100-under-over-heat-late-prior;}
    public static CookingReport fresh(int inherited){return new CookingReport(0,0,0,0,inherited<0?0:Math.round((100-inherited)*.2f));}
    public static CookingReport read(ItemStack s){
        if(!Quality.has(s)||!s.getOrCreateTag().contains(KEY,10))return null;
        CompoundTag t=s.getTag().getCompound(KEY);
        CookingReport r=new CookingReport(t.getInt("Under"),t.getInt("Over"),t.getInt("Heat"),t.getInt("Late"),t.getInt("Prior"));
        return r.score()==Quality.get(s)?r:null;
    }
    public void apply(ItemStack s){
        if(!Quality.eligible(s))return;
        Quality.set(s,score());
        CompoundTag t=new CompoundTag();
        t.putInt("Under",under);t.putInt("Over",over);t.putInt("Heat",heat);t.putInt("Late",late);t.putInt("Prior",prior);
        s.getOrCreateTag().put(KEY,t);
    }
    public static void late(ItemStack s,int loss){
        if(!Quality.has(s)||!Quality.fresh(s))return;
        CookingReport old=read(s);
        if(old==null)old=new CookingReport(0,0,0,0,100-Quality.get(s));
        new CookingReport(old.under,old.over,old.heat,old.late+loss,old.prior).apply(s);
    }
    public static int stars(int q){return q>=95?5:q>=80?4:q>=60?3:q>=40?2:q>=20?1:0;}
    public static String starsText(int q){int n=stars(q);return "★".repeat(n)+"☆".repeat(5-n);}
    public static String tier(int q){return switch(stars(q)){case 5->"excellent";case 4->"good";case 3->"ordinary";case 2->"mediocre";case 1->"poor";default->"ruined";};}
    public static void notify(Player p,ItemStack s){
        if(Quality.has(s))p.displayClientMessage(Component.translatable("hearthbound.result",starsText(Quality.get(s)),Quality.get(s)).withStyle(ChatFormatting.GOLD),true);
    }
}
