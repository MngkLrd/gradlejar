package dev.minemods.traumacraft.integration;
import dev.minemods.traumacraft.health.ImpactMaterial;
import java.lang.reflect.Method;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

/** Core API v1 revision >=14 / PhysicalMaterials revision >=1.
 * Keep compatibility with the pinned older Core while the new provider is undergoing QA.
 * No table of material constants, no relative-weight conversion, no gameplay mutations.
 */
public final class MaterialBridge {
    private MaterialBridge(){}
    private static final class Provider {static final Access ACCESS=Access.find();}
    private record Access(Method item,Method placed,Method mass,Method volume,Method density,Method object,Method evidence){
        static Access find(){
            try{
                var api=Class.forName("dev.minemods.worldlogic.api.WorldLogicApi");
                if(api.getField("API_VERSION").getInt(null)!=1||api.getField("API_REVISION").getInt(null)<14)return null;
                var type=Class.forName("dev.minemods.worldlogic.api.PhysicalMaterials");
                if(type.getField("REVISION").getInt(null)<1)return null;
                var result=Class.forName(type.getName()+"$Result");
                var profile=Class.forName(type.getName()+"$ObjectProfile");
                return new Access(type.getMethod("itemResult",ItemStack.class),type.getMethod("placed",ServerLevel.class,BlockPos.class),
                    result.getMethod("massKg"),result.getMethod("volumeM3"),result.getMethod("bulkDensityKgM3"),result.getMethod("object"),profile.getMethod("evidence"));
            }catch(ReflectiveOperationException|LinkageError|SecurityException ex){return null;}
        }
        Optional<ImpactMaterial> read(ImpactMaterial.Kind kind,String id,Object value)throws ReflectiveOperationException{
            String provenance=((Enum<?>)evidence.invoke(object.invoke(value))).name();
            if("UNKNOWN".equals(provenance))return Optional.empty();
            return observation(kind,id,provenance,((Number)mass.invoke(value)).doubleValue(),((Number)volume.invoke(value)).doubleValue(),((Number)density.invoke(value)).doubleValue());
        }
    }
    public static boolean available(){return Provider.ACCESS!=null;}
    public static Optional<ImpactMaterial> observation(ImpactMaterial.Kind kind,String id,String evidence,double mass,double volume,double density){
        if("UNKNOWN".equals(evidence))return Optional.empty();
        try{return Optional.of(new ImpactMaterial(kind,id,evidence,mass,volume,density));}
        catch(IllegalArgumentException ex){return Optional.empty();}
    }
    public static Optional<ImpactMaterial> item(ItemStack stack){
        var a=Provider.ACCESS;if(a==null||stack.isEmpty())return Optional.empty();
        try{return a.read(ImpactMaterial.Kind.HELD_ITEM,BuiltInRegistries.ITEM.getKey(stack.getItem()).toString(),a.item.invoke(null,stack));}
        catch(ReflectiveOperationException|LinkageError|RuntimeException ex){return Optional.empty();}
    }
    public static Optional<ImpactMaterial> surface(ServerLevel level,BlockPos pos){
        var a=Provider.ACCESS;if(a==null||!level.getServer().isSameThread()||level.isOutsideBuildHeight(pos)||!level.hasChunkAt(pos))return Optional.empty();
        var state=level.getBlockState(pos);
        if(state.isAir()||state.getCollisionShape(level,pos).isEmpty())return Optional.empty();
        try{
            Optional<?> result=(Optional<?>)a.placed.invoke(null,level,pos);
            return result.isEmpty()?Optional.empty():a.read(ImpactMaterial.Kind.LANDING_SURFACE,BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString(),result.get()).map(m->m.withSurface(ContactPhysics.surface(level,pos)));
        }catch(ReflectiveOperationException|LinkageError|RuntimeException ex){return Optional.empty();}
    }
    public static Optional<ImpactMaterial> contact(ServerPlayer victim,DamageSource source){
        if(source.is(DamageTypeTags.IS_FALL))return surface(victim.serverLevel(),victim.getOnPos());
        // Only direct melee damage. Do not assign the shooter's held item to a projectile.
        String id=source.getMsgId();
        if(("player".equals(id)||"mob".equals(id)||"mobAttack".equals(id))&&source.getDirectEntity()==source.getEntity()
            &&source.getDirectEntity() instanceof LivingEntity attacker)return item(attacker.getMainHandItem());
        return Optional.empty();
    }
}
