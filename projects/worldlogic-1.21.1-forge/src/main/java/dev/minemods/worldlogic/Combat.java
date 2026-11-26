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
 public static void knockback(LivingKnockBackEvent e){if(Rules.on("knockback_weight")&&e.getEntity().level() instanceof ServerLevel)e.setStrength((float)(e.getStrength()*Math.max(.45,Math.min(1.35,1+(1/Math.sqrt(Material.mass(e.getEntity()))-1)*Rules.strength("impact")))));}
 public static void hurt(LivingHurtEvent e){var ent=e.getEntity();if(!(ent.level() instanceof ServerLevel l))return;
  if(Rules.on("projectile_force")&&e.getSource().getDirectEntity() instanceof AbstractArrow a){double speed=a.getDeltaMovement().length(); if(speed>0){Impulse impact=new Impulse(a.getDeltaMovement(),a instanceof net.minecraft.world.entity.projectile.ThrownTrident?.8:.2);e.setAmount(e.getAmount()*(float)Math.max(.9,Math.min(1.12,.90+impact.energy()*.15)));Vec3 v=impact.velocity().normalize().scale(impact.transfer(ent)*.20);ent.push(v.x,Math.max(0,v.y)*.15,v.z);ent.hurtMarked=true;}}
  if(Rules.on("creeper_shockwave")&&e.getSource().is(DamageTypeTags.IS_EXPLOSION)&&e.getSource().getEntity() instanceof Creeper c){Vec3 away=ent.position().subtract(c.position());if(away.lengthSqr()>.01){Vec3 impulse=away.normalize().scale(.14/Math.sqrt(Material.mass(ent)));ent.push(impulse.x,.04,impulse.z);ent.hurtMarked=true;}}
  if(Rules.on("sharp_blocks")&&e.getSource().is(net.minecraft.world.damagesource.DamageTypes.CACTUS)){double speed=ent.getDeltaMovement().horizontalDistance();e.setAmount(e.getAmount()*(float)(1+Math.min(1,speed*2)));}
 }
 public static void fall(LivingFallEvent e){var x=e.getEntity();if(!(x.level() instanceof ServerLevel l))return;float d=e.getDistance();BlockState s=l.getBlockState(x.getOnPos());
  if(Rules.on("soft_landing")&&Material.block(s,"soft_landing")&&!s.is(Blocks.HAY_BLOCK))e.setDamageMultiplier(e.getDamageMultiplier()*.72f);
  if(Rules.on("landing_recovery")&&x instanceof Player p&&d>5&&!p.isCreative()&&!p.isSpectator()){int t=Math.min(20,Math.round((d-5)*2));Body.state(p).recoverUntil=l.getGameTime()+t;}
 }
 public static void shield(ShieldBlockEvent e){if(!Rules.on("shield_momentum")||!(e.getEntity().level() instanceof ServerLevel)||!(e.getDamageSource().getDirectEntity() instanceof LivingEntity a)||!a.isSprinting())return;
  var v=e.getEntity();Vec3 d=v.position().subtract(a.position());if(d.lengthSqr()<.01)return;double m=Math.min(.35,new Impulse(a.getDeltaMovement(),Material.mass(a)).transfer(v)*.7);v.push(d.normalize().x*m,.02,d.normalize().z*m);v.hurtMarked=true;a.setSprinting(false);}
 public static void contact(ServerLevel l,Player p,Body.State s,long now){
  Vec3 v=p.getDeltaMovement();if(Rules.on("shield_momentum")&&p.isBlocking()&&now>=s.shieldUntil){
   for(Player a:l.getEntitiesOfClass(Player.class,p.getBoundingBox().inflate(.40),a->a!=p&&a.isSprinting()&&!a.isSpectator()&&!a.isCreative())){
    Vec3 delta=p.position().subtract(a.position());if(delta.lengthSqr()<.01||a.getDeltaMovement().dot(delta.normalize())<.07||p.getLookAngle().dot(delta.normalize().scale(-1))<.35||!a.canHarmPlayer(p))continue;
    double strength=new Impulse(a.getDeltaMovement(),Material.mass(a)).transfer(p)*.65;Vec3 push=delta.normalize().scale(strength);
    p.push(push.x,.02,push.z);p.hurtMarked=true;a.setDeltaMovement(a.getDeltaMovement().multiply(.45,1,.45));a.hurtMarked=true;a.setSprinting(false);s.shieldUntil=now+14;break;
   }
  }
  if(Rules.on("sharp_blocks")&&now>=s.sharpUntil&&v.horizontalDistance()>.18){
   for(net.minecraft.core.BlockPos q:net.minecraft.core.BlockPos.betweenClosed(p.blockPosition().offset(-1,0,-1),p.blockPosition().offset(1,0,1))){
    if(!l.hasChunkAt(q)||!Material.block(l.getBlockState(q),"sharp")||l.getBlockState(q).is(Blocks.CACTUS)||l.getBlockState(q).is(Blocks.POINTED_DRIPSTONE))continue;
    var shape=l.getBlockState(q).getCollisionShape(l,q);if(!shape.isEmpty()&&shape.bounds().move(q.getX(),q.getY(),q.getZ()).inflate(.04).intersects(p.getBoundingBox())){p.hurt(l.damageSources().generic(),(float)Math.min(3,1+v.horizontalDistance()*3));s.sharpUntil=now+15;break;}
   }
  }
 }
 public static void visibility(LivingEvent.LivingVisibilityEvent e){if(Rules.on("armor_noise")&&e.getEntity() instanceof Player p&&p.level() instanceof ServerLevel&&p.isSprinting())e.modifyVisibility(Material.armorNoise(p));}
}
