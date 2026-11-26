package dev.minemods.worldlogic;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.phys.Vec3;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.effect.*;
import net.minecraftforge.event.entity.living.*;
public final class Combat {
 public static void knockback(LivingKnockBackEvent e){if(Rules.on("knockback_weight")&&e.getEntity().level() instanceof ServerLevel)e.setStrength((float)(e.getStrength()*Math.max(.55,Math.min(1.2,1/Math.sqrt(Material.mass(e.getEntity()))))));}
 public static void hurt(LivingHurtEvent e){var ent=e.getEntity();if(!(ent.level() instanceof ServerLevel l))return;
  if(Rules.on("projectile_force")&&e.getSource().getDirectEntity() instanceof AbstractArrow a){double speed=a.getDeltaMovement().length(); if(speed>0)e.setAmount(e.getAmount()*(float)Math.max(.88,Math.min(1.12,.88+speed*.08)));}
  if(Rules.on("creeper_shockwave")&&e.getSource().is(DamageTypeTags.IS_EXPLOSION)&&e.getSource().getEntity() instanceof Creeper c){Vec3 away=ent.position().subtract(c.position());if(away.lengthSqr()>.01){Vec3 impulse=away.normalize().scale(.14/Math.sqrt(Material.mass(ent)));ent.push(impulse.x,.04,impulse.z);ent.hurtMarked=true;}}
  if(Rules.on("sharp_blocks")&&e.getSource().is(net.minecraft.world.damagesource.DamageTypes.CACTUS)){double speed=ent.getDeltaMovement().horizontalDistance();e.setAmount(e.getAmount()*(float)(1+Math.min(1,speed*2)));}
 }
 public static void fall(LivingFallEvent e){var x=e.getEntity();if(!(x.level() instanceof ServerLevel l))return;float d=e.getDistance();BlockState s=l.getBlockState(x.getOnPos());
  if(Rules.on("soft_landing")&&Material.block(s,"soft_landing")&&!s.is(Blocks.HAY_BLOCK))e.setDamageMultiplier(e.getDamageMultiplier()*.72f);
  if(Rules.on("landing_recovery")&&x instanceof Player p&&d>5&&!p.isCreative()&&!p.isSpectator()){int t=Math.min(20,Math.round((d-5)*2));p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN,Math.max(4,t),0,false,false,false));Body.state(p).recoverUntil=l.getGameTime()+t;}
 }
 public static void shield(ShieldBlockEvent e){if(!Rules.on("shield_momentum")||!(e.getEntity().level() instanceof ServerLevel)||!(e.getDamageSource().getDirectEntity() instanceof LivingEntity a)||!a.isSprinting())return;
  var v=e.getEntity();Vec3 d=v.position().subtract(a.position());if(d.lengthSqr()<.01)return;double m=Math.min(.35,.13*Material.mass(a)/Material.mass(v));v.push(d.normalize().x*m,.02,d.normalize().z*m);v.hurtMarked=true;a.setSprinting(false);}
 public static void visibility(LivingEvent.LivingVisibilityEvent e){if(Rules.on("armor_noise")&&e.getEntity() instanceof Player p&&p.level() instanceof ServerLevel&&p.isSprinting())e.modifyVisibility(Material.armorNoise(p));}
}
