package dev.worldfold.item;
import com.mojang.logging.LogUtils;
import dev.worldfold.fold.*;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import java.util.*;
public final class FoldCompass extends Item {
 public FoldCompass(Properties p){super(p);}
 private static CompoundTag read(ItemStack s){return s.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag();}
 private static void write(ItemStack s,CompoundTag t){s.set(DataComponents.CUSTOM_DATA,CustomData.of(t));}
 private static void msg(Player p,String k,Object...a){p.displayClientMessage(Component.translatable("worldfold."+k,a),true);}
 private static double scale(CompoundTag t){return switch(t.getInt("mode")){case 1->2;case 2->.5;default->1;};}
 private static boolean owner(CompoundTag t,Player p){return !t.hasUUID("owner")||t.getUUID("owner").equals(p.getUUID());}
 @Override public InteractionResult useOn(UseOnContext c){
  if(c.getClickedFace()!=Direction.UP)return InteractionResult.PASS;
  if(c.getLevel().isClientSide)return InteractionResult.SUCCESS;
  if(!(c.getPlayer() instanceof ServerPlayer p))return InteractionResult.PASS;
  ItemStack s=c.getItemInHand();CompoundTag t=read(s);
  if(!owner(t,p)){msg(p,"owner");return InteractionResult.CONSUME;}
  if(t.hasUUID("link")){msg(p,"active");return InteractionResult.CONSUME;}
  try{
   ServerLevel l=p.serverLevel();boolean second=t.contains("first");Anchor b=new Anchor(l.dimension(),c.getClickedPos(),p.getDirection().getOpposite(),second?scale(t):1);
   if(!b.clear(l)){msg(p,"blocked");return InteractionResult.CONSUME;}
   if(!second){t.put("first",b.save());t.putUUID("owner",p.getUUID());write(s,t);msg(p,"first");return InteractionResult.CONSUME;}
   Anchor a=Anchor.load(t.getCompound("first"));ServerLevel al=a.level(p.getServer());
   if(al==null){msg(p,"failure");return InteractionResult.CONSUME;}
   al.getChunkAt(a.floor());if(!a.clear(al)){msg(p,"blocked");return InteractionResult.CONSUME;}
   if(a.dimension().equals(b.dimension())&&a.clearance().inflate(.25).intersects(b.clearance())){msg(p,"overlap");return InteractionResult.CONSUME;}
   UUID id=UUID.randomUUID();PortalBridge.connect(p.getServer(),a,b,id);t.put("second",b.save());t.putUUID("link",id);write(s,t);msg(p,"linked");
  }catch(ReflectiveOperationException|RuntimeException ex){LogUtils.getLogger().error("World Fold connect failed",ex);msg(p,"failure");}
  return InteractionResult.CONSUME;
 }
 @Override public InteractionResultHolder<ItemStack> use(Level l,Player p,InteractionHand h){
  ItemStack s=p.getItemInHand(h);if(l.isClientSide)return InteractionResultHolder.success(s);
  if(!(p instanceof ServerPlayer sp))return InteractionResultHolder.pass(s);
  CompoundTag t=read(s);if(!owner(t,p)){msg(p,"owner");return InteractionResultHolder.consume(s);}
  try{if(p.isShiftKeyDown()){if(t.hasUUID("link"))PortalBridge.remove(sp.getServer(),Anchor.load(t.getCompound("first")),Anchor.load(t.getCompound("second")),t.getUUID("link"));t.remove("first");t.remove("second");t.remove("link");t.remove("owner");write(s,t);msg(p,"clear");}
  else if(t.hasUUID("link"))msg(p,"active");else{t.putInt("mode",(t.getInt("mode")+1)%3);write(s,t);msg(p,"mode",scale(t)+"x");}}
  catch(RuntimeException ex){LogUtils.getLogger().error("World Fold reset failed",ex);msg(p,"failure");}
  return InteractionResultHolder.consume(s);
 }
 @Override public void appendHoverText(ItemStack s,TooltipContext c,List<Component> lines,TooltipFlag f){CompoundTag t=read(s);lines.add(Component.translatable("worldfold.tooltip"));lines.add(Component.translatable("worldfold.mode",scale(t)+"x"));if(t.hasUUID("link"))lines.add(Component.translatable("worldfold.connected"));else if(t.contains("first"))lines.add(Component.translatable("worldfold.pending"));}
}