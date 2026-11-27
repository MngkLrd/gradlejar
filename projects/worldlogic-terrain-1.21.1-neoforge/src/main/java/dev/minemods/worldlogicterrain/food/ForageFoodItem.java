package dev.minemods.worldlogicterrain.food;

import java.util.List;
import dev.minemods.worldlogic.api.WorldLogicApi;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

public final class ForageFoodItem extends Item {
    private final double recovery;
    public ForageFoodItem(int hunger,float saturation,double recovery) {
        super(new Item.Properties().food(new FoodProperties.Builder().nutrition(hunger).saturationModifier(saturation).build()));
        this.recovery=recovery;
    }
    @Override public ItemStack finishUsingItem(ItemStack stack,Level level,LivingEntity entity) {
        boolean recover= !level.isClientSide && entity instanceof ServerPlayer p && p.canEat(false) && !stack.isEmpty() && recovery>0;
        ItemStack result=super.finishUsingItem(stack,level,entity);
        if(recover && entity instanceof ServerPlayer p)WorldLogicApi.recoverExertion(p,recovery);
        return result;
    }
    @Override public void appendHoverText(ItemStack stack,Item.TooltipContext context,List<Component> lines,TooltipFlag flag) {
        super.appendHoverText(stack,context,lines,flag);
        if(recovery>0)lines.add(Component.translatable("tooltip.worldlogicterrain.exertion",(int)recovery));
    }
}
