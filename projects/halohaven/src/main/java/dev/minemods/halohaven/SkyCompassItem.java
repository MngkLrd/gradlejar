package dev.minemods.halohaven;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public final class SkyCompassItem extends Item {
    public SkyCompassItem(Properties p) { super(p); }
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack held=player.getItemInHand(hand);
        if (!(player instanceof ServerPlayer sp)) return InteractionResultHolder.success(held);
        if (!level.dimension().equals(HaloHaven.HEAVEN)) {
            sp.displayClientMessage(Component.translatable("message.halohaven.compass_outside"), true);
            return InteractionResultHolder.success(held);
        }
        BlockPos p=sp.blockPosition();
        int x=Math.round(p.getX()/512f)*512;
        int z=Math.round(p.getZ()/512f)*512;
        double bearing=Math.toDegrees(Math.atan2(z+0.5-p.getZ(),x+0.5-p.getX()));
        sp.displayClientMessage(Component.literal("Sky Haven  X: "+x+"  Z: "+z+
            "  | "+(int)Math.hypot(x-p.getX(),z-p.getZ())+"m   | "+(int)bearing+"°"+
            "   | Favor: "+sp.getPersistentData().getInt("halohaven_favor")),true);
        sp.getCooldowns().addCooldown(this, 15);
        return InteractionResultHolder.success(held);
    }
}
