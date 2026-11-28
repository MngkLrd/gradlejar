package dev.minemods.traumacraft.integration;

import java.lang.reflect.Method;
import java.util.Optional;
import java.util.OptionalDouble;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;

/** Read-only Core 1.2.1 contact response. Caller owns measured motion/geometry.
 * Older providers remain valid for anatomy; no invented mass, velocity, contact area or strength.
 */
public final class ContactPhysics {
    private ContactPhysics(){}
    public record Surface(String materialId,String evidence,double stoppingDistanceM,double moistureKgKg){
        public Surface {
            if(materialId==null||materialId.isEmpty()||materialId.length()>128||evidence==null||evidence.isEmpty()||evidence.length()>32||"UNKNOWN".equals(evidence)
                ||!Double.isFinite(stoppingDistanceM)||stoppingDistanceM<=0||(!Double.isNaN(moistureKgKg)&&(!Double.isFinite(moistureKgKg)||moistureKgKg<0)))
                throw new IllegalArgumentException("Invalid Core surface response");
        }
    }
    public record Contact(String materialId,double componentMassKg,double contactAreaM2,double kineticEnergyJ){}
    private record Access(Method surface,Method energy,Method item,Method distance,Method moisture,Method id,Method evidence,
        Method component,Method area,Method kinetic,Method material,Method materialId,Method mass){
        static Access find(){
            try{
                var api=Class.forName("dev.minemods.worldlogic.api.WorldLogicApi");
                if(api.getField("API_VERSION").getInt(null)!=1||api.getField("API_REVISION").getInt(null)<15)return null;
                var type=Class.forName("dev.minemods.worldlogic.api.PhysicalMaterials");
                if(type.getField("REVISION").getInt(null)<2)return null;
                var s=Class.forName(type.getName()+"$SurfaceContact");
                var i=Class.forName(type.getName()+"$ItemContact");
                var c=Class.forName(type.getName()+"$ComponentResult");
                var m=Class.forName(type.getName()+"$MaterialProfile");
                return new Access(type.getMethod("surfaceContact",ServerLevel.class,BlockPos.class),
                    type.getMethod("impactEnergyJ",double.class,double.class),
                    type.getMethod("itemContact",ItemStack.class,int.class,double.class,double.class,double.class),
                    s.getMethod("stoppingDistanceM"),s.getMethod("moistureKgKg"),s.getMethod("materialId"),s.getMethod("evidence"),
                    i.getMethod("contactingComponent"),i.getMethod("contactAreaM2"),i.getMethod("kineticEnergyJ"),
                    c.getMethod("material"),m.getMethod("id"),c.getMethod("massKg"));
            }catch(ReflectiveOperationException|LinkageError|SecurityException ex){return null;}
        }
    }
    private static class Provider{static final Access ACCESS=Access.find();}
    private static double number(Method m,Object o)throws ReflectiveOperationException{return ((Number)m.invoke(o)).doubleValue();}
    public static boolean available(){return Provider.ACCESS!=null;}
    public static Optional<Surface> surface(ServerLevel level,BlockPos pos){
        var a=Provider.ACCESS;
        if(a==null||level==null||pos==null||!level.getServer().isSameThread()||level.isOutsideBuildHeight(pos)||!level.hasChunkAt(pos))return Optional.empty();
        try{
            var value=((Optional<?>)a.surface.invoke(null,level,pos)).orElse(null);
            if(value==null)return Optional.empty();
            return Optional.of(new Surface(a.id.invoke(value).toString(),((Enum<?>)a.evidence.invoke(value)).name(),number(a.distance,value),number(a.moisture,value)));
        }catch(ReflectiveOperationException|LinkageError|RuntimeException ex){return Optional.empty();}
    }
    public static OptionalDouble impactEnergyJ(double effectiveMovingMassKg,double normalSpeedMps){
        var a=Provider.ACCESS;if(a==null)return OptionalDouble.empty();
        try{return (OptionalDouble)a.energy.invoke(null,effectiveMovingMassKg,normalSpeedMps);}
        catch(ReflectiveOperationException|LinkageError|RuntimeException ex){return OptionalDouble.empty();}
    }
    public static Optional<Contact> itemContact(ItemStack stack,int componentIndex,double contactAreaM2,double movingMassKg,double normalSpeedMps){
        var a=Provider.ACCESS;if(a==null||stack==null||stack.isEmpty())return Optional.empty();
        try{
            var value=((Optional<?>)a.item.invoke(null,stack,componentIndex,contactAreaM2,movingMassKg,normalSpeedMps)).orElse(null);
            if(value==null)return Optional.empty();
            var c=a.component.invoke(value);
            double kg=number(a.mass,c),area=number(a.area,value),energy=number(a.kinetic,value);
            if(!Double.isFinite(kg)||kg<=0||!Double.isFinite(area)||area<=0||!Double.isFinite(energy)||energy<0)return Optional.empty();
            return Optional.of(new Contact(a.materialId.invoke(a.material.invoke(c)).toString(),kg,area,energy));
        }catch(ReflectiveOperationException|LinkageError|RuntimeException ex){return Optional.empty();}
    }
}
