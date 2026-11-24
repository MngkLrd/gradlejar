package dev.fractal.item;
import dev.fractal.block.HostBlock;
import dev.fractal.cell.*;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
public final class LatticeLens extends Item {
 public LatticeLens(Properties p){super(p);}
 @Override public InteractionResultHolder<ItemStack> use(Level l,Player p,InteractionHand hand){if(p instanceof ServerPlayer sp)sp.displayClientMessage(Component.translatable("fractal.lens_help"),true);return InteractionResultHolder.sidedSuccess(p.getItemInHand(hand),l.isClientSide);}
 @Override public InteractionResult useOn(UseOnContext c){
  var l=c.getLevel();var pos=c.getClickedPos();var state=l.getBlockState(pos);
  if(!(state.getBlock() instanceof HostBlock)){if(c.getPlayer() instanceof ServerPlayer p)p.displayClientMessage(Component.translatable("fractal.lens_help"),true);return InteractionResult.sidedSuccess(l.isClientSide);}
  if(c.getPlayer() instanceof ServerPlayer p){
   CellData d=CellData.get(p.getServer());CellData.Cell cell=d.host(l.dimension(),pos);
   if(!d.live(cell))return InteractionResult.FAIL;
   boolean open=!state.getValue(HostBlock.CUTAWAY);
   l.setBlock(pos,state.setValue(HostBlock.CUTAWAY,open),3);
   if(open)CellNetwork.preview(p.getServer(),cell);
   p.displayClientMessage(Component.translatable(open?"fractal.cutaway_open":"fractal.cutaway_closed"),true);
  }
  return InteractionResult.sidedSuccess(l.isClientSide);
 }
}
