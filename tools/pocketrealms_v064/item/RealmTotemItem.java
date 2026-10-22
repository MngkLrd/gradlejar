package com.kirill.pocketrealms.item;

import com.kirill.pocketrealms.world.ModDimensions;
import com.kirill.pocketrealms.world.PocketTeleporter;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerPlayer;

/** One-use overworld expedition: allocates a brand-new sector, no door needed. */
public final class RealmTotemItem extends Item {
    public RealmTotemItem(Properties properties){super(properties);}
    @Override public InteractionResultHolder<ItemStack> use(Level level,Player player,InteractionHand hand){
        ItemStack held=player.getItemInHand(hand);
        if(!level.dimension().equals(Level.OVERWORLD))
            return InteractionResultHolder.fail(held);
        if(level.isClientSide())return InteractionResultHolder.success(held);
        if(!(player instanceof ServerPlayer serverPlayer))return InteractionResultHolder.fail(held);
        if(player.getCooldowns().isOnCooldown(this))return InteractionResultHolder.fail(held);
        if(!PocketTeleporter.startTotemRun(serverPlayer))
            return InteractionResultHolder.fail(held);
        player.getCooldowns().addCooldown(this,20*12);
        if(!player.getAbilities().instabuild)held.shrink(1);
        return InteractionResultHolder.success(held);
    }
}
