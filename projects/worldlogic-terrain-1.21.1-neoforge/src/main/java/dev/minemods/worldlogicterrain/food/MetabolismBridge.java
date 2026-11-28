package dev.minemods.worldlogicterrain.food;

import net.neoforged.fml.ModList;

/** Optional peer capability. Normal FOOD completion is owned by Metabolism v1 rev4+. */
public final class MetabolismBridge {
    private MetabolismBridge(){}
    private static volatile Boolean cached;
    public static boolean supportsAutomaticFood(int version,int revision){return version==1&&revision>=4;}
    public static boolean automaticFood(){
        Boolean value=cached;
        if(value!=null)return value;
        if(!ModList.get().isLoaded("metabolism")){cached=false;return false;}
        try{
            Class<?> api=Class.forName("dev.minemods.metabolism.api.MetabolismApi",false,MetabolismBridge.class.getClassLoader());
            boolean compatible=supportsAutomaticFood(api.getField("API_VERSION").getInt(null),api.getField("API_REVISION").getInt(null));
            cached=compatible;
            return compatible;
        }catch(ClassNotFoundException|NoSuchFieldException oldApi){
            cached=false;return false;
        }catch(IllegalAccessException ex){
            throw new IllegalStateException("Cannot read Metabolism API version",ex);
        }
    }
}
