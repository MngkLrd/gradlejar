package dev.minemods.phantomlure;
import dev.minemods.phantomlure.registry.ModBlocks;
import dev.minemods.phantomlure.registry.ModBlockEntities;
import dev.minemods.phantomlure.gameplay.PhantomCleanup;
import dev.minemods.phantomlure.block.PhantomLureBlock;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
public final class PhantomLure implements ModInitializer {
 public static final String MOD_ID="phantomlure";
 @Override public void onInitialize() {
   ModBlocks.initialize();
   ModBlockEntities.initialize();
   CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.FUNCTIONAL_BLOCKS)
     .register(entries->entries.accept(ModBlocks.PHANTOM_LURE));
   ServerTickEvents.END_LEVEL_TICK.register(PhantomCleanup::onWorldTick);
   PlayerBlockBreakEvents.AFTER.register((level,player,pos,state,blockEntity)->{
     if(!level.isClientSide() && state.is(ModBlocks.PHANTOM_LURE)
       && state.getValue(PhantomLureBlock.CHARGED) && !player.getAbilities().instabuild) {
       Block.popResource(level,pos,new ItemStack(Items.DIAMOND));
     }
   });
 }
}
