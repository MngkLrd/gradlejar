package dev.anvilborn.event;

import dev.anvilborn.forge.*;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.ChatFormatting;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.effect.*;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.ItemAttributeModifierEvent;
import net.neoforged.neoforge.event.entity.player.*;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.entity.living.LivingFallEvent;

public final class ForgeEvents {
    @SubscribeEvent
    public void openAnvil(PlayerInteractEvent.RightClickBlock event) {
        Player player = event.getEntity();
        if (!player.isShiftKeyDown() || !player.level().getBlockState(event.getPos()).is(BlockTags.ANVIL)) return;
        if (event.getHand() != InteractionHand.MAIN_HAND) return;
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.sidedSuccess(player.level().isClientSide));
        if (!player.level().isClientSide) {
            BlockPos pos = event.getPos().immutable();
            player.openMenu(new SimpleMenuProvider(
                (id, inventory, p) -> new ForgeMenu(id, inventory, true, pos),
                Component.translatable("menu.anvilborn.forge")
            ));
        }
    }

    @SubscribeEvent
    public void tooltip(ItemTooltipEvent event) {
        ItemStack item = event.getItemStack();
        if (!item.has(dev.anvilborn.registry.ModComponents.FORGE_DATA.get())) return;
        ForgeData data = ForgeRules.data(item);
        ChatFormatting color = data.level() >= 8 ? ChatFormatting.GOLD : data.level() >= 5 ? ChatFormatting.LIGHT_PURPLE
            : data.level() >= 3 ? ChatFormatting.BLUE : data.level() >= 1 ? ChatFormatting.GREEN : data.level() < 0
            ? ChatFormatting.RED : ChatFormatting.GRAY;
        event.getToolTip().add(Component.translatable("tooltip.anvilborn.quality", data.level()).withStyle(color));
        event.getToolTip().add(Component.translatable("tooltip.anvilborn.hits", data.hits(), ForgeRules.TARGET_HITS)
            .withStyle(ChatFormatting.DARK_GRAY));
        for (ForgeData.Roll roll : data.aspects()) {
            Aspect a = Aspects.byId(roll.id());
            if (a != null) event.getToolTip().add(Component.translatable(a.translationKey())
                .append(Component.literal(" " + "I".repeat(roll.rank())))
                .withStyle(a.harmful() ? ChatFormatting.RED : ChatFormatting.AQUA));
        }
    }

    private static double strength(ForgeData.Roll roll, Aspect a, ForgeData data) {
        double scale = 1.0 + Math.max(0, data.level()) * 0.055;
        return a.amount() * roll.rank() * scale;
    }
    private static double sum(ItemStack item, String stat) {
        ForgeData d = ForgeRules.data(item);
        double amount = 0.0;
        for (ForgeData.Roll roll : d.aspects()) {
            Aspect a = Aspects.byId(roll.id());
            if (a != null && a.effect().equals(stat)) amount += strength(roll, a, d);
        }
        return Math.clamp(amount, -0.35, 0.50);
    }

    @SubscribeEvent
    public void attributes(ItemAttributeModifierEvent event) {
        ItemStack stack = event.getItemStack();
        if (!stack.has(dev.anvilborn.registry.ModComponents.FORGE_DATA.get())) return;
        String family = ForgeRules.family(stack);
        EquipmentSlotGroup slot = family.equals("armor") && stack.getItem() instanceof ArmorItem armor
            ? EquipmentSlotGroup.bySlot(armor.getEquipmentSlot()) : EquipmentSlotGroup.MAINHAND;
        apply(event, Attributes.ATTACK_DAMAGE, "damage", stack, slot);
        apply(event, Attributes.ATTACK_SPEED, "speed", stack, slot);
        apply(event, Attributes.ARMOR, "armor", stack, slot);
        apply(event, Attributes.ARMOR_TOUGHNESS, "toughness", stack, slot);
        apply(event, Attributes.MOVEMENT_SPEED, "movement", stack, slot);
        apply(event, Attributes.MAX_HEALTH, "health", stack, slot);
        apply(event, Attributes.ATTACK_KNOCKBACK, "knockback", stack, slot);
        apply(event, Attributes.KNOCKBACK_RESISTANCE, "resistance", stack, slot);
        apply(event, Attributes.LUCK, "luck", stack, slot);
    }

    private static void apply(ItemAttributeModifierEvent event, net.minecraft.core.Holder<Attribute> attr,
                              String kind, ItemStack stack, EquipmentSlotGroup slot) {
        double power = sum(stack, kind);
        if (power == 0) return;
        AttributeModifier.Operation operation =
            kind.equals("luck") || kind.equals("resistance") || kind.equals("knockback") || kind.equals("toughness")
                ? AttributeModifier.Operation.ADD_VALUE : AttributeModifier.Operation.ADD_MULTIPLIED_BASE;
        if (kind.equals("luck")) power *= 3.0;
        event.addModifier(attr, new AttributeModifier(ResourceLocation.fromNamespaceAndPath("anvilborn", kind),
            power, operation), slot);
    }

    @SubscribeEvent
    public void mining(PlayerEvent.BreakSpeed event) {
        ItemStack tool = event.getEntity().getMainHandItem();
        if (!tool.has(dev.anvilborn.registry.ModComponents.FORGE_DATA.get())) return;
        double bonus = sum(tool, "mining");
        if (event.getPosition().isPresent() && event.getPosition().get().getY() < 0)
            bonus += sum(tool, "deep_mining");
        event.setNewSpeed(Math.max(0, event.getNewSpeed() * (1.0f + (float)bonus)));
    }


    @SubscribeEvent
    public void bonusExperience(BlockEvent.BreakEvent event) {
        if (event.isCanceled() || event.getPlayer() == null || event.getPlayer().level().isClientSide) return;
        ItemStack stack = event.getPlayer().getMainHandItem();
        applyDurabilityAspect(event.getPlayer(), stack);
    }

    @SubscribeEvent
    public void bonusBlockExperience(net.neoforged.neoforge.event.level.BlockDropsEvent event) {
        if (event.isCanceled() || !(event.getBreaker() instanceof Player)) return;
        int base = event.getDroppedExperience();
        double bonus = Math.max(0, sum(event.getTool(), "xp"));
        if (bonus > 0 && base > 0)
            event.setDroppedExperience(Math.min(200, (int)Math.ceil(base * (1 + bonus))));
    }

    @SubscribeEvent
    public void softenFall(LivingFallEvent event) {
        if (!(event.getEntity() instanceof Player player) || player.level().isClientSide) return;
        double softness = 0;
        for (ItemStack piece : player.getArmorSlots()) softness += Math.max(0, sum(piece, "fall"));
        if (softness > 0)
            event.setDamageMultiplier(event.getDamageMultiplier() * Math.max(0.50f, 1.0f - (float)softness));
    }

    private static void applyDurabilityAspect(Player player, ItemStack item) {
        if (!item.isDamageableItem()) return;
        double value = sum(item, "preserve");
        int wear = item.getDamageValue();
        if (value > 0 && wear > 0 && player.getRandom().nextDouble() < Math.min(0.3, value)) {
            // Refund one durability just before the normal action consumes its durability.
            item.setDamageValue(wear - 1);
        } else if (value < 0 && wear + 2 < item.getMaxDamage()
                   && player.getRandom().nextDouble() < Math.min(0.3, -value)) {
            item.setDamageValue(wear + 1);
        }
    }

    @SubscribeEvent
    public void damage(LivingDamageEvent.Pre event) {
        if (event.getEntity().level().isClientSide) return;
        float amount = event.getNewDamage();
        Entity dealer = event.getSource().getEntity();
        if (dealer instanceof LivingEntity attacker) {
            boolean ranged = event.getSource().getDirectEntity() != dealer;
            ItemStack weapon = attacker.getMainHandItem();
            ForgeData data = ForgeRules.data(weapon);
            if (ranged) amount *= 1.0f + (float)sum(weapon, "ranged");
            // Melee damage comes from ItemAttributeModifierEvent already; avoid double application.
            if (event.getEntity().getHealth() <= event.getEntity().getMaxHealth() * 0.35f)
                amount *= 1.0f + (float)sum(weapon, "execute");
            if (attacker instanceof Player player && !ranged) applyDurabilityAspect(player, weapon);
            if (!weapon.isEmpty() && data.level() > 0) {
                double fire = sum(weapon, "fire");
                double bleed = sum(weapon, "bleed");
                double poison = sum(weapon, "poison");
                double slow = sum(weapon, "slow");
                if (fire > 0 && attacker.getRandom().nextFloat() < fire) event.getEntity().igniteForSeconds(3);
                if (bleed > 0 && attacker.getRandom().nextFloat() < bleed)
                    event.getEntity().addEffect(new MobEffectInstance(MobEffects.WITHER, 60, 0));
                if (poison > 0 && attacker.getRandom().nextFloat() < poison)
                    event.getEntity().addEffect(new MobEffectInstance(MobEffects.POISON, 60, 0));
                if (slow > 0 && attacker.getRandom().nextFloat() < slow)
                    event.getEntity().addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 0));
                double leech = sum(weapon, "leech");
                if (leech > 0 && attacker.getRandom().nextFloat() < 0.24f)
                    attacker.heal(Math.min(1.5f, amount * (float)leech));
            }
        }
        if (event.getEntity() instanceof Player victim) {
            double guard = 0;
            for (ItemStack armor : victim.getArmorSlots()) guard += sum(armor, "guard");
            amount *= Math.max(0.65f, 1.0f - (float)guard);
        }
        event.setNewDamage(Math.max(0.0f, amount));
    }
}
