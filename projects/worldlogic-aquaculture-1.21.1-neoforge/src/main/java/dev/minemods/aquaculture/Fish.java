package dev.minemods.aquaculture;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Cod;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

public final class Fish extends Cod {
 public Fish(EntityType<? extends Cod> type,Level level){super(type,level);}
 @Override protected InteractionResult mobInteract(Player player,InteractionHand hand){
  // Transport is not implemented; prevent vanilla cod bucket identity conversion.
  if(player.getItemInHand(hand).is(Items.WATER_BUCKET))return InteractionResult.FAIL;
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

