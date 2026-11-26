package dev.minemods.sleepdebt.block;
import com.mojang.serialization.MapCodec;
import dev.minemods.sleepdebt.gameplay.CampRestSessions;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;

public final class CampBedrollBlock extends CampStructureBlock {
 public static final MapCodec<CampBedrollBlock> CODEC=simpleCodec(CampBedrollBlock::new);
 public static final IntegerProperty PART=IntegerProperty.create("part",0,1);
 private static final int[][] OFFSETS={{0,0,0},{0,0,1}};
 private static final int[][] FOOT={{1,0,0,15,2,15},{2,2,0,14,3,14},{2,3,10,14,4,11}};
 private static final int[][] HEAD={{1,0,1,15,2,16},{2,2,6,14,3,16},{2,2,2,14,4,6}};
 public CampBedrollBlock(Properties p){super(p);}
 @Override protected MapCodec<? extends CampBedrollBlock> codec(){return CODEC;}
 @Override protected IntegerProperty partProperty(){return PART;}
 @Override protected int[][] offsets(){return OFFSETS;}
 @Override protected int[][] boxes(int part){return part==0?FOOT:HEAD;}
 @Override protected InteractionResult useWithoutItem(BlockState state,Level level,BlockPos pos,Player player,BlockHitResult hit){
  if(player instanceof ServerPlayer sp){
   if(!complete(level,pos,state)){
    sp.displayClientMessage(Component.translatable("message.sleepdebt.camp_incomplete"),true);
   }else CampRestSessions.begin(sp,anchor(pos,state));
  }
  return InteractionResult.sidedSuccess(level.isClientSide);
 }
}
