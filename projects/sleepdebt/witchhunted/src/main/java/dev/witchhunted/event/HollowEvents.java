package dev.witchhunted.event;

import dev.witchhunted.WitchHunted;
import dev.witchhunted.registry.ModContent;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.Skeleton;
import net.minecraft.world.entity.monster.Witch;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

public final class HollowEvents {
    private HollowEvents() {}

    public static void onDrops(LivingDropsEvent event) {
        if (!(event.getEntity() instanceof Witch witch) || !(witch.level() instanceof ServerLevel level)) return;
        if (level.random.nextFloat() >= 0.20F) return;
        event.getDrops().add(new ItemEntity(level, witch.getX(), witch.getY()+0.5, witch.getZ(),
            new ItemStack(ModContent.WITCH_HAT_ITEM.get())));
    }

    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (!player.level().dimension().equals(WitchHunted.THE_HOLLOW)) return;
        ServerLevel level = player.serverLevel();
        if (player.tickCount % 100 != 0 || player.isSpectator() || player.isCreative()) return;
        double dist = player.position().horizontalDistance();
        if (dist < 8 || dist > 156) return;
        AABB area = player.getBoundingBox().inflate(30, 14, 30);
        if (level.getEntitiesOfClass(Monster.class,area).size() >= 14) return;

        for(int attempt=0; attempt<8; attempt++) {
            double angle = level.random.nextDouble()*Math.PI*2;
            int range=12+level.random.nextInt(10);
            BlockPos pos=new BlockPos((int)Math.floor(player.getX()+Math.cos(angle)*range),
                                      2,
                                      (int)Math.floor(player.getZ()+Math.sin(angle)*range));
            if (!level.getBlockState(pos).isAir() || !level.getBlockState(pos.above()).isAir()) continue;
            if (!level.getBlockState(pos.below()).isSolidRender(level,pos.below())) continue;

            Mob mob;
            if(level.random.nextInt(5)==0) {
                Skeleton skeleton=EntityType.SKELETON.create(level);
                if(skeleton==null) return;
                skeleton.setCustomName(Component.literal("THE WITNESS"));
                skeleton.setItemSlot(EquipmentSlot.HEAD,new ItemStack(Items.CARVED_PUMPKIN));
                mob=skeleton;
            } else {
                Zombie zombie=EntityType.ZOMBIE.create(level);
                if(zombie==null) return;
                zombie.setCustomName(Component.literal("THE HUNTED"));
                zombie.setItemSlot(EquipmentSlot.HEAD,new ItemStack(Items.CARVED_PUMPKIN));
                if(zombie.getAttribute(Attributes.MOVEMENT_SPEED)!=null)
                    zombie.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0.31D);
                mob=zombie;
            }
            mob.moveTo(pos.getX()+0.5D,pos.getY(),pos.getZ()+0.5D,level.random.nextFloat()*360,0);
            mob.setTarget(player);
            level.addFreshEntity(mob);
            if(level.random.nextInt(4)==0)
                player.sendSystemMessage(Component.translatable("message.witchhunted.seen"));
            return;
        }
    }
}
