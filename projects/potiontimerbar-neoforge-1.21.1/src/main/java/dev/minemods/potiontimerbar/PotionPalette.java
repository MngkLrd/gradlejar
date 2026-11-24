package dev.minemods.potiontimerbar;

import java.util.HashMap;
import java.util.Map;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.alchemy.Potion;

/** Resolve the default liquid color of a registered potion, not the HUD icon's color. */
final class PotionPalette {
    private static Map<Holder<MobEffect>, Integer> potionColors;
    private PotionPalette() {}

    static Gradient forEffect(Holder<MobEffect> effect) {
        if (potionColors == null) potionColors = indexPotions();
        Integer liquid = potionColors.get(effect);
        if (liquid == null) {
            return switch (effect.value().getCategory()) {
                case HARMFUL -> new Gradient(0xFF9292, 0xCA373F, 0x54111C);
                case BENEFICIAL -> new Gradient(0xE0FFAE, 0x75D54E, 0x235C27);
                case NEUTRAL -> new Gradient(0xE1F2A2, 0xA7B959, 0x4B5123);
            };
        }

        // Preserve the potion hue from the first to the final pixel.
        int base = liquid & 0xFFFFFF;
        int peak = Math.max((base >> 16) & 255, Math.max((base >> 8) & 255, base & 255));
        if (peak < 96) base = mix(base, 0xFFFFFF, .20f);
        return new Gradient(mix(base, 0xFFFFFF, .58f), base, mix(base, 0x000000, .50f));
    }

    private static Map<Holder<MobEffect>, Integer> indexPotions() {
        Map<Holder<MobEffect>, Integer> single = new HashMap<>();
        Map<Holder<MobEffect>, Integer> multi = new HashMap<>();
        for (Potion potion : BuiltInRegistries.POTION) {
            var effects = potion.getEffects();
            if (effects.isEmpty()) continue;
            // Vanilla potion liquid: RGB average weighted by visible effect amplifier + 1.
            long red = 0, green = 0, blue = 0, weight = 0;
            for (MobEffectInstance instance : effects) {
                if (!instance.isVisible()) continue;
                int color = instance.getEffect().value().getColor();
                long w = (long) instance.getAmplifier() + 1L;
                red += ((color >> 16) & 255) * w;
                green += ((color >> 8) & 255) * w;
                blue += (color & 255) * w;
                weight += w;
            }
            if (weight == 0) continue;
            int rgb = ((int)(red / weight) << 16) | ((int)(green / weight) << 8) | (int)(blue / weight);
            Map<Holder<MobEffect>, Integer> target = effects.size() == 1 ? single : multi;
            for (MobEffectInstance instance : effects) target.putIfAbsent(instance.getEffect(), rgb);
        }
        // A dedicated potion is more representative than a mixed potion.
        single.forEach(multi::put);
        return Map.copyOf(multi);
    }

    static int mix(int a, int b, float t) {
        t = Math.max(0, Math.min(1, t));
        int r = Math.round(((a >> 16) & 255) * (1-t) + ((b >> 16) & 255) * t);
        int g = Math.round(((a >> 8) & 255) * (1-t) + ((b >> 8) & 255) * t);
        int b2 = Math.round((a & 255) * (1-t) + (b & 255) * t);
        return (r << 16) | (g << 8) | b2;
    }

    record Gradient(int light, int middle, int dark) {
        int at(float t) {
            return t < .5f ? mix(light, middle, t * 2) : mix(middle, dark, (t - .5f) * 2);
        }
    }
}
