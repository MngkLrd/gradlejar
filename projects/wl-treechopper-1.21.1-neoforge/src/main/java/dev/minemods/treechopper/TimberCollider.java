package dev.minemods.treechopper;

import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Interaction;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

/** Non-pickable, transient physical proxy. The coordinator owns all timber and yield. */
public final class TimberCollider extends Interaction {
    private static final EntityDataAccessor<Float> DEPTH=SynchedEntityData.defineId(TimberCollider.class,EntityDataSerializers.FLOAT);
    UUID owner;
    public TimberCollider(EntityType<? extends TimberCollider> type,Level level){super(type,level);setNoGravity(true);}
    @Override protected void defineSynchedData(SynchedEntityData.Builder builder){super.defineSynchedData(builder);builder.define(DEPTH,1f);}
    @Override public boolean canBeCollidedWith(){return isAlive();}
    @Override public boolean isPickable(){return false;}
    @Override public boolean canBeHitByProjectile(){return false;}
    @Override public boolean isPushable(){return false;}
    @Override protected AABB makeBoundingBox(){
        AABB box=super.makeBoundingBox();
        float half=getEntityData().get(DEPTH)*.5f;
        return new AABB(box.minX,box.minY,getZ()-half,box.maxX,box.maxY,getZ()+half);
    }
    @Override public void onSyncedDataUpdated(EntityDataAccessor<?> key){super.onSyncedDataUpdated(key);if(key.equals(DEPTH))setBoundingBox(makeBoundingBox());}
    void setBox(AABB box){
        CompoundTag config=new CompoundTag();config.putFloat("width",(float)box.getXsize());config.putFloat("height",(float)box.getYsize());config.putBoolean("response",false);
        readAdditionalSaveData(config);getEntityData().set(DEPTH,(float)box.getZsize());
        setPos(box.getCenter().x,box.minY,box.getCenter().z);setBoundingBox(makeBoundingBox());
    }
    @Override public void tick(){
        super.tick();
        if(level() instanceof ServerLevel l&&(owner==null||l.getEntity(owner)==null))discard();
    }
}
