package dev.anvilborn.forge;

import dev.anvilborn.registry.ModComponents;
import net.minecraft.world.item.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.util.RandomSource;
import java.util.ArrayList;
import java.util.List;

public final class ForgeRules {
    public static final int TARGET_HITS = 6;
    public static final int MAX_LEVEL = 10;
    public static ForgeData data(ItemStack stack) { return stack.getOrDefault(ModComponents.FORGE_DATA.get(), ForgeData.FRESH); }
    public static String family(ItemStack stack) {
        Item item = stack.getItem();
        if (item instanceof ArmorItem) return "armor";
        if (item instanceof BowItem || item instanceof CrossbowItem || item instanceof TridentItem) return "ranged";
        if (item instanceof SwordItem) return "weapon";
        if (item instanceof DiggerItem) return "tool";
        return "any";
    }
    public static boolean eligible(ItemStack stack) {
        return !stack.isEmpty() && stack.getCount() == 1 && stack.isDamageableItem() && stack.getMaxStackSize() == 1
            && !(stack.getItem() instanceof ElytraItem);
    }
    public static int breakChance(int quality) {
        if (quality <= 0) return 0;
        if (quality <= 2) return 3;
        if (quality <= 4) return 8;
        if (quality <= 6) return 15;
        if (quality <= 8) return 24;
        return 35;
    }
    public static int maxAspects(int quality) {
        if (quality <= 0) return 1;
        if (quality <= 2) return 1;
        if (quality <= 4) return 2;
        return 3;
    }
    public static int strike(ItemStack stack, RandomSource random) {
        // 0 = unchanged, 1 = improved, 2 = great success, 3 = worsened, 4 = shattered, 5 = capped
        ForgeData previous = data(stack);
        if (previous.level() >= MAX_LEVEL) return 5;
        int level = previous.level();
        int hits = previous.hits();
        int frenzy = previous.frenzy();
        int chance = frenzy > 0 ? 0 : breakChance(level);
        if (random.nextInt(100) < chance) return 4;
        int outcome = random.nextInt(100);
        int result;
        if (outcome < 12) { level = Math.max(-2, level - 1); result = 3; }
        else if (outcome < 28) { result = 0; }
        else if (outcome < 91) { level = Math.min(MAX_LEVEL, level + 1); result = 1; }
        else { level = Math.min(MAX_LEVEL, level + 2); result = 2; }

        List<ForgeData.Roll> aspects = new ArrayList<>(previous.aspects());
        if (level > 0 && aspects.size() < maxAspects(level) && (level >= 1 && previous.level() < 1
             || level >= 3 && previous.level() < 3 || level >= 5 && previous.level() < 5
             || level >= 8 && previous.level() < 8)) {
            ForgeData.Roll roll = Aspects.roll(random, family(stack), aspects, false, level);
            if (roll != null) aspects.add(roll);
        }
        if (level < 0 && aspects.isEmpty()) {
            ForgeData.Roll roll = Aspects.roll(random, family(stack), aspects, true, 1);
            if (roll != null) aspects.add(roll);
        }
        // The progress counter persists on the item, preventing swapping to exploit Frenzy.
        if (frenzy > 0) frenzy--;
        else if (++hits >= TARGET_HITS) { hits = 0; frenzy = 2; }
        stack.set(ModComponents.FORGE_DATA.get(), new ForgeData(level, hits, frenzy, aspects));
        return result;
    }
    private ForgeRules() {}
}
