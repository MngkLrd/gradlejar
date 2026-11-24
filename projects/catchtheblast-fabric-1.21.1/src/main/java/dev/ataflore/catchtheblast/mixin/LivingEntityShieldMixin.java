package dev.ataflore.catchtheblast.mixin;
import dev.ataflore.catchtheblast.combat.BlastCombat;
import dev.ataflore.catchtheblast.item.BlastShieldItem;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.LargeFireball;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(LivingEntity.class)
public abstract class LivingEntityShieldMixin {
 @Unique private float catchtheblast$damage;
 @Inject(method="hurt",at=@At("HEAD"))
 private void catchtheblast$remember(DamageSource source,float amount,CallbackInfoReturnable<Boolean> ci){catchtheblast$damage=amount;}
 @Inject(method="isDamageSourceBlocked",at=@At("RETURN"),cancellable=true)
 private void catchtheblast$block(DamageSource source,CallbackInfoReturnable<Boolean> ci){
  if(!((Object)this instanceof Player p)||p.level().isClientSide||!(p.getUseItem().getItem() instanceof BlastShieldItem)||!p.isBlocking())return;
  boolean blocked=ci.getReturnValueZ();
  if(!blocked&&source.getDirectEntity() instanceof LargeFireball ball){
   var incoming=ball.getDeltaMovement().scale(-1);
   if(incoming.lengthSqr()>.0001&&incoming.normalize().dot(p.getLookAngle())>.1){blocked=true;ci.setReturnValue(true);}
  }
  if(blocked)BlastCombat.onBlock(p,source,catchtheblast$damage);
 }
}