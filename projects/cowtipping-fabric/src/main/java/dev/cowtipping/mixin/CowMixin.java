package dev.cowtipping.mixin;
import dev.cowtipping.gameplay.CowTipState;
import dev.cowtipping.gameplay.CuriousCowGoal;
import dev.cowtipping.gameplay.TippedCowGoal;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(Cow.class)
public abstract class CowMixin extends Animal {
    protected CowMixin(EntityType<? extends Animal> type,Level level){super(type,level);}
    @Inject(method="mobInteract",at=@At("HEAD"),cancellable=true)
    private void cowtipping$interact(Player player,InteractionHand hand,CallbackInfoReturnable<InteractionResult> result){
        Cow cow=(Cow)(Object)this;
        if(hand!=InteractionHand.MAIN_HAND||!player.getItemInHand(hand).isEmpty()||player.isSpectator()||!CowTipState.canTip(cow))return;
        if(!cow.level().isClientSide())CowTipState.tip(cow,player);
        result.setReturnValue(InteractionResult.sidedSuccess(cow.level().isClientSide()));
    }
    @Inject(method="registerGoals",at=@At("TAIL"))
    private void cowtipping$registerGoals(CallbackInfo callback){
        Cow cow=(Cow)(Object)this;
        goalSelector.addGoal(0,new TippedCowGoal(cow));
        goalSelector.addGoal(3,new CuriousCowGoal(cow));
    }
}
