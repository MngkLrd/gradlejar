package dev.minemods.fragileglass.mixin;
import dev.minemods.fragileglass.gameplay.GlassRules;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Snowball;
import net.minecraft.world.entity.projectile.ThrownEgg;
import net.minecraft.world.entity.projectile.ThrownEnderpearl;
import net.minecraft.world.entity.projectile.ThrownPotion;
import net.minecraft.world.entity.projectile.ThrownExperienceBottle;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(Projectile.class)
public abstract class ProjectileMixin {
 @Inject(method="onHit",at=@At("HEAD"),cancellable=true)
 private void fragileglass$impact(HitResult result, CallbackInfo ci) {
  Projectile p=(Projectile)(Object)this;
  if(!(p.level() instanceof ServerLevel level) || !(result instanceof BlockHitResult hit)) return;
  if(p instanceof ThrownEnderpearl || p instanceof ThrownPotion || p instanceof ThrownExperienceBottle) return;
  var state=level.getBlockState(hit.getBlockPos());
  if(!GlassRules.fragile(state)) return;
  boolean pane=state.is(GlassRules.PANES), light=p instanceof Snowball || p instanceof ThrownEgg;
  double speed=p.getDeltaMovement().length();
  if(light?(!pane||speed<0.35):speed<(pane?0.35:0.65)) return;
  Entity owner=p.getOwner();
  if(GlassRules.shatter(level,hit.getBlockPos(),owner!=null?owner:p) && p instanceof AbstractArrow){
    p.setDeltaMovement(p.getDeltaMovement().scale(0.75));
    ci.cancel();
  }
 }
}
