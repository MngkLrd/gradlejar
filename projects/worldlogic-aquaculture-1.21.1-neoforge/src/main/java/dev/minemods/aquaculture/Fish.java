package dev.minemods.aquaculture;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Cod;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Items;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;

public final class Fish extends Cod {
 public Fish(EntityType<? extends Cod> type,Level level){super(type,level);}
 @Override protected InteractionResult mobInteract(Player player,InteractionHand hand){
  // Transport is not implemented; prevent vanilla cod bucket identity conversion.
  if(player.getItemInHand(hand).is(Items.WATER_BUCKET))return InteractionResult.FAIL;
  Species s=Aquaculture.species(this);
  var food=player.getItemInHand(hand);
  if(s!=null&&!food.isEmpty()&&food.is(FishFeeding.foodTag(s))){
   if(level().isClientSide)return InteractionResult.SUCCESS;
   var result=FishFeeding.offer(this,s,food,player.getAbilities().instabuild);
   if(result==FishFeeding.Result.FED){
    player.displayClientMessage(Component.translatable("aquaculture.feed.fed",FishFeeding.satiety(this,level().getGameTime())),true);
    return InteractionResult.CONSUME;
   }
   player.displayClientMessage(Component.translatable("aquaculture.feed."+result.name().toLowerCase(java.util.Locale.ROOT)),true);
   return InteractionResult.FAIL;
  }
  return super.mobInteract(player,hand);
 }
 @Override public void tick(){
  super.tick();
  if(!level().isClientSide&&tickCount==1){
   Species s=Aquaculture.species(this);
   if(s!=null)Aquaculture.traits(this,s);
  }
 }
}

