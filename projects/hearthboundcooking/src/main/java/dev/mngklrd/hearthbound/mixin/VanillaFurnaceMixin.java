package dev.mngklrd.hearthbound.mixin;
import dev.mngklrd.hearthbound.Quality;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.AbstractFurnaceBlock;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractFurnaceBlockEntity.class)
public abstract class VanillaFurnaceMixin {
    @Unique private static final ThreadLocal<int[]> hearthbound$before=new ThreadLocal<>();
    @Unique private static boolean hearthbound$eligible(AbstractFurnaceBlockEntity be){return be instanceof FurnaceBlockEntity||be instanceof SmokerBlockEntity;}
    @Inject(method="serverTick",at=@At("HEAD"))
    private static void hearthbound$before(Level l,BlockPos p,BlockState s,AbstractFurnaceBlockEntity be,CallbackInfo ci){
        if(!hearthbound$eligible(be))return;
        ItemStack out=be.getItem(2);int score=Quality.has(out)?Quality.get(out):-1;
        hearthbound$before.set(new int[]{out.getCount(),score});
        // Remove only our quality tag inside the synchronous vanilla recipe comparison.
        if(score>=0&&out.getTag()!=null){
            CompoundTag tag=out.getTag();tag.remove(Quality.KEY);if(tag.isEmpty())out.setTag(null);
        }
    }
    @Inject(method="serverTick",at=@At("RETURN"))
    private static void hearthbound$after(Level l,BlockPos p,BlockState s,AbstractFurnaceBlockEntity be,CallbackInfo ci){
        if(!hearthbound$eligible(be))return;
        int[] before=hearthbound$before.get();hearthbound$before.remove();if(before==null)return;
        ItemStack out=be.getItem(2);CompoundTag data=be.getPersistentData();int q=before[1];
        if(out.isEmpty()||!Quality.food(out)){data.remove("hearthbound_hold");return;}
        if(out.getCount()>before[0]){q=before[0]==0?100:q<0?70:q;if(before[0]==0)data.putInt("hearthbound_hold",0);}
        if(q<0)return;
        int hold=data.getInt("hearthbound_hold");
        if(s.hasProperty(AbstractFurnaceBlock.LIT)&&s.getValue(AbstractFurnaceBlock.LIT)&&q>0){
            if(++hold>=200){q=Math.max(0,q-5);hold=0;be.setChanged();}
            data.putInt("hearthbound_hold",hold);
        }
        Quality.set(out,q);if(q!=before[1])be.setChanged();
    }
}
