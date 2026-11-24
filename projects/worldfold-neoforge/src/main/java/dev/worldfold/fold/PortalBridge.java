package dev.worldfold.fold;

import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.*;
import net.minecraft.world.phys.Vec3;
import java.lang.reflect.Method;
import java.util.*;

/** API adapter pinned to the public Immersive Portals CE 6.1.3.x interface. */
public final class PortalBridge {
    public static final String TAG="worldfold2_";
    private static final String P="qouteall.imm_ptl.core.portal.Portal",A="qouteall.imm_ptl.core.api.PortalAPI",Q="qouteall.q_misc_util.my_util.DQuaternion";
    private PortalBridge(){}
    private static Class<?> portal()throws ReflectiveOperationException{return Class.forName(P);}
    private static void call(Entity e,String m,Class<?>[] types,Object...args)throws ReflectiveOperationException{portal().getMethod(m,types).invoke(e,args);}
    private static Entity make(Anchor f,Anchor t,ServerLevel l)throws ReflectiveOperationException{
        Entity e=((EntityType<?>)portal().getField("ENTITY_TYPE").get(null)).create(l);
        if(e==null)throw new IllegalStateException("Portal creation failed");
        call(e,"setOriginPos",new Class<?>[]{Vec3.class},f.center());
        call(e,"setOrientationAndSize",new Class<?>[]{Vec3.class,Vec3.class,double.class,double.class},f.axisW(),new Vec3(0,1,0),f.width(),f.height());
        call(e,"setPortalShapeToDefault",new Class<?>[0]);
        call(e,"setDestinationDimension",new Class<?>[]{ResourceKey.class},t.dimension());
        call(e,"setDestination",new Class<?>[]{Vec3.class},t.center());
        call(e,"setScaleTransformation",new Class<?>[]{double.class},t.scale()/f.scale());
        for(String m:List.of("setTeleportChangesScale","setCrossPortalCollisionEnabled","setInteractable","setTeleportable"))
            call(e,m,new Class<?>[]{boolean.class},true);
        return e;
    }
    public static void connect(MinecraftServer s,Anchor a,Anchor b,UUID generation)throws ReflectiveOperationException{
        ServerLevel al=a.level(s),bl=b.level(s);
        if(al==null||bl==null)throw new IllegalStateException("Missing dimension");
        Class<?> p=portal(),api=Class.forName(A),q=Class.forName(Q);
        Entity one=make(a,b,al),two=make(b,a,bl);
        Method orient=api.getMethod("getPortalOrientationQuaternion",p);
        Object ao=orient.invoke(null,one),bo=orient.invoke(null,two);
        call(one,"setOtherSideOrientation",new Class<?>[]{q},bo);
        call(two,"setOtherSideOrientation",new Class<?>[]{q},ao);
        Method flip=api.getMethod("createFlippedPortal",p);
        Entity back1=(Entity)flip.invoke(null,one),back2=(Entity)flip.invoke(null,two);
        for(Entity e:List.of(back1,back2)){
            call(e,"setCrossPortalCollisionEnabled",new Class<?>[]{boolean.class},true);
            call(e,"setInteractable",new Class<?>[]{boolean.class},true);
        }
        List<Entity> added=new ArrayList<>();
        try{
            for(Entity e:List.of(one,two,back1,back2)){
                e.addTag(TAG+generation);
                if(!((ServerLevel)e.level()).addFreshEntity(e))throw new IllegalStateException("Portal rejected");
                added.add(e);
            }
        }catch(RuntimeException ex){added.forEach(Entity::discard);throw ex;}
    }
    public static void removeLoaded(MinecraftServer s,Anchor a,Anchor b,UUID gen){
        for(Anchor x:List.of(a,b)){
            ServerLevel l=x.level(s);
            if(l==null||!l.hasChunkAt(x.floor()))continue;
            for(Entity e:l.getEntities((Entity)null,x.clearance().inflate(3),v->v.getTags().contains(TAG+gen)))e.discard();
        }
    }
    public static boolean occupied(MinecraftServer s,Anchor... anchors){
        for(Anchor a:anchors){
            if(a==null)continue;ServerLevel l=a.level(s);
            if(l!=null&&l.hasChunkAt(a.floor())&&!l.getEntities((Entity)null,a.clearance().inflate(.8),
                e->e instanceof LivingEntity||e instanceof net.minecraft.world.entity.item.ItemEntity||e instanceof net.minecraft.world.entity.vehicle.AbstractMinecart).isEmpty())return true;
        }
        return false;
    }
}
