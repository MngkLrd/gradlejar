package dev.minemods.riverweir.logic;

import dev.minemods.riverweir.RiverWeirMod;
import dev.minemods.riverweir.block.WeirNetBlock;
import dev.minemods.riverweir.blockentity.WeirPostBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.AbstractFish;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.items.ItemStackHandler;
import java.util.UUID;

/** Live fish contacts a mesh, struggles for several seconds, then becomes stored catch. */
public final class WeirCapture {
    private static final String CLAIM="RiverWeirHoldUntil";
    private static final double CONTACT_MARGIN=.16;
    private WeirCapture(){}
    public record Hold(UUID fish,BlockPos mesh,Vec3 anchor,long started,int duration,boolean alongX){}

    private static boolean intact(ServerLevel level,BlockPos p){
        if(!level.hasChunkAt(p))return false;
        BlockState state=level.getBlockState(p);
        return state.is(RiverWeirMod.NET.get())&&state.getValue(WeirNetBlock.STAGE)<2
            &&!state.getFluidState().isEmpty();
    }
    private static AABB contactBox(BlockPos mesh,boolean x){
        return x?new AABB(mesh.getX(),mesh.getY(),mesh.getZ()+7/16.0,
            mesh.getX()+1,mesh.getY()+1,mesh.getZ()+9/16.0).inflate(CONTACT_MARGIN):
            new AABB(mesh.getX()+7/16.0,mesh.getY(),mesh.getZ(),
            mesh.getX()+9/16.0,mesh.getY()+1,mesh.getZ()+1).inflate(CONTACT_MARGIN);
    }
    private static ItemStackHandler plan(WeirPostBlockEntity be,ItemStack caught){
        if(caught.isEmpty())return null;
        ItemStackHandler trial=new ItemStackHandler(be.getTier().storage);
        for(int i=0;i<trial.getSlots();i++)trial.setStackInSlot(i,be.items.getStackInSlot(i).copy());
        ItemStack left=caught.copy();
        for(int i=0;i<trial.getSlots()&&!left.isEmpty();i++)left=trial.insertItem(i,left,false);
        return left.isEmpty()?trial:null;
    }
    public static void tryContact(ServerLevel level,WeirPostBlockEntity be){
        long now=level.getGameTime();
        if(be.hold!=null||be.other==null||be.wear>=be.getTier().endurance||be.clog>=90
            ||now-be.lastCatch<be.getTier().cooldownTicks)return;
        BlockPos first=be.getBlockPos(),second=be.other;
        boolean x=first.getZ()==second.getZ();
        int dx=Integer.signum(second.getX()-first.getX()),dz=Integer.signum(second.getZ()-first.getZ());
        int count=Math.abs(second.getX()-first.getX())+Math.abs(second.getZ()-first.getZ())-1;
        if(count<1||count>32||(!x&&first.getX()!=second.getX()))return;
        AABB search=new AABB(Math.min(first.getX(),second.getX())-1,be.netY-be.maxDepth()-1,
            Math.min(first.getZ(),second.getZ())-1,Math.max(first.getX(),second.getX())+2,
            be.netY+2,Math.max(first.getZ(),second.getZ())+2);
        for(AbstractFish fish:level.getEntitiesOfClass(AbstractFish.class,search,f->f.isAlive()&&f.isInWater())){
            if(fish.getPersistentData().getLong(CLAIM)>now)continue;
            AABB box=fish.getBoundingBox();
            BlockPos mesh=null;
            // Check real hitbox contact with the occupied plane, not the fish's center.
            for(int col=0;col<count&&mesh==null;col++){
                int px=first.getX()+dx*(col+1),pz=first.getZ()+dz*(col+1);
                if(box.maxX<px-.16||box.minX>px+1.16||box.maxZ<pz-.16||box.minZ>pz+1.16)continue;
                for(int d=0;d<be.depthAt(col);d++){
                    BlockPos p=new BlockPos(px,be.netY-d,pz);
                    if(contactBox(p,x).intersects(box)&&intact(level,p)){mesh=p;break;}
                }
            }
            if(mesh==null)continue;
            float chance=Math.min(.95f,be.getTier().catchChance*(be.baitCharges>0?1.8f:1f)*(1f-be.clog/140f));
            if(level.random.nextFloat()>=chance||plan(be,WeirLogic.catchItem(fish))==null)continue;
            int duration=100+level.random.nextInt(81);
            be.hold=new Hold(fish.getUUID(),mesh.immutable(),fish.position(),now,duration,x);
            fish.getPersistentData().putLong(CLAIM,now+duration+20);
            fish.setDeltaMovement(Vec3.ZERO);
            fish.hurtMarked=true;
            return;
        }
    }
    public static void release(ServerLevel level,WeirPostBlockEntity be){
        Hold h=be.hold;
        be.hold=null;
        if(h==null)return;
        Entity fish=level.getEntity(h.fish());
        if(fish!=null)fish.getPersistentData().remove(CLAIM);
    }
    public static void tick(ServerLevel level,WeirPostBlockEntity be){
        Hold h=be.hold;
        if(h==null)return;
        long now=level.getGameTime();
        Entity entity=level.getEntity(h.fish());
        if(!(entity instanceof AbstractFish fish)||!fish.isAlive()||!fish.isInWater()
            ||be.wear>=be.getTier().endurance||be.clog>=90||!intact(level,h.mesh())
            ||fish.distanceToSqr(h.anchor().x,h.anchor().y,h.anchor().z)>2.25){
            release(level,be);return;
        }
        if(now-h.started()>=h.duration()){
            ItemStackHandler trial=plan(be,WeirLogic.catchItem(fish));
            if(trial==null){release(level,be);be.lastCatch=now;be.setChanged();return;}
            for(int i=0;i<trial.getSlots();i++)be.items.setStackInSlot(i,trial.getStackInSlot(i).copy());
            fish.discard();
            be.hold=null;
            be.markCatch(h.mesh());
            be.lastCatch=now;
            be.clog=Math.min(100,be.clog+be.getTier().clogPerCatch);
            if(be.baitCharges>0)be.baitCharges--;
            be.setChanged();
            WeirLogic.damage(level,be,be.getTier().catchWear+(level.random.nextInt(3)==0?1:0));
            return;
        }
        // Keep natural AI enabled. Only gently tether position/velocity while the controller ticks.
        // If the chunk unloads, no permanent AI, gravity or persistence flags are left on the fish.
        double phase=(now-h.started())*.65;
        Vec3 target=h.anchor().add(h.alongX()?Math.sin(phase)*.055:0,Math.sin(phase*.7)*.035,h.alongX()?0:Math.sin(phase)*.055);
        Vec3 delta=target.subtract(fish.position()).scale(.65);
        fish.setDeltaMovement(delta);
        if(now%4==0){
            if(level.noCollision(fish,fish.getBoundingBox().move(target.subtract(fish.position()))))fish.setPos(target.x,target.y,target.z);
            fish.hurtMarked=true;
        }
        if(now%8==0)level.sendParticles(ParticleTypes.BUBBLE,fish.getX(),fish.getY()+fish.getBbHeight()*.5,fish.getZ(),2,.12,.12,.12,.015);
    }
}
