package dev.minemods.traumacraft.integration;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.Potions;
import net.neoforged.fml.ModList;
import java.util.Optional;
import java.lang.reflect.Method;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;

/** External use of ONE complete vanilla bottle. Never modifies foreign liquid NBT or drinking balance. */
public final class SurfaceWater {
    private SurfaceWater(){}
    public static boolean waterBottle(ItemStack stack){
        var contents=stack.get(DataComponents.POTION_CONTENTS);
        return stack.is(Items.POTION)&&contents!=null&&contents.is(Potions.WATER);
    }
    public static boolean cleanProfile(int volume,String quality,double salinity,double risk){
        return volume==250&&"DRINKABLE".equals(quality)&&Double.isFinite(salinity)&&salinity>=0&&salinity<1&&Double.isFinite(risk)&&risk>=0&&risk<=.10;
    }
    private record Access(Method read,Method volume,Method quality,Method salt,Method risk){
        static Access resolve(){
            try{
                var api=Class.forName("dev.minemods.hydration.api.HydrationApi");
                if(api.getField("API_VERSION").getInt(null)!=1||api.getField("API_REVISION").getInt(null)<2)return null;
                var status=Class.forName("dev.minemods.hydration.api.HydrationApi$LiquidStatus");
                return new Access(api.getMethod("bottleWater",ItemStack.class),status.getMethod("volumeMb"),status.getMethod("quality"),status.getMethod("salinityGkg"),status.getMethod("contaminationRisk"));
            }catch(ReflectiveOperationException|LinkageError|RuntimeException ex){return null;}
        }
        boolean clean(ItemStack bottle){
            try{
                var opt=(Optional<?>)read.invoke(null,bottle);if(opt.isEmpty())return false;
                var s=opt.get();return cleanProfile(((Number)volume.invoke(s)).intValue(),(String)quality.invoke(s),((Number)salt.invoke(s)).doubleValue(),((Number)risk.invoke(s)).doubleValue());
            }catch(ReflectiveOperationException|LinkageError|RuntimeException ex){return false;}
        }
    }
    private static class Provider{static final Access API=Access.resolve();static final Withdrawal TX=Withdrawal.resolve();}
    private record Withdrawal(Method preview,Method commit,Method water,Method volume,Method quality,Method salt,Method risk){
        static Withdrawal resolve(){
            try{
                var api=Class.forName("dev.minemods.hydration.api.HydrationApi");
                if(api.getField("API_VERSION").getInt(null)!=1||api.getField("API_REVISION").getInt(null)<5)return null;
                var quote=Class.forName("dev.minemods.hydration.WaterWithdrawal$Quote");
                var status=Class.forName("dev.minemods.hydration.api.HydrationApi$LiquidStatus");
                return new Withdrawal(api.getMethod("previewWithdrawal",ServerPlayer.class,InteractionHand.class,int.class),api.getMethod("withdrawExact",ServerPlayer.class,quote),quote.getMethod("water"),status.getMethod("volumeMb"),status.getMethod("quality"),status.getMethod("salinityGkg"),status.getMethod("contaminationRisk"));
            }catch(ReflectiveOperationException|LinkageError|RuntimeException ex){return null;}
        }
        Optional<?> quote(ServerPlayer p){
            try{
                var opt=(Optional<?>)preview.invoke(null,p,InteractionHand.MAIN_HAND,250);
                if(opt.isEmpty())return Optional.empty();
                var s=water.invoke(opt.get());
                return cleanProfile(((Number)volume.invoke(s)).intValue(),(String)quality.invoke(s),((Number)salt.invoke(s)).doubleValue(),((Number)risk.invoke(s)).doubleValue())?opt:Optional.empty();
            }catch(ReflectiveOperationException|LinkageError|RuntimeException ex){return Optional.empty();}
        }
        boolean debit(ServerPlayer p,Object quote){
            try{return ((Optional<?>)commit.invoke(null,p,quote)).isPresent();}
            catch(ReflectiveOperationException|LinkageError|RuntimeException ex){return false;}
        }
    }
    /** Read-only client recognition; only the server quote authorizes a withdrawal. */
    public static boolean candidate(ItemStack stack){
        if(waterBottle(stack))return true;
        if(!ModList.get().isLoaded("hydration"))return false;
        try{
            var api=Class.forName("dev.minemods.hydration.api.HydrationApi");
            return ((Optional<?>)api.getMethod("liquidWater",ItemStack.class).invoke(null,stack)).isPresent();
        }catch(ReflectiveOperationException|LinkageError|RuntimeException ex){return false;}
    }
    public static boolean transactional(){return ModList.get().isLoaded("hydration")&&Provider.TX!=null;}
    public static Optional<?> preview(ServerPlayer p){return transactional()?Provider.TX.quote(p):Optional.empty();}
    public static boolean withdraw(ServerPlayer p,Object quote){return transactional()&&Provider.TX.debit(p,quote);}
    public static boolean clean(ItemStack bottle){
        if(!waterBottle(bottle))return false;
        if(!ModList.get().isLoaded("hydration"))return true;
        var api=Provider.API;return api!=null&&api.clean(bottle);
    }
}
