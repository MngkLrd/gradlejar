package dev.minemods.thermal.server;
import dev.minemods.thermal.ThermalCondition;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.effect.MobEffectInstance;
import java.util.Locale;
final class ThermalInjury {
    private ThermalInjury(){}
    static DamageSource source(ServerPlayer p,ThermalCondition.Kind kind){
        if(kind==ThermalCondition.Kind.NONE)throw new IllegalArgumentException("No injury");
        ResourceKey<DamageType> key=ResourceKey.create(Registries.DAMAGE_TYPE,ResourceLocation.fromNamespaceAndPath("worldlogic_thermal",kind.name().toLowerCase(Locale.ROOT)));
        return new DamageSource(p.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(key));
    }
    static void apply(ServerPlayer p,ThermalCondition.Progress progress){
        if(progress.kind()==ThermalCondition.Kind.NONE)return;
        // Never clear or downgrade a stronger/longer effect from another mod.
        var existing=p.getEffect(MobEffects.WEAKNESS);
        if(existing==null || existing.getAmplifier()==0 && existing.getDuration()<25)
            p.addEffect(new MobEffectInstance(MobEffects.WEAKNESS,40,0,true,false,true));
        // Real damage event is the compatibility bridge: Trauma consumes it using
        // its normal anatomy handler; no extra direct damage to body regions.
        if(ThermalCondition.pulse(progress))p.hurt(source(p,progress.kind()),1f);
    }
}
