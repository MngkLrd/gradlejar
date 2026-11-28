package dev.minemods.metabolism;
import java.lang.reflect.Method;
import java.util.Optional;
import net.minecraft.world.item.ItemStack;
/** Optional Aqua public API. Never reads catch/portion private data, never calls a second intake. */
final class AquaFoodBridge {
 record Portion(String species,int grams,boolean cooked){}
 private record Access(Method read,Method species,Method mass,Method prep){
  static Access find(){
   try{
    Class<?> api=Class.forName("dev.minemods.aquaculture.api.AquaApi");
    if(api.getField("API_VERSION").getInt(null)!=1)return null;
    Class<?> type=Class.forName(api.getName()+"$FoodPortion");
    return new Access(api.getMethod("foodPortion",ItemStack.class),type.getMethod("speciesId"),type.getMethod("edibleMassGrams"),type.getMethod("preparation"));
   }catch(ReflectiveOperationException|LinkageError|RuntimeException ex){return null;}
  }
  Optional<Portion> query(ItemStack stack){
   try{
    Optional<?> value=(Optional<?>)read.invoke(null,stack);if(value.isEmpty())return Optional.empty();
    Object p=value.get();String id=(String)species.invoke(p);int grams=((Number)mass.invoke(p)).intValue();
    Object preparation=prep.invoke(p);if(!(preparation instanceof Enum<?> kind))return Optional.empty();
    boolean cooked=kind.name().equals("COOKED");if(!cooked&&!kind.name().equals("RAW"))return Optional.empty();
    if(id==null||grams<1||grams>(cooked?80:100))return Optional.empty();
    return Optional.of(new Portion(id,grams,cooked));
   }catch(ReflectiveOperationException|LinkageError|RuntimeException ex){return Optional.empty();}
  }
 }
 private static final Access API=Access.find();
 static Optional<Portion> read(ItemStack stack){return API==null?Optional.empty():API.query(stack);}
 private AquaFoodBridge(){}
}
