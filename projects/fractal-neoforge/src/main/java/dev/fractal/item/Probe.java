package dev.fractal.item;
import dev.fractal.Fractal;
import dev.fractal.block.*;
import dev.fractal.cell.*;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
public final class Probe extends Item {
 public Probe(Properties p){super(p);}
 private static void msg(ServerPlayer p,String key,Object... args){p.displayClientMessage(Component.translatable(key,args),true);}
 @Override public InteractionResult useOn(UseOnContext ctx){
  Level l=ctx.getLevel();if(ctx.getPlayer()==null)return InteractionResult.PASS;
  BlockPos pos=ctx.getClickedPos();BlockState state=l.getBlockState(pos);boolean host=state.getBlock() instanceof HostBlock;Material m=Material.from(state);
  if(!host&&m==null&&!state.is(Fractal.SOCKET.get())&&!state.is(Fractal.EXIT.get())&&!state.is(Fractal.INPUT.get())&&!state.is(Fractal.OUTPUT.get()))return InteractionResult.PASS;
  if(l.isClientSide)return InteractionResult.SUCCESS;
  if(!(ctx.getPlayer() instanceof ServerPlayer p)||!(l instanceof ServerLevel level))return InteractionResult.PASS;
  CellData d=CellData.get(p.getServer());CellData.Cell parent=level.dimension().equals(CellSpace.KEY)?d.at(pos):null;
  if(level.dimension().equals(CellSpace.KEY)&&(!d.live(parent)||!CellSpace.inside(parent,pos))){msg(p,"fractal.invalid");return InteractionResult.FAIL;}
  if(state.is(Fractal.EXIT.get())){if(parent!=null&&CellSpace.exit(parent.id).equals(pos)){CellNetwork.syncLineage(p.getServer(),d,parent);CellSpace.ascend(p,d,parent);}return InteractionResult.CONSUME;}
  if(state.is(Fractal.INPUT.get())||state.is(Fractal.OUTPUT.get())){if(parent!=null){CellNetwork.syncLineage(p.getServer(),d,parent);msg(p,state.is(Fractal.INPUT.get())?"fractal.input":"fractal.output",state.is(Fractal.INPUT.get())?parent.input:parent.output);}return InteractionResult.CONSUME;}
  if(state.is(Fractal.SOCKET.get())){
   if(parent==null||CellSpace.socketIndex(parent,pos)<0)return InteractionResult.FAIL;
   ItemStack ingredient=ctx.getHand()==InteractionHand.MAIN_HAND?p.getOffhandItem():p.getMainHandItem();
   int type=ingredient.is(Items.IRON_INGOT)?1:ingredient.is(Items.REDSTONE)?2:ingredient.is(Items.GLOWSTONE_DUST)?3:0;
   if(type==0&&!(p.isShiftKeyDown()&&ingredient.isEmpty())){msg(p,"fractal.socket_help");return InteractionResult.CONSUME;}
   if(type==state.getValue(SocketBlock.TYPE)){msg(p,"fractal.unchanged");return InteractionResult.CONSUME;}
   if(level.setBlock(pos,state.setValue(SocketBlock.TYPE,type),3)){if(type!=0&&!p.getAbilities().instabuild)ingredient.shrink(1);CellNetwork.syncLineage(p.getServer(),d,parent);CellNetwork.inspect(p,parent);}
   return InteractionResult.CONSUME;
  }
  if(host){CellData.Cell c=d.host(level.dimension(),pos);if(!d.live(c)){msg(p,"fractal.invalid");return InteractionResult.FAIL;}CellNetwork.syncLineage(p.getServer(),d,c);if(!d.live(c))return InteractionResult.FAIL;if(p.isShiftKeyDown())CellNetwork.inspect(p,c);else CellSpace.enter(p,c);return InteractionResult.CONSUME;}
  if(p.isShiftKeyDown()){msg(p,"fractal.material",m.name());return InteractionResult.CONSUME;}
  if(!d.available()||(parent!=null&&parent.depth>=CellData.MAX_DEPTH)){msg(p,"fractal.limit");return InteractionResult.FAIL;}
  CellData.Cell c=d.allocate(level.dimension(),pos,parent,m);
  if(!CellSpace.build(p.getServer(),c)){d.destroy(c);msg(p,"fractal.invalid");return InteractionResult.FAIL;}
  BlockState b=m.host().defaultBlockState().setValue(HostBlock.REINFORCEMENT,c.reinforcement).setValue(HostBlock.LIGHT,c.light);
  if(!level.setBlock(pos,b,3)){d.destroy(c);msg(p,"fractal.invalid");return InteractionResult.FAIL;}
  CellNetwork.syncLineage(p.getServer(),d,c);CellSpace.enter(p,c);return InteractionResult.CONSUME;
 }
 @Override public InteractionResultHolder<ItemStack> use(Level l,Player p,InteractionHand hand){
  ItemStack stack=p.getItemInHand(hand);if(!l.dimension().equals(CellSpace.KEY)||!p.isShiftKeyDown())return InteractionResultHolder.pass(stack);
  if(p instanceof ServerPlayer sp){CellData d=CellData.get(sp.getServer());CellData.Cell c=d.at(sp.blockPosition());if(d.live(c))CellNetwork.syncLineage(sp.getServer(),d,c);CellSpace.ascend(sp,d,c);}return InteractionResultHolder.sidedSuccess(stack,l.isClientSide);
 }
}
