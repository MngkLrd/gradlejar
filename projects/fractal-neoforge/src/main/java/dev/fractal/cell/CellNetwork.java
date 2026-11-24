package dev.fractal.cell;
import dev.fractal.Fractal;
import dev.fractal.block.*;
import net.minecraft.commands.*;
import net.minecraft.core.*;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.*;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
public final class CellNetwork {
 public static void tick(ServerTickEvent.Post e){
  MinecraftServer s=e.getServer();if(s.getLevel(CellSpace.KEY)==null||s.overworld().getGameTime()%5!=0)return;
  CellData d=CellData.get(s);
  for(CellData.Cell c:d.cells.values())if(d.live(c))sync(s,d,c);
  for(ServerPlayer p:s.getPlayerList().getPlayers())if(p.serverLevel().dimension().equals(CellSpace.KEY)){
   CellData.Cell c=d.at(p.blockPosition());
   if(!d.live(c)||!CellSpace.inside(c,p.blockPosition())){p.sendSystemMessage(Component.translatable("fractal.collapsed"));CellSpace.ascend(p,d,c);}
  }
 }
 public static void sync(MinecraftServer s,CellData d,CellData.Cell c){
  if(!d.live(c))return;
  ServerLevel outer=s.getLevel(c.address.dimension()),inner=s.getLevel(CellSpace.KEY);
  if(outer==null||inner==null)return;
  BlockPos host=c.address.position();boolean loaded=outer.hasChunkAt(host);
  BlockState ext=loaded?outer.getBlockState(host):null;
  if(loaded&&!ext.is(c.material.host())){d.destroy(c);return;}
  int r=c.reinforcement,k=c.conductivity,l=c.light,i=c.input,o=c.output;
  boolean ready=inner.hasChunkAt(CellSpace.input(c.id))&&inner.hasChunkAt(CellSpace.output(c.id));
  for(int n=0;n<6&&ready;n++)ready=inner.hasChunkAt(CellSpace.socket(c.id,n));
  if(ready){
   int[] types=new int[6];
   for(int n=0;n<6;n++){BlockState b=inner.getBlockState(CellSpace.socket(c.id,n));types[n]=b.is(Fractal.SOCKET.get())?b.getValue(SocketBlock.TYPE):0;}
   var props=LatticeMath.evaluate(types);c.reinforcement=props.reinforcement();c.conductivity=props.conductivity();c.light=props.light();
   c.output=LatticeMath.signal(inner.getBestNeighborSignal(CellSpace.output(c.id)),c.conductivity);
  }
  if(loaded&&outer.hasChunkAt(host.west()))c.input=LatticeMath.signal(outer.getSignal(host.west(),Direction.WEST),c.conductivity);
  else c.input=LatticeMath.signal(c.input,c.conductivity);
  c.output=LatticeMath.signal(c.output,c.conductivity);
  if(ready){BlockPos p=CellSpace.input(c.id);BlockState b=inner.getBlockState(p);if(b.is(Fractal.INPUT.get())&&b.getValue(InputBlock.POWER)!=c.input)inner.setBlock(p,b.setValue(InputBlock.POWER,c.input),3);}
  if(loaded){BlockState next=ext.setValue(HostBlock.REINFORCEMENT,c.reinforcement).setValue(HostBlock.LIGHT,c.light).setValue(HostBlock.POWER,c.output);if(next!=ext)outer.setBlock(host,next,3);}
  if(r!=c.reinforcement||k!=c.conductivity||l!=c.light||i!=c.input||o!=c.output)d.setDirty();
 }
 public static void inspect(ServerPlayer p,CellData.Cell c){p.displayClientMessage(Component.translatable("fractal.status",c.id,c.depth,c.reinforcement,c.conductivity,c.light*5,c.input,c.output),false);}
 public static void onBreak(BlockEvent.BreakEvent e){
  var s=e.getState();
  if(s.is(Fractal.STONE_MATRIX.get())||s.is(Fractal.COPPER_MATRIX.get())||s.is(Fractal.WOOD_MATRIX.get())||s.is(Fractal.CRYSTAL.get())||s.is(Fractal.SOCKET.get())||s.is(Fractal.INPUT.get())||s.is(Fractal.OUTPUT.get())||s.is(Fractal.EXIT.get()))e.setCanceled(true);
 }
 public static void commands(RegisterCommandsEvent e){
  e.getDispatcher().register(Commands.literal("fractal")
   .then(Commands.literal("ascend").executes(ctx->{ServerPlayer p=ctx.getSource().getPlayerOrException();if(!p.serverLevel().dimension().equals(CellSpace.KEY))return 0;CellData d=CellData.get(p.getServer());CellData.Cell c=d.at(p.blockPosition());if(d.live(c))sync(p.getServer(),d,c);CellSpace.ascend(p,d,c);return 1;}))
   .then(Commands.literal("status").executes(ctx->{ServerPlayer p=ctx.getSource().getPlayerOrException();CellData d=CellData.get(p.getServer());CellData.Cell c=p.serverLevel().dimension().equals(CellSpace.KEY)?d.at(p.blockPosition()):null;if(c!=null)inspect(p,c);else p.sendSystemMessage(Component.translatable("fractal.registry",d.cells.values().stream().filter(d::live).count(),CellData.LIMIT));return 1;})));
 }
}
