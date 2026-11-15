package dev.minemods.halohaven;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.level.Level;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.network.chat.Component;

public final class Skyfolk extends Villager {
    public Skyfolk(EntityType<? extends Villager> type, Level level) {
        super(type, level);
        this.setCustomName(Component.translatable("entity.halohaven.skyfolk"));
        this.setCustomNameVisible(false);
    }
    public void populateSkyTrades() {
        if (!this.getOffers().isEmpty()) return;
        this.getOffers().add(new MerchantOffer(new ItemCost(Items.AMETHYST_SHARD, 3),
            new ItemStack(HaloHaven.SKYBERRY.get(), 5), 16, 2, 0.03f));
        this.getOffers().add(new MerchantOffer(new ItemCost(Items.EMERALD, 5),
            new ItemStack(HaloHaven.STARDUST.get(), 2), 9, 4, 0.05f));
        this.getOffers().add(new MerchantOffer(new ItemCost(HaloHaven.STARDUST.get(), 4),
            new ItemStack(HaloHaven.SUNSHARD.get(), 1), 7, 6, 0.07f));
    }
}
