package dev.minemods.worldlogic;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.common.util.FakePlayer;
public final class GuideItem extends Item{
 private static final String GIVEN="worldlogic_guide_received";
 public GuideItem(Properties p){super(p);}
 @Override public InteractionResultHolder<ItemStack> use(Level level,Player player,InteractionHand hand){
  return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand),level.isClientSide());
 }
 public static void login(PlayerEvent.PlayerLoggedInEvent event){
  if(event.getEntity() instanceof ServerPlayer p&&!(p instanceof FakePlayer))grant(p);
 }
 static boolean grant(ServerPlayer p){
  var data=p.getPersistentData();
  CompoundTag persisted=data.getCompound(Player.PERSISTED_NBT_TAG);
  if(persisted.getBoolean(GIVEN))return false;
  boolean already=p.getInventory().contains(new ItemStack(Content.GUIDE.get()));
  if(!already){
   ItemStack book=new ItemStack(Content.GUIDE.get());
   if(!p.getInventory().add(book)&&!book.isEmpty()&&p.drop(book,false)==null)return false;
  }
  persisted.putBoolean(GIVEN,true);data.put(Player.PERSISTED_NBT_TAG,persisted);return true;
 }
 static void verify(net.minecraft.server.level.ServerLevel level){
  var player=net.neoforged.neoforge.common.util.FakePlayerFactory.get(level,new com.mojang.authlib.GameProfile(java.util.UUID.fromString("56e61e52-e22f-4efb-81bf-ae3b48fbff19"),"WorldLogicGuideQA"));
  var data=player.getPersistentData();data.remove(Player.PERSISTED_NBT_TAG);player.getInventory().clearContent();
  if(!grant(player)||!player.getInventory().contains(new ItemStack(Content.GUIDE.get()))||grant(player))throw new IllegalStateException("guide first join/duplicate");
  player.getInventory().clearContent();
  if(grant(player)||player.getInventory().contains(new ItemStack(Content.GUIDE.get())))throw new IllegalStateException("guide lost-book duplicate");
  data.remove(Player.PERSISTED_NBT_TAG);player.getInventory().add(new ItemStack(Content.GUIDE.get()));
  if(!grant(player)||grant(player))throw new IllegalStateException("existing guide recognition");
  int count=0;for(int i=0;i<player.getInventory().getContainerSize();i++)if(player.getInventory().getItem(i).is(Content.GUIDE.get()))count+=player.getInventory().getItem(i).getCount();
  if(count!=1)throw new IllegalStateException("existing guide duplicate "+count);
  player.getInventory().clearContent();data.remove(Player.PERSISTED_NBT_TAG);
 }
}
