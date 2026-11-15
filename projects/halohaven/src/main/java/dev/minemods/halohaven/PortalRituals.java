package dev.minemods.halohaven;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

/**
 * Two distinct effects:
 * 1) When the fifth ritual component is physically placed, the new portal
 *    plays a construction effect even without the Ascension Sigil.
 * 2) Right clicking the bell with the Sigil plays a 48-tick activation before
 *    dimension transfer. Both animations are server-originated and visible
 *    to EVERY nearby player (no client-only gameplay state).
 */
public final class PortalRituals {
    private PortalRituals() {}

    private record Animation(BlockPos bell, UUID playerId, int tick, boolean entering,
                             ResourceKey<Level> origin) {
        private Animation next(){return new Animation(bell,playerId,tick+1,entering,origin);}
    }
    private static final Map<GlobalPos,Animation> BUILDING=new HashMap<>();
    private static final Map<UUID,Animation> ENTERING=new HashMap<>();

    public static void onBlockPlaced(BlockEvent.EntityPlaceEvent event) {
        if(!(event.getLevel() instanceof ServerLevel level))return;
        if(level.dimension().equals(HaloHaven.HEAVEN))return;
        BlockPos changed=event.getPos();
        // Handles all possible placement orders: bell first, quartz last, or
        // one of the four amethyst corners last.
        for(int dy=0;dy<=1;dy++)for(int dx=-1;dx<=1;dx++)for(int dz=-1;dz<=1;dz++){
            BlockPos bell=changed.offset(dx,dy,dz);
            if(!level.getBlockState(bell).is(Blocks.BELL))continue;
            if(!AscensionSigilItem.ritualReady(level,bell))continue;
            GlobalPos key=GlobalPos.of(level.dimension(),bell);
            if(!BUILDING.containsKey(key)){
                BUILDING.put(key,new Animation(bell,null,0,false,level.dimension()));
                level.playSound(null,bell,SoundEvents.AMETHYST_BLOCK_CHIME,SoundSource.BLOCKS,1.3f,0.8f);
            }
            return;
        }
    }

    public static void activate(ServerPlayer player,BlockPos bell) {
        if(ENTERING.containsKey(player.getUUID()) ||
            player.getCooldowns().isOnCooldown(HaloHaven.ASCENSION_SIGIL.get()))return;
        if(player.getServer().getLevel(HaloHaven.HEAVEN)==null) {
            player.displayClientMessage(Component.translatable("message.halohaven.missing_dimension"),true);
            return;
        }
        // The gate now has a real, visible two-block-high portal surface.
        // The source bell remains untouched; expired fields clear themselves.
        ServerLevel origin=player.serverLevel();
        for(int dy=1;dy<=2;dy++){
            BlockPos at=bell.above(dy);
            origin.setBlock(at,HaloHaven.ASCENSION_GATEWAY.get().defaultBlockState()
                .setValue(TemporaryGatewayBlock.UPPER,dy==2),3);
            origin.scheduleTick(at,HaloHaven.ASCENSION_GATEWAY.get(),100);
        }
        ENTERING.put(player.getUUID(),new Animation(bell,player.getUUID(),0,true,player.serverLevel().dimension()));
        player.getCooldowns().addCooldown(HaloHaven.ASCENSION_SIGIL.get(),65);
        player.level().playSound(null,bell,SoundEvents.BEACON_ACTIVATE,SoundSource.BLOCKS,1.1f,0.7f);
        player.displayClientMessage(Component.translatable("message.halohaven.portal_opening"),true);
    }

    public static void onLevelTick(LevelTickEvent.Post event) {
        if(!(event.getLevel() instanceof ServerLevel level))return;
        if(level.dimension().equals(HaloHaven.HEAVEN))return;

        Iterator<Map.Entry<GlobalPos,Animation>> building=BUILDING.entrySet().iterator();
        while(building.hasNext()){
            var e=building.next();
            if(!e.getKey().dimension().equals(level.dimension()))continue;
            Animation a=e.getValue();
            if(a.tick>=55 || !AscensionSigilItem.ritualReady(level,a.bell)){
                building.remove();continue;
            }
            paint(level,a.bell,a.tick,false);
            if(a.tick==18||a.tick==37) {
                level.playSound(null,a.bell,SoundEvents.AMETHYST_BLOCK_RESONATE,
                    SoundSource.BLOCKS,0.9f,1.1f+a.tick/100f);
            }
            e.setValue(a.next());
        }

        Iterator<Map.Entry<UUID,Animation>> entry=ENTERING.entrySet().iterator();
        while(entry.hasNext()){
            var e=entry.next();
            Animation a=e.getValue();
            // NeoForge ticks EACH dimension in succession. Previously a tick
            // in Nether or another modded dimension cancelled every ritual
            // started in the Overworld before it could reach 48 ticks.
            if(!a.origin.equals(level.dimension()))continue;
            ServerPlayer player=level.getServer().getPlayerList().getPlayer(e.getKey());
            if(player==null || player.level()!=level ||
                player.distanceToSqr(a.bell.getX()+0.5,a.bell.getY()+0.5,a.bell.getZ()+0.5)>56 ||
                !AscensionSigilItem.ritualReady(level,a.bell)){
                eraseGate(level,a.bell);
                if(player!=null && player.level()==level)
                    player.displayClientMessage(Component.translatable("message.halohaven.portal_interrupted"),true);
                entry.remove();continue;
            }
            paint(level,a.bell,a.tick,true);
            if(a.tick==19 || a.tick==35)
                level.playSound(null,a.bell,SoundEvents.BEACON_POWER_SELECT,
                    SoundSource.BLOCKS,1.0f,1.0f+a.tick/100f);
            if(a.tick>=48){
                // Remove BEFORE invoking worldgen / teleport to prevent duplicate entry.
                entry.remove();
                eraseGate(level,a.bell);
                ServerLevel heaven=level.getServer().getLevel(HaloHaven.HEAVEN);
                if(heaven==null)continue;
                player.getPersistentData().putDouble("halohaven_return_x",player.getX());
                player.getPersistentData().putDouble("halohaven_return_y",player.getY());
                player.getPersistentData().putDouble("halohaven_return_z",player.getZ());
                paint(level,a.bell,48,true);
                HavenVillages.ensure(heaven,0,0);
                player.teleportTo(heaven,0.5,HavenVillages.WALK_Y+1.0,5.5,
                    player.getYRot(),player.getXRot());
                player.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING,400,0,false,true));
                player.displayClientMessage(Component.translatable("message.halohaven.welcome"),false);
            }else{
                e.setValue(a.next());
            }
        }
    }

    private static void eraseGate(ServerLevel level,BlockPos bell) {
        for(int dy=1;dy<=2;dy++){
            BlockPos at=bell.above(dy);
            if(level.getBlockState(at).is(HaloHaven.ASCENSION_GATEWAY.get()))
                level.removeBlock(at,false);
        }
    }

    private static void paint(ServerLevel level,BlockPos bell,int tick,boolean activation) {
        if((tick&1)!=0)return;  // 10 ring points every two ticks, not 600 packets/tick.
        double progress=Math.min(1.0,tick/(activation?48d:55d));
        double radius=activation?1.3+0.9*progress:2.2-0.9*progress;
        for(int i=0;i<10;i++){
            double angle=i*Math.PI*2/10+tick*0.12;
            double xx=bell.getX()+0.5+Math.cos(angle)*radius;
            double zz=bell.getZ()+0.5+Math.sin(angle)*radius;
            double yy=bell.getY()+0.3+0.07*tick+0.17*Math.sin(angle*3+tick*.11);
            level.sendParticles(activation?ParticleTypes.END_ROD:ParticleTypes.ENCHANT,
                xx,yy,zz,1,0.03,0.05,0.03,0.01);
        }
        if(tick%8==0) {
            level.sendParticles(ParticleTypes.CLOUD,bell.getX()+.5,bell.getY()+.9,
                bell.getZ()+.5,9,1.1,0.35,1.1,0.025);
            level.sendParticles(ParticleTypes.PORTAL,bell.getX()+.5,bell.getY()+1.4,
                bell.getZ()+.5,12,0.85,1.0,0.85,0.08);
        }
        if(tick>=40) {
            level.sendParticles(ParticleTypes.END_ROD,bell.getX()+.5,bell.getY()+2.3,
                bell.getZ()+.5,6,0.5,1.0,0.5,0.02);
        }
    }
}
