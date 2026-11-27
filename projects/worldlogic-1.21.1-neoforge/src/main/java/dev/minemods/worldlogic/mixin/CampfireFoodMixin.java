package dev.minemods.worldlogic.mixin;
import dev.minemods.worldlogic.HotFood;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Containers;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.core.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import net.minecraft.world.level.block.entity.CampfireBlockEntity;
@Mixin(value=CampfireBlockEntity.class,remap=false)
public abstract class CampfireFoodMixin {
 @Redirect(method="cookTick",at=@At(value="INVOKE",target="Lnet/minecraft/world/Containers;dropItemStack(Lnet/minecraft/world/level/Level;DDDLnet/minecraft/world/item/ItemStack;)V"),remap=false,require=0)
 private static void worldlogic$cook(Level level,double x,double y,double z,ItemStack stack){
  if(level instanceof ServerLevel l)HotFood.stamp(stack,HotFood.now(l)+1200);
  Containers.dropItemStack(level,x,y,z,stack);
 }
}
