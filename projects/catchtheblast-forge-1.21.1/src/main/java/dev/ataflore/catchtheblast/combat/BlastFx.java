package dev.ataflore.catchtheblast.combat;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

final class BlastFx {
    private BlastFx() {}
    static void capture(ServerLevel level, Player player, int charge) {
        Vec3 look = player.getLookAngle();
        Vec3 center = player.getEyePosition().add(look.scale(0.8)).add(0,-0.45,0);
        for (int i = 0; i < 34; i++) {
            double a = i * 2.39996;
            double y = (i / 33.0 - 0.5) * 1.9;
            double r = 0.55 + 0.45 * Math.sin(i * 1.7) * Math.sin(i * 1.7);
            Vec3 p = center.add(Math.cos(a)*r, y, Math.sin(a)*r);
            Vec3 v = center.subtract(p).normalize().scale(0.14);
            level.sendParticles(ParticleTypes.FLAME, p.x,p.y,p.z,0,v.x,v.y,v.z,1);
        }
        level.sendParticles(ParticleTypes.ELECTRIC_SPARK, center.x,center.y,center.z,12,.22,.27,.22,.06);
        level.playSound(null, player.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.0f, charge > 100 ? .65f : 1.0f+charge*.004f);
    }
    static void release(ServerLevel level, Player player, Vec3 dir, double range, float strength) {
        Vec3 origin = player.getEyePosition().add(0,-.45,0);
        Vec3 right = dir.cross(new Vec3(0,1,0)).normalize();
        if (right.lengthSqr() < .1) right = new Vec3(1,0,0);
        Vec3 up = right.cross(dir).normalize();
        for (int ring = 1; ring <= 5; ring++) {
            double distance = range * ring / 5.0;
            double radius = .25 + distance * .53;
            Vec3 center = origin.add(dir.scale(distance));
            for (int i = 0; i < 18; i++) {
                double a = i * Math.PI * 2 / 18;
                Vec3 p = center.add(right.scale(Math.cos(a)*radius)).add(up.scale(Math.sin(a)*radius));
                Vec3 velocity = dir.scale(.055 + .07*strength);
                level.sendParticles(i%3==0 ? ParticleTypes.ELECTRIC_SPARK : ParticleTypes.FLAME,p.x,p.y,p.z,0,velocity.x,velocity.y,velocity.z,1);
            }
        }
        Vec3 p = origin.add(dir);
        level.sendParticles(ParticleTypes.CLOUD,p.x,p.y,p.z,12,.3,.3,.3,.12);
        level.playSound(null, player.blockPosition(), SoundEvents.WARDEN_SONIC_BOOM, SoundSource.PLAYERS, .65f+strength*.3f, 1.25f-strength*.3f);
    }
    static void warning(ServerLevel level, Player player) {
        Vec3 p = player.getEyePosition().add(player.getLookAngle().scale(.7)).add(0,-.55,0);
        level.sendParticles(ParticleTypes.ELECTRIC_SPARK,p.x,p.y,p.z,8,.23,.3,.23,.08);
        level.sendParticles(ParticleTypes.SMOKE,p.x,p.y,p.z,3,.15,.2,.15,.025);
        level.playSound(null, player.blockPosition(), SoundEvents.FIRECHARGE_USE, SoundSource.PLAYERS,.25f,1.7f);
    }
}
