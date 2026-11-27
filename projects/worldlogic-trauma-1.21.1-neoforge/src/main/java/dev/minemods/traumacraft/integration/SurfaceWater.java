package dev.minemods.traumacraft.integration;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.Potions;
import net.neoforged.fml.ModList;
import java.util.Optional;
import java.lang.reflect.Method;

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
    private static class Provider{static final Access API=Access.resolve();}
    public static boolean clean(ItemStack bottle){
        if(!waterBottle(bottle))return false;
        if(!ModList.get().isLoaded("hydration"))return true;
        var api=Provider.API;return api!=null&&api.clean(bottle);
    }
}
