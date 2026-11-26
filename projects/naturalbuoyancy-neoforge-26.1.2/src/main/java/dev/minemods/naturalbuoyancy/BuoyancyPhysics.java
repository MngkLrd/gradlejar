package dev.minemods.naturalbuoyancy;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

public final class BuoyancyPhysics {
    private BuoyancyPhysics(){}
    public static void tick(ItemEntity item) {
        if(!(item.level() instanceof ServerLevel)||item.isRemoved()||!item.isInWater()) return;
        ItemStack stack=item.getItem();
        if(stack.isEmpty()) return;
        DensityRules.Kind kind=classify(stack);
        if(kind==DensityRules.Kind.VANILLA) return;
        Vec3 v=item.getDeltaMovement();
        double next=v.y;
        if(kind==DensityRules.Kind.FLOAT) {
            if(item.getFluidHeight(FluidTags.WATER)>0.23D) next=Math.min(0.075D,v.y+0.009D);
        } else if(item.onGround()) {
            next=Math.max(0D,Math.min(v.y,0.005D));
        } else {
            double terminal=kind==DensityRules.Kind.SINK?-0.105D:-0.040D;
            double acceleration=kind==DensityRules.Kind.SINK?0.018D:0.008D;
            next=Math.max(terminal,v.y-acceleration);
        }
        if(next!=v.y) item.setDeltaMovement(v.x,next,v.z);
    }
    private static DensityRules.Kind classify(ItemStack stack) {
        boolean sink=false,slow=false,light=false,taggedSink=false,taggedLight=false;
        var tags=stack.getItemHolder().tags().iterator();
        while(tags.hasNext()) {
            String name=tags.next().location().toString();
            switch(DensityRules.override(name)) {
                case SINK -> sink=true;
                case SLOW_SINK -> slow=true;
                case FLOAT -> light=true;
                default -> {}
            }
            switch(DensityRules.materialTag(name)) {
                case SINK -> taggedSink=true;
                case FLOAT -> taggedLight=true;
                default -> {}
            }
        }
        if(sink) return DensityRules.Kind.SINK;
        if(slow) return DensityRules.Kind.SLOW_SINK;
        if(light) return DensityRules.Kind.FLOAT;
        if(taggedSink) return DensityRules.Kind.SINK;
        if(taggedLight) return DensityRules.Kind.FLOAT;
        return DensityRules.byPath(BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath());
    }
}
